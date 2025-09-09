package com.astrivix.qr114.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar; // Import ProgressBar
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.astrivix.qr114.BuildConfig;
import com.astrivix.qr114.MainApplication;
import com.astrivix.qr114.R;
import com.astrivix.qr114.models.ApiResponse; // Import new model
import com.astrivix.qr114.models.PasswordChangeRequest; // Import new model
import com.astrivix.qr114.ui.CommonActivity;
import com.astrivix.qr114.ui.settings.app.AppSettingsFragment;
import com.astrivix.qr114.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    // NOTE: This endpoint should be for CHANGING a password while logged in, not resetting.
    private static final String API_URL = BuildConfig.BASE_URL + "change-password";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private SessionManager sessionManager;
    private View contextView;

    // --- NEW: Views for collapsible section ---
    private TextView changePasswordHeader;
    private LinearLayout collapsiblePasswordSection;

    // UI Components
    private TextView emailTextView, serialNumberTextView;
    private ImageButton settingsButton;
    private Button updatePasswordButton, logoutButton;
    private EditText currentPasswordEditText, newPasswordEditText, confirmPasswordEditText;
    private ImageView currentPasswordToggle, newPasswordToggle, confirmPasswordToggle;
    private ProgressBar loadingProgressBar;

    public ProfileFragment() { /* Required empty public constructor */ }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        contextView = view;
        initializeViews(view);
        loadUserData();
        setupClickListeners();
    }

    private void initializeViews(View view) {
        emailTextView = view.findViewById(R.id.emailTextView);
        serialNumberTextView = view.findViewById(R.id.serialNumberTextView);
        settingsButton = view.findViewById(R.id.settingsButton);
        updatePasswordButton = view.findViewById(R.id.updatePasswordButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar); // Initialize ProgressBar

        currentPasswordEditText = view.findViewById(R.id.currentPasswordEditText);
        newPasswordEditText = view.findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText);

        currentPasswordToggle = view.findViewById(R.id.currentPasswordToggle);
        newPasswordToggle = view.findViewById(R.id.newPasswordToggle);
        confirmPasswordToggle = view.findViewById(R.id.confirmPasswordToggle);

        // --- NEW: Initialize collapsible section views ---
        changePasswordHeader = view.findViewById(R.id.changePasswordHeader);
        collapsiblePasswordSection = view.findViewById(R.id.collapsiblePasswordSection);
    }

    private void loadUserData() {
        emailTextView.setText(sessionManager.getUserEmail());

        // Remove colons from serial number
        String rawSerial = sessionManager.getKeySerialNumber();
        String formattedSerial = "SN: " + rawSerial.replace(":", "");

        serialNumberTextView.setText(formattedSerial);
    }


    private void setupClickListeners() {
        settingsButton.setOnClickListener(v -> navigateToSettings());
        updatePasswordButton.setOnClickListener(v -> attemptPasswordChange());
        logoutButton.setOnClickListener(v -> performLogout());

        setupPasswordToggle(currentPasswordEditText, currentPasswordToggle);
        setupPasswordToggle(newPasswordEditText, newPasswordToggle);
        setupPasswordToggle(confirmPasswordEditText, confirmPasswordToggle);

        // --- NEW: Click listener for the collapsible header ---
        changePasswordHeader.setOnClickListener(v -> togglePasswordSection());
    }

    // --- NEW: Method to handle toggling the password section ---
    private void togglePasswordSection() {
        boolean isVisible = collapsiblePasswordSection.getVisibility() == View.VISIBLE;
        collapsiblePasswordSection.setVisibility(isVisible ? View.GONE : View.VISIBLE);

        // Change the arrow icon to reflect the state (up or down)
        int arrowResId = isVisible ? android.R.drawable.arrow_down_float : android.R.drawable.arrow_up_float;
        changePasswordHeader.setCompoundDrawablesWithIntrinsicBounds(0, 0, arrowResId, 0);
    }

    private void attemptPasswordChange() {
        currentPasswordEditText.setError(null);
        newPasswordEditText.setError(null);
        confirmPasswordEditText.setError(null);

        String currentPassword = currentPasswordEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        boolean isValid = true;
        if (currentPassword.isEmpty()) {
            currentPasswordEditText.setError("Current password is required.");
            isValid = false;
        }
        if (newPassword.length() < 8) {
            newPasswordEditText.setError("Password must be at least 8 characters.");
            isValid = false;
        }
        if (!newPassword.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match.");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // --- Start API Call ---
        showLoading(true);

        // Create the request body object
        PasswordChangeRequest requestData = new PasswordChangeRequest(currentPassword, newPassword, newPassword);
        String jsonBody = gson.toJson(requestData);

        RequestBody body = RequestBody.create(jsonBody, JSON);

        // Build the request with the Authorization header
        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + sessionManager.getKeyAuthToken())
                .post(body)
                .build();

        // Enqueue the asynchronous call
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Network error or server is down
                Log.e(TAG, "Password change failed", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), "Password change failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }

            // In ProfileFragment.java

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (getActivity() == null) return;

                final String responseBody = response.body().string();
                final int responseCode = response.code();

                // This is the important part: Log the raw response so you can see what the server is sending!
                Log.d(TAG, "Server Response (Code: " + responseCode + "): " + responseBody);

                getActivity().runOnUiThread(() -> {
                    showLoading(false);

                    // Check if the HTTP request itself was successful (2xx range)
                    if (response.isSuccessful()) {
                        try {
                            ApiResponse apiResponse = gson.fromJson(responseBody, ApiResponse.class);
                            if (apiResponse != null && apiResponse.isSuccess()) {
                                // SUCCESS
                                Toast.makeText(getContext(), apiResponse.getMessage(), Toast.LENGTH_LONG).show();
                                // Clear fields on success
                                currentPasswordEditText.setText("");
                                newPasswordEditText.setText("");
                                confirmPasswordEditText.setText("");
                            } else {
                                // This case should not happen with a successful response, but it's safe to have.
                                Toast.makeText(getContext(), "An unexpected success response was received.", Toast.LENGTH_LONG).show();
                            }
                        } catch (JsonSyntaxException e) {
                            Log.e(TAG, "Error parsing successful response. Body was not valid JSON.", e);
                            Toast.makeText(getContext(), "Received an invalid response from the server.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // The request was NOT successful (4xx or 5xx error)
                        // The Laravel fix ensures this is always JSON, so we can parse it.
                        try {
                            ApiResponse errorResponse = gson.fromJson(responseBody, ApiResponse.class);
                            String errorMessage = "An error occurred.";
                            if (errorResponse != null && errorResponse.getMessage() != null) {
                                errorMessage = errorResponse.getMessage();
                            } else if (!responseBody.isEmpty()) {
                                // Fallback if the JSON parsing fails but we have a response body
                                errorMessage = responseBody;
                            }
                            // Specifically handle "wrong password" error by highlighting the field
                            if (errorMessage.toLowerCase().contains("current password does not match")) {
                                currentPasswordEditText.setError(errorMessage);
                                currentPasswordEditText.requestFocus();
                            }

                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        } catch (JsonSyntaxException e) {
                            // This catch block handles the original crash reason.
                            Log.e(TAG, "Failed to parse error response. It was not JSON. Code: " + responseCode, e);
                            Toast.makeText(getContext(), "An unreadable error response was received from the server.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            loadingProgressBar.setVisibility(View.VISIBLE);
            updatePasswordButton.setEnabled(false);
            currentPasswordEditText.setEnabled(false);
            newPasswordEditText.setEnabled(false);
            confirmPasswordEditText.setEnabled(false);
        } else {
            loadingProgressBar.setVisibility(View.GONE);
            updatePasswordButton.setEnabled(true);
            currentPasswordEditText.setEnabled(true);
            newPasswordEditText.setEnabled(true);
            confirmPasswordEditText.setEnabled(true);
        }
    }

    private void setupPasswordToggle(final EditText editText, ImageView toggleView) {
        toggleView.setOnClickListener(v -> {
            if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            editText.setSelection(editText.length());
        });
    }

    private void navigateToSettings() {
        CommonActivity.startCommonActivity(this, AppSettingsFragment.class.getCanonicalName());
    }

    private void performLogout() {
        sessionManager.logoutUser();

        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        requireActivity().finish(); // ✅ Make sure current activity ends
    }


}