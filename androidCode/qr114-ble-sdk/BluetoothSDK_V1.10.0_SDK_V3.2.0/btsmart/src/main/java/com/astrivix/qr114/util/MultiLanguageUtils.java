// File: com/astrivix/qr114/util/MultiLanguageUtils.java
package com.astrivix.qr114.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;
import com.astrivix.qr114.MainApplication;
import com.jieli.component.utils.PreferencesHelper;
import java.util.Locale;

public class MultiLanguageUtils {
    // Language Codes
    public final static String LANGUAGE_AUTO = "auto";
    public final static String LANGUAGE_EN = "en";
    public final static String LANGUAGE_ZH = "zh";
    public final static String LANGUAGE_JA = "ja";
    public final static String LANGUAGE_IN = "in";
    public final static String LANGUAGE_AR = "ar";
    public final static String LANGUAGE_MS = "ms";
    public final static String LANGUAGE_TR = "tr";

    // Area (Country) Codes
    public final static String AREA_AUTO = "AUTO";
    public final static String AREA_EN = "US";
    public final static String AREA_ZH = "CN";
    public final static String AREA_JA = "JP";
    public final static String AREA_IN = "ID"; // Correct code for Indonesian is ID, not IN
    // *** FIX: ADDED THE MISSING AREA CODES ***
    public final static String AREA_TR = "TR";
    public final static String AREA_MS = "MY";
    public final static String AREA_AR = "SA";

    // SharedPreferences Keys
    public final static String SP_LANGUAGE = "SP_LANGUAGE";
    public final static String SP_COUNTRY = "SP_COUNTRY";


    public static void changeLanguage(Context context, String language, String area) {
        if (TextUtils.equals(LANGUAGE_AUTO, language) && TextUtils.equals(AREA_AUTO, area)) {
            PreferencesHelper.putStringValue(MainApplication.getApplication(), SP_LANGUAGE, LANGUAGE_AUTO);
            PreferencesHelper.putStringValue(MainApplication.getApplication(), SP_COUNTRY, AREA_AUTO);
        } else {
            Locale newLocale = new Locale(language, area);
            changeAppLanguage(context, newLocale, true);
        }
    }

    public static void changeAppLanguage(Context context, Locale locale, boolean persistence) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        Configuration configuration = resources.getConfiguration();
        setLanguage(context, locale, configuration);
        resources.updateConfiguration(configuration, metrics);
        if (persistence) {
            saveLanguageSetting(context, locale);
        }
    }

    private static void setLanguage(Context context, Locale locale, Configuration configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            configuration.setLocales(new LocaleList(locale));
            context.createConfigurationContext(configuration);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
        } else {
            configuration.locale = locale;
        }
    }

    public static void saveLanguageSetting(Context context, Locale locale) {
        PreferencesHelper.putStringValue(MainApplication.getApplication(), SP_LANGUAGE, locale.getLanguage());
        PreferencesHelper.putStringValue(MainApplication.getApplication(), SP_COUNTRY, locale.getCountry());
    }

    // ... The rest of your original code is correct and remains unchanged ...
    public static boolean isSameWithSetting(Context context) {
        Locale locale = getAppLocale(context);
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String sp_language = PreferencesHelper.getSharedPreferences(MainApplication.getApplication()).getString(SP_LANGUAGE,LANGUAGE_AUTO);
        String sp_country = PreferencesHelper.getSharedPreferences(MainApplication.getApplication()).getString(SP_COUNTRY, AREA_AUTO);
        if (sp_language.equals(LANGUAGE_AUTO) && sp_country.equals(AREA_AUTO)) {
            Locale locale1 = getSystemLanguage().get(0);
            return TextUtils.equals(language, locale1.getLanguage());
        }
        return TextUtils.equals(language, sp_language) && TextUtils.equals(country, sp_country);
    }

    public static Locale getAppLocale(Context context) {
        Locale local;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            local = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            local = context.getResources().getConfiguration().locale;
        }
        return local;
    }

    public static LocaleListCompat getSystemLanguage() {
        Configuration configuration = Resources.getSystem().getConfiguration();
        return ConfigurationCompat.getLocales(configuration);
    }

    public static Application.ActivityLifecycleCallbacks callbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            String language = PreferencesHelper.getSharedPreferences(MainApplication.getApplication()).getString(SP_LANGUAGE,LANGUAGE_AUTO);
            String country = PreferencesHelper.getSharedPreferences(MainApplication.getApplication()).getString(SP_COUNTRY, AREA_AUTO);
            if (!isSameWithSetting(activity)) {
                if (TextUtils.equals(LANGUAGE_AUTO, language) && TextUtils.equals(AREA_AUTO, country)) {
                    Locale locale = getSystemLanguage().get(0);
                    language = locale.getLanguage();
                    country = locale.getCountry();
                }
                Locale locale = new Locale(language, country);
                changeAppLanguage(activity, locale, false);
            }
        }
        @Override public void onActivityStarted(Activity activity) { }
        @Override public void onActivityResumed(Activity activity) { }
        @Override public void onActivityPaused(Activity activity) { }
        @Override public void onActivityStopped(Activity activity) { }
        @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
        @Override public void onActivityDestroyed(Activity activity) { }
    };
}