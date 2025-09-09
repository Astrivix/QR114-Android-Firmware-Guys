package com.astrivix.qr114.ui.launcher;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.astrivix.qr114.models.MobileModel;
import com.astrivix.qr114.models.UserCountry;
import com.google.gson.Gson; // <-- Import GSON
import com.astrivix.qr114.BuildConfig;
import com.astrivix.qr114.R;
import com.astrivix.qr114.models.ErrorResponse; // <-- Import your new models
import com.astrivix.qr114.models.LoginResponse;
import com.astrivix.qr114.models.RegisterRequest;
import com.astrivix.qr114.ui.home.HomeActivity;
import com.astrivix.qr114.util.SessionManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 *
 * @author sasanda saumya
 * @since 2025/7/16
 */
public class SignUpFragment extends Fragment {

    // --- API and UI Components ---
    private static final String API_URL = BuildConfig.BASE_URL +"register";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson(); // <-- Instantiate GSON

    private EditText emailEditText;
    private EditText serialEditText;
    private EditText passwordEditText;
    private EditText passwordEditText2;
    private Button registerButton;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private String userCountry;
    private String mobileModel;
    String serial = null;

    private OnNavigateToLoginListener mListener;

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
        return inflater.inflate(R.layout.fragment_sign_up, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- Find Views ---
        emailEditText = view.findViewById(R.id.emailEditText);
        serialEditText = view.findViewById(R.id.serialEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        passwordEditText2 = view.findViewById(R.id.passwordEditText2);
        registerButton = view.findViewById(R.id.registerButton);
        progressBar = view.findViewById(R.id.progressBar);
        TextView loginLinkText = view.findViewById(R.id.loginLinkText);
        sessionManager = new SessionManager(requireContext());

        //set user country and mobile model
        userCountry = new UserCountry().getUserCountry(requireContext());
        mobileModel = new MobileModel().getMobileModel();

        ImageView passwordToggle = view.findViewById(R.id.passwordToggle);
        ImageView passwordToggle2 = view.findViewById(R.id.passwordToggle2);

        // ADDED: Set up click listeners for the password toggles
        setupPasswordToggle(passwordEditText, passwordToggle);
        setupPasswordToggle(passwordEditText2, passwordToggle2);


        focusChangeDrawables(view);

        // --- Setup Listeners ---
        loginLinkText.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.navigateToLogin();
            }
        });

        registerButton.setOnClickListener(v -> {
            emailEditText.setError(null);
            serialEditText.setError(null);
            passwordEditText.setError(null);
            passwordEditText2.setError(null);
            attemptRegistration();
        });


        addSerialAutoFormatting(serialEditText);
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

    private void attemptRegistration() {
        String email = emailEditText.getText().toString().trim();
//        String serial = serialEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String password2 = passwordEditText2.getText().toString().trim();

        // --- Client-side validation --- (No changes here)
        if (serial.isEmpty()) {
            serialEditText.setError("Serial number is required.");
            serialEditText.requestFocus();
            return;
        }
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
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required.");
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

        setLoadingState(true);

        // --- GSON: Create request object and convert to JSON string ---
        RegisterRequest registerRequest = new RegisterRequest(email, serial, password, userCountry, mobileModel);
        String jsonBody = gson.toJson(registerRequest);

        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Accept", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SignUpFragment", "API call failed", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        setLoadingState(false);
                        Toast.makeText(getContext(), "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                        // HTTP 2xx: Success
                        LoginResponse loginResponse = gson.fromJson(responseBody, LoginResponse.class);
                        String token = loginResponse.getData().getAccessToken();
                        String userEmail = loginResponse.getData().getUser().getEmail();
                        String serialNumber = loginResponse.getData().getUser().getSerialNumber();

                        sessionManager.createLoginSession(token, userEmail, serialNumber);

                        Toast.makeText(getContext(), "Registration Successful!", Toast.LENGTH_LONG).show();

                        // Navigate to the main part of the app
                        Intent intent = new Intent(getActivity(), HomeActivity.class);
                        startActivity(intent);
                        requireActivity().finish(); // Close the launcher flow

                        /**
                         * {
                         *     "success": true,
                         *     "message": "User registered successfully",
                         *     "data": {
                         *         "user": {
                         *             "email": "sasa@example.com",
                         *             "serial_number": "AA:BB:CC:DD:EE:FF",
                         *             "updated_at": "2025-07-16T04:17:44.000000Z",
                         *             "created_at": "2025-07-16T04:17:44.000000Z",
                         *             "id": 6
                         *         },
                         *         "access_token": "4|TsJrRlQvuXJZdSFk58i1N85DAXcov081zaYQB0im5f4190c3",
                         *         "token_type": "Bearer"
                         *     }
                         * }
                         */

                    } else {
                        // HTTP 4xx, 5xx: Error. Parse with GSON.
                        try {
                            ErrorResponse errorResponse = gson.fromJson(responseBody, ErrorResponse.class);
                            if (errorResponse != null && errorResponse.getErrors() != null) {
                                handleValidationErrors(errorResponse.getErrors());
                            } else if (errorResponse != null) {
                                Toast.makeText(getContext(), errorResponse.getMessage(), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getContext(), "An unknown error occurred.", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e("SignUpFragment", "JSON parsing error", e);
                            Toast.makeText(getContext(), "An unexpected error occurred parsing the response.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    // --- MODIFIED to use a Map, which is much cleaner and type-safe ---
    private void handleValidationErrors(Map<String, List<String>> errors) {
        // Iterate through each field that has an error
        for (Map.Entry<String, List<String>> entry : errors.entrySet()) {
            String field = entry.getKey();
            List<String> messages = entry.getValue();

            if (messages != null && !messages.isEmpty()) {
                String errorMessage = messages.get(0); // Get the first error message
                switch (field) {
                    case "email":
                        emailEditText.setError(errorMessage);
                        emailEditText.requestFocus();
                        break;
                    case "serial_number":
                        serialEditText.setError(errorMessage);
                        serialEditText.requestFocus();
                        break;
                    case "password":
                        passwordEditText.setError(errorMessage);
                        passwordEditText.requestFocus();
                        break;
                }
            }
        }
    }


    private void setLoadingState(boolean isLoading) {
        // ... (your existing setLoadingState method - no changes needed)
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            registerButton.setEnabled(false);
            emailEditText.setEnabled(false);
            serialEditText.setEnabled(false);
            passwordEditText.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            registerButton.setEnabled(true);
            emailEditText.setEnabled(true);
            serialEditText.setEnabled(true);
            passwordEditText.setEnabled(true);
        }
    }


    private void focusChangeDrawables(View view){
        // ... (your existing focusChangeDrawables method - no changes needed)
        ConstraintLayout emailInputLayout = view.findViewById(R.id.emailInputLayout);
        ConstraintLayout serialInputLayout = view.findViewById(R.id.serialInputLayout);
        ConstraintLayout passwordInputLayout = view.findViewById(R.id.passwordInputLayout);
        EditText emailEditText = view.findViewById(R.id.emailEditText);
        EditText passwordEditText = view.findViewById(R.id.passwordEditText);
        EditText serialEditText = view.findViewById(R.id.serialEditText);

        serialEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus){
                serialInputLayout.setBackgroundResource(R.drawable.shape_edittext_focused);
            }else{
                serialInputLayout.setBackgroundResource(R.drawable.edittext_background_selector);
            }
        });

        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus){
                emailInputLayout.setBackgroundResource(R.drawable.shape_edittext_focused);
            }else{
                emailInputLayout.setBackgroundResource(R.drawable.edittext_background_selector);
            }
        });

        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus){
                passwordInputLayout.setBackgroundResource(R.drawable.shape_edittext_focused);
            }else{
                passwordInputLayout.setBackgroundResource(R.drawable.edittext_background_selector);
            }
        });
    }

    private void addSerialAutoFormatting(final EditText editText) {
        // ... (your existing addSerialAutoFormatting method - no changes needed)
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;
                String cleanString = s.toString().replaceAll("[^a-zA-Z0-9]", "");
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < cleanString.length(); i++) {
                    formatted.append(cleanString.charAt(i));
                    if ((i + 1) % 2 == 0 && (i + 1) != cleanString.length()) {
                        formatted.append(":");
                    }
                }
//                editText.setText(formatted.toString().toUpperCase());
//                editText.setSelection(formatted.length());
                serial = formatted.toString().toUpperCase();
                isFormatting = false;
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}