package com.astrivix.qr114.ui.launcher;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
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

import com.astrivix.qr114.BuildConfig;
import com.google.gson.Gson;
import com.astrivix.qr114.R;
import com.astrivix.qr114.models.ErrorResponse;
import com.astrivix.qr114.models.LoginRequest;
import com.astrivix.qr114.models.LoginResponse;
import com.astrivix.qr114.ui.home.HomeActivity;
import com.astrivix.qr114.util.SessionManager;

import java.io.IOException;

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
public class LoginFragment extends Fragment {

    // --- API and UI Components ---
    private static final String API_URL = BuildConfig.BASE_URL +"login";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private OnNavigateToForgotPasswordListener mForgotListener;
    private OnNavigateToSignUpListener mListener;

    // ADD THIS: The new listener for login success
    private OnLoginSuccessListener mLoginSuccessListener;

    // ADD THIS: The interface definition
    public interface OnLoginSuccessListener {
        void onLoginSuccess();
    }


    // ADDED: Flag to prevent multiple login attempts
    private boolean isLoginInProgress = false;

    public interface OnNavigateToSignUpListener {
        void navigateToSignUp();
    }

    public interface OnNavigateToForgotPasswordListener {
        void navigateToForgotPassword();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnNavigateToSignUpListener) {
            mListener = (OnNavigateToSignUpListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNavigateToSignUpListener");
        }
        if (context instanceof OnNavigateToForgotPasswordListener) {
            mForgotListener = (OnNavigateToForgotPasswordListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNavigateToForgotPasswordListener");
        }
        // ADD THIS: Attach the new login success listener
        if (context instanceof OnLoginSuccessListener) {
            mLoginSuccessListener = (OnLoginSuccessListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnLoginSuccessListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        loginButton = view.findViewById(R.id.loginButton);
        progressBar = view.findViewById(R.id.progressBar);
        TextView signUpLinkText = view.findViewById(R.id.signUpLinkText);
        TextView forgotPasswordTextView = view.findViewById(R.id.forgotPasswordTextView);
        ImageView passwordToggle = view.findViewById(R.id.passwordToggle);

        setupPasswordToggle(passwordEditText, passwordToggle);
        setupSignUpLink(signUpLinkText);
        focusChangeDrawables(view);

        loginButton.setOnClickListener(v -> attemptLogin());

        forgotPasswordTextView.setOnClickListener(v -> {
            if (mForgotListener != null) {
                mForgotListener.navigateToForgotPassword();
            }
        });
    }

    private void attemptLogin() {
        // MODIFIED: Added check to prevent multiple clicks
        if (isLoginInProgress) {
            return; // Don't start a new login if one is already running
        }
        isLoginInProgress = true; // Mark login as started

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty()) {
            emailEditText.setError("Email is required.");
            emailEditText.requestFocus();
            isLoginInProgress = false; // Reset flag on validation fail
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email address.");
            emailEditText.requestFocus();
            isLoginInProgress = false; // Reset flag on validation fail
            return;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required.");
            passwordEditText.requestFocus();
            isLoginInProgress = false; // Reset flag on validation fail
            return;
        }

        setLoadingState(true);

        LoginRequest loginRequest = new LoginRequest(email, password);
        String jsonBody = gson.toJson(loginRequest);
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Accept", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("LoginFragment", "API call failed", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        setLoadingState(false);
                        isLoginInProgress = false; // ADDED: Reset flag on failure
                        Toast.makeText(getContext(), "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body().string();
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    setLoadingState(false);
                    isLoginInProgress = false; // ADDED: Reset flag on response

                    if (response.isSuccessful()) {
                        LoginResponse loginResponse = gson.fromJson(responseBody, LoginResponse.class);
                        String token = loginResponse.getData().getAccessToken();
                        String userEmail = loginResponse.getData().getUser().getEmail();
                        String serialNumber = loginResponse.getData().getUser().getSerialNumber();

                        sessionManager.createLoginSession(token, userEmail, serialNumber);

                        Toast.makeText(getContext(), "Login Successful!", Toast.LENGTH_SHORT).show();

                        // --- THIS IS THE KEY CHANGE ---
                        // REMOVE the old navigation code
                         Intent intent = new Intent(getActivity(), HomeActivity.class);
                         startActivity(intent);
                         requireActivity().finish();

                        // INSTEAD, notify the activity that login was successful
//                        if (mLoginSuccessListener != null) {
//                            mLoginSuccessListener.onLoginSuccess();
//                        }

                        /**
                         * {
                         *     "success": true,
                         *     "message": "Login successful",
                         *     "data": {
                         *         "user": {
                         *             "id": 5,
                         *             "email": "dtstuser@example.com",
                         *             "serial_number": "58:F2:DC:D0:22:A8",
                         *             "email_verified_at": null,
                         *             "created_at": "2025-07-14T01:33:33.000000Z",
                         *             "updated_at": "2025-07-14T01:33:33.000000Z"
                         *         },
                         *         "access_token": "3|CzkjkJ5deUXuqb2UWfkly2H6wE7krC6FacIRAzH953090f02",
                         *         "token_type": "Bearer"
                         *     }
                         * }
                         */

                    } else {
                        try {
                            ErrorResponse errorResponse = gson.fromJson(responseBody, ErrorResponse.class);
                            String errorMessage = errorResponse.getMessage() != null ? errorResponse.getMessage() : "An unknown error occurred.";
                            emailEditText.setError(errorMessage);
                            passwordEditText.setError(errorMessage);
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e("LoginFragment", "Error parsing failed login response", e);
                            Toast.makeText(getContext(), "An unexpected error occurred.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    private void setupPasswordToggle(final EditText passwordField, final ImageView toggleIcon) {
        final Typeface originalTypeface = passwordField.getTypeface();
        toggleIcon.setOnClickListener(v -> {
            if (passwordField.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            passwordField.setTypeface(originalTypeface);
            passwordField.setSelection(passwordField.getText().length());
        });
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);
            emailEditText.setEnabled(false);
            passwordEditText.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
            emailEditText.setEnabled(true);
            passwordEditText.setEnabled(true);
        }
    }

    private void focusChangeDrawables(View view){
        ConstraintLayout emailInputLayout = view.findViewById(R.id.emailInputLayout);
        ConstraintLayout passwordInputLayout = view.findViewById(R.id.passwordInputLayout);
        EditText emailEditText = view.findViewById(R.id.emailEditText);
        EditText passwordEditText = view.findViewById(R.id.passwordEditText);

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

    private void setupSignUpLink(TextView signUpTextView) {
        String fullText = "Don't have an account? Sign Up";
        SpannableString spannableString = new SpannableString(fullText);
        ClickableSpan signUpSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                if (mListener != null) {
                    mListener.navigateToSignUp();
                }
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(getResources().getColor(R.color.primary_green));
                ds.setFakeBoldText(true);
            }
        };

        int startIndex = fullText.indexOf("Sign Up");
        spannableString.setSpan(signUpSpan, startIndex, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        signUpTextView.setText(spannableString);
        signUpTextView.setMovementMethod(LinkMovementMethod.getInstance());
        signUpTextView.setHighlightColor(Color.TRANSPARENT);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mLoginSuccessListener = null;
    }
}