package com.astrivix.qr114.ui.launcher;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.astrivix.qr114.BuildConfig;
import com.astrivix.qr114.R;
import com.astrivix.qr114.models.ErrorResponse;
import com.astrivix.qr114.models.ForgotPasswordRequest;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import android.graphics.Typeface; // <-- Add this import
import android.text.InputType;   // <-- Add this import
import android.widget.ImageView;   // <-- Add this import

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ForgotPasswordFragment extends Fragment {

    // --- API and UI Components ---
    private static final String API_URL = BuildConfig.BASE_URL + "forgot-password-serial";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private EditText emailEditText;
    private EditText serialEditText;
    private EditText passwordEditText;
    private EditText passwordEditText2;
    private TextView contactSupportLink;
    private Button resetPasswordButton;
    private ProgressBar progressBar;

    private OnNavigateToLoginListener mListener;

    // Interface to communicate with the host activity
    public interface OnNavigateToLoginListener {
        void navigateToLogin();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnNavigateToLoginListener) {
            mListener = (OnNavigateToLoginListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNavigateToLoginListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- Find Views ---
        emailEditText = view.findViewById(R.id.emailEditText);
        serialEditText = view.findViewById(R.id.serialEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        passwordEditText2 = view.findViewById(R.id.passwordEditText2);
        resetPasswordButton = view.findViewById(R.id.resetPasswordButton);
        contactSupportLink = view.findViewById(R.id.contactSupportLInk);
        progressBar = view.findViewById(R.id.progressBar);
        TextView loginLinkText = view.findViewById(R.id.loginLinkText);

        ImageView passwordToggle = view.findViewById(R.id.passwordToggle);
        ImageView passwordToggle2 = view.findViewById(R.id.passwordToggle2);

        // --- Setup Listeners ---
        resetPasswordButton.setOnClickListener(v -> attemptPasswordReset());
        loginLinkText.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.navigateToLogin();
            }
        });

        contactSupportLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = "https://qr114.com/forgot-password-support";
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                view.getContext().startActivity(intent);
            }
        });

        // ADDED: Set up click listeners for the password toggles
        setupPasswordToggle(passwordEditText, passwordToggle);
        setupPasswordToggle(passwordEditText2, passwordToggle2);
    }

    // ADD THIS NEW HELPER METHOD TO THE FRAGMENT
    /**
     * Sets up a click listener on a toggle icon to show/hide the password in an EditText.
     * @param passwordField The EditText for the password.
     * @param toggleIcon The ImageView that acts as the toggle button.
     */
    private void setupPasswordToggle(final EditText passwordField, final ImageView toggleIcon) {
        // Store the original typeface to re-apply it after changing input type
        final Typeface originalTypeface = passwordField.getTypeface();

        toggleIcon.setOnClickListener(v -> {
            // Check the current input type to see if the password is visible or hidden
            if (passwordField.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                // If password is HIDDEN, show it
                passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                // If password is VISIBLE, hide it
                passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            // Restore the original typeface to prevent font style changes
            passwordField.setTypeface(originalTypeface);
            // Move the cursor to the end of the text
            passwordField.setSelection(passwordField.getText().length());
        });
    }

    private void attemptPasswordReset() {
        String email = emailEditText.getText().toString().trim();
        String serial = serialEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String password2 = passwordEditText2.getText().toString().trim();

        // --- Client-side validation ---
        if (email.isEmpty()) {
            emailEditText.setError("Email is required.");
            emailEditText.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email address.");
            emailEditText.requestFocus();
            return;
        }
        if (serial.isEmpty()) {
            serialEditText.setError("Serial number is required.");
            serialEditText.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("New password is required.");
            passwordEditText.requestFocus();
            return;
        }
        if (password.length() < 8) {
            passwordEditText.setError("Password must be at least 8 characters long.");
            passwordEditText.requestFocus();
            return;
        }
        if (!password.equals(password2)) {
            passwordEditText2.setError("Passwords do not match.");
            passwordEditText2.requestFocus();
            return;
        }

        // IMPORTANT: Format the serial number to match the database (FF:EE:DD:...)
        String formattedSerial = formatSerialNumberForApi(serial);

        setLoadingState(true);

        // Create the request object
        ForgotPasswordRequest requestPayload = new ForgotPasswordRequest(email, formattedSerial, password, password2);
        String jsonBody = gson.toJson(requestPayload);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Accept", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("ForgotPassword", "API call failed", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        setLoadingState(false);
                        Toast.makeText(getContext(), "Request failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body().string();
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    setLoadingState(false);
                    if (response.isSuccessful()) {
                        // HTTP 200: Success
                        Toast.makeText(getContext(), "Password reset successfully!", Toast.LENGTH_LONG).show();
                        // Navigate back to the login screen
                        if (mListener != null) {
                            mListener.navigateToLogin();
                        }
                    } else {
                        // HTTP 4xx, 5xx: Error
                        try {
                            ErrorResponse errorResponse = gson.fromJson(responseBody, ErrorResponse.class);
                            if (errorResponse != null && errorResponse.getErrors() != null) {
                                handleValidationErrors(errorResponse.getErrors());
                            } else if (errorResponse != null && errorResponse.getMessage() != null) {
                                // Show general error (e.g., "User not found")
                                Toast.makeText(getContext(), errorResponse.getMessage(), Toast.LENGTH_LONG).show();
                                emailEditText.setError(errorResponse.getMessage());
                                serialEditText.setError(errorResponse.getMessage());
                            } else {
                                Toast.makeText(getContext(), "An unknown error occurred.", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e("ForgotPassword", "JSON parsing error", e);
                            Toast.makeText(getContext(), "An error occurred parsing the response.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    /**
     * This helper function takes a clean serial string (e.g., "FFEEDD4C22A8")
     * and formats it into the required API format (e.g., "FF:EE:DD:4C:22:A8").
     */
    private String formatSerialNumberForApi(String rawSerial) {
        String cleanString = rawSerial.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (cleanString.length() != 12) {
            // Return as is if it's not the expected length, validation will catch it.
            return rawSerial;
        }
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < cleanString.length(); i += 2) {
            formatted.append(cleanString.substring(i, i + 2));
            if (i < cleanString.length() - 2) {
                formatted.append(":");
            }
        }
        return formatted.toString();
    }


    private void handleValidationErrors(Map<String, List<String>> errors) {
        for (Map.Entry<String, List<String>> entry : errors.entrySet()) {
            String field = entry.getKey();
            List<String> messages = entry.getValue();
            if (messages != null && !messages.isEmpty()) {
                String errorMessage = messages.get(0);
                switch (field) {
                    case "email":
                        emailEditText.setError(errorMessage);
                        break;
                    case "serial_number":
                        serialEditText.setError(errorMessage);
                        break;
                    case "password":
                        passwordEditText.setError(errorMessage);
                        break;
                }
            }
        }
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            resetPasswordButton.setEnabled(false);
            emailEditText.setEnabled(false);
            serialEditText.setEnabled(false);
            passwordEditText.setEnabled(false);
            passwordEditText2.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            resetPasswordButton.setEnabled(true);
            emailEditText.setEnabled(true);
            serialEditText.setEnabled(true);
            passwordEditText.setEnabled(true);
            passwordEditText2.setEnabled(true);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}