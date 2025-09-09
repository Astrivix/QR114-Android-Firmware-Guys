package com.astrivix.qr114.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class AppPreferences {

    private static final String PREF_NAME = "QR_APP_PREFS";
    private static final String KEY_QR_CODE_VALUE = "qr_code_value";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Checks if a user is considered signed in by seeing if a QR code value exists.
     */
    public static boolean isUserSignedIn(Context context) {
        String qrValue = getPrefs(context).getString(KEY_QR_CODE_VALUE, null);
        // User is signed in if the value is not null and not an empty string
        return !TextUtils.isEmpty(qrValue);
    }

    /**
     * Saves the scanned QR code value.
     */
    public static void saveQRCodeValue(Context context, String value) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(KEY_QR_CODE_VALUE, value);
        editor.apply();
    }

    /**
     * Retrieves the stored QR code value.
     */
    public static String getQRCodeValue(Context context) {
        return getPrefs(context).getString(KEY_QR_CODE_VALUE, null);
    }

    /**
     * Clears the stored value (useful for a logout feature).
     */
    public static void clear(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.clear();
        editor.apply();
    }
}