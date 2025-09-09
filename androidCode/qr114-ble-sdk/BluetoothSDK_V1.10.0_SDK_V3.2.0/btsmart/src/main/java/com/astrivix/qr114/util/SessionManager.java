package com.astrivix.qr114.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.astrivix.qr114.ui.home.HomeActivity;
import com.astrivix.qr114.ui.home.LoginActivity;

/**
 *
 * @author sasanda saumya
 * @since 2025/7/16
 */

public class SessionManager {

    // SharedPreferences file name
    private static final String PREF_NAME = "AppUserSession";

    // All Shared Preferences Keys
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    public static final String KEY_AUTH_TOKEN = "authToken";
    public static final String KEY_USER_EMAIL = "userEmail";
    public static final String KEY_SERIAL_NUMBER = "serialNum";

    // --- ADDED FOR FIRST-RUN LANGUAGE SELECTION ---
    private static final String KEY_IS_FIRST_RUN_COMPLETE = "isFirstRunComplete";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;

    // Constructor
    public SessionManager(Context context) {
        this.context = context.getApplicationContext(); // Use application context to avoid memory leaks
        pref = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * Creates a login session for the user.
     * This should be called from your LoginActivity after a successful API call.
     *
     * @param authToken The authentication token received from the server.
     * @param email     The user's email.
     */
    public void createLoginSession(String authToken, String email, String serialNumber) {
        // Storing login value as TRUE
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        // Storing auth token and email in pref
        editor.putString(KEY_AUTH_TOKEN, authToken);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_SERIAL_NUMBER, serialNumber);
        // commit changes
        editor.apply(); // use apply() as it is asynchronous
    }

    /**
     * Checks the login status of the user.
     * If false it will redirect user to login page
     * Else won't do anything
     *
     * @return true if logged in, false otherwise.
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Retrieves the stored authentication token.
     *
     * @return The auth token, or null if not found.
     */
    public String getAuthToken() {
        return pref.getString(KEY_AUTH_TOKEN, null);
    }

    public String getUserEmail(){
        return pref.getString(KEY_USER_EMAIL, null);
    }

    public String getKeyAuthToken(){
        return pref.getString(KEY_AUTH_TOKEN, null);
    }

    /**
     * Retrieves the stored serial number.
     *
     * @return The user's serial number, or null if not found.
     */
    public String getKeySerialNumber(){
        return pref.getString(KEY_SERIAL_NUMBER, null);
    }

    /**
     * Sets the flag indicating that the initial language selection has been completed.
     * This should be called from LanguageSelectActivity after the user clicks "OK".
     */
    public void setFirstRunComplete(boolean isComplete) {
        editor.putBoolean(KEY_IS_FIRST_RUN_COMPLETE, isComplete);
        editor.apply();
    }

    /**
     * Checks if the first-run setup (language selection) has been completed.
     * This is called from SplashActivity to decide which screen to show.
     *
     * @return true if the language has been set, false otherwise.
     */
    public boolean isFirstRunComplete() {
        return pref.getBoolean(KEY_IS_FIRST_RUN_COMPLETE, false);
    }

    /**
     * Clears session details on logout.
     */
    public void logoutUser() {
        // Clearing all data from SharedPreferences
//        editor.clear();
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_AUTH_TOKEN);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_SERIAL_NUMBER);
        editor.apply();

        // After logout, redirect user to the Login Activity
//        Intent i = new Intent(context, LoginActivity.class);
//        // Add flags to clear the activity stack and start a new task
//        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        context.startActivity(i);
    }
}