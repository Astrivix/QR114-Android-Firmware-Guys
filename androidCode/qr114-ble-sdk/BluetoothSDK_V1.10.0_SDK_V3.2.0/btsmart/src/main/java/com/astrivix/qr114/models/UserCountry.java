package com.astrivix.qr114.models;
import android.content.Context;
import android.telephony.TelephonyManager;
import java.util.Locale;

public class UserCountry {

    public String getUserCountry(Context context) {
        String country = null;

        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            // Get SIM country
            if (tm != null && tm.getSimState() == TelephonyManager.SIM_STATE_READY) {
                country = tm.getSimCountryIso().toUpperCase();
            }

            // If SIM country is not available, use network
            if (country == null || country.length() == 0) {
                country = tm.getNetworkCountryIso().toUpperCase();
            }

            // Fallback to Locale
            if (country == null || country.length() == 0) {
                country = Locale.getDefault().getCountry();
            }
        } catch (Exception e) {
            e.printStackTrace();
            country = Locale.getDefault().getCountry(); // Fallback
        }

        return country;
    }
}
