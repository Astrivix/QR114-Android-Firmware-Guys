package com.astrivix.qr114.ui.home;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.astrivix.qr114.R;
import com.astrivix.qr114.ui.launcher.ForgotPasswordFragment;
import com.astrivix.qr114.ui.launcher.LoginFragment;
import com.astrivix.qr114.ui.launcher.SignUpFragment; // Make sure you have created this fragment

public class LoginActivity extends AppCompatActivity implements
        LoginFragment.OnNavigateToSignUpListener, SignUpFragment.OnNavigateToLoginListener , LoginFragment.OnNavigateToForgotPasswordListener, ForgotPasswordFragment.OnNavigateToLoginListener , LoginFragment.OnLoginSuccessListener{

    // ADD THIS ENTIRE METHOD - This is called by the fragment on success
    @Override
    public void onLoginSuccess() {
        // All navigation logic now lives safely in the Activity.
        Intent intent = new Intent(this, HomeActivity.class);
        // These flags clear the activity stack so the user can't press "back" to get to the login screen.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish the LoginActivity
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // This ensures the fragment is only added once, when the activity is first created.
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.loginActivityFrameLayout, new LoginFragment())
                    .commit();
        }
    }

    // This method is called from LoginFragment when the "Sign Up" link is clicked
    @Override
    public void navigateToSignUp() {
        Fragment signUpFragment = new SignUpFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.loginActivityFrameLayout, signUpFragment)
                // Add this transaction to the back stack. This allows the user
                // to press the back button to return to the LoginFragment.
                .addToBackStack(null)
                .commit();
    }



    // This method is called from SignUpFragment when the "Already have an account?" link is clicked
    @Override
    public void navigateToLogin() {
        // We just pop the back stack to go back to the previous fragment (LoginFragment)
        getSupportFragmentManager().popBackStack();
    }

    @Override
    public void navigateToForgotPassword() {
        Fragment forgotPasswordFragment = new ForgotPasswordFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.loginActivityFrameLayout, forgotPasswordFragment)
                // Add to back stack so the user can press back to return to Login
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            // If navigating within login/signup/forgot fragments, allow back
            super.onBackPressed();
        } else {
            // If no fragments in back stack (i.e. just landed here after logout), block back
            moveTaskToBack(true); // ✅ Optionally: finishAffinity(); to exit app
        }
    }

}