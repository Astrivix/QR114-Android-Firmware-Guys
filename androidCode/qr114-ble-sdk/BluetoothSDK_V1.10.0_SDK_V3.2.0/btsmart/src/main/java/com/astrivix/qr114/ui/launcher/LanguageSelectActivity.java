// File: com/astrivix/qr114/ui/launcher/LanguageSelectActivity.java
package com.astrivix.qr114.ui.launcher;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.astrivix.qr114.R;
import com.astrivix.qr114.ui.home.HomeActivity;
import com.astrivix.qr114.ui.home.LoginActivity;
import com.astrivix.qr114.util.MultiLanguageUtils;
import com.astrivix.qr114.util.SessionManager;
// import com.jieli.bluetooth.utils.JL_Log; // Replaced with standard Log
// import com.jieli.component.network.WifiHelper; // Removed as it's not core to this logic

import java.util.HashMap;
import java.util.Map;

public class LanguageSelectActivity extends AppCompatActivity {

    private Map<String, TextView> languageViewsMap;
    private Button registerButton;
    private String selectedLanguageCode;
    private SessionManager sessionManager;
    public static final String TAG = "LanguageSelectActivity"; // Use final for constants

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_select);
        sessionManager = new SessionManager(this);

        initializeViews();
        setupClickListeners();

        // Set English as the default selected language
        selectedLanguageCode = MultiLanguageUtils.LANGUAGE_EN;
        updateSelectionUI();
    }

    private void initializeViews() {
        languageViewsMap = new HashMap<>();
        // ... (Your initializeViews code is perfect, no changes needed)
        TextView tvArabic = findViewById(R.id.tv_arabic);
        TextView tvChinese = findViewById(R.id.tv_simplified_chinese);
        TextView tvEnglish = findViewById(R.id.tv_english);
        TextView tvIndonesian = findViewById(R.id.tv_indonesian);
        TextView tvJapanese = findViewById(R.id.tv_japanese);
        TextView tvMalay = findViewById(R.id.tv_malay);
        TextView tvTurkish = findViewById(R.id.tv_turkish);

        tvArabic.setTag(MultiLanguageUtils.LANGUAGE_AR);
        tvChinese.setTag(MultiLanguageUtils.LANGUAGE_ZH);
        tvEnglish.setTag(MultiLanguageUtils.LANGUAGE_EN);
        tvIndonesian.setTag(MultiLanguageUtils.LANGUAGE_IN);
        tvJapanese.setTag(MultiLanguageUtils.LANGUAGE_JA);
        tvMalay.setTag(MultiLanguageUtils.LANGUAGE_MS);
        tvTurkish.setTag(MultiLanguageUtils.LANGUAGE_TR);

        languageViewsMap.put(MultiLanguageUtils.LANGUAGE_AR, tvArabic);
        languageViewsMap.put(MultiLanguageUtils.LANGUAGE_ZH, tvChinese);
        languageViewsMap.put(MultiLanguageUtils.LANGUAGE_EN, tvEnglish);
        languageViewsMap.put(MultiLanguageUtils.LANGUAGE_IN, tvIndonesian);
        languageViewsMap.put(MultiLanguageUtils.LANGUAGE_JA, tvJapanese);
        languageViewsMap.put(MultiLanguageUtils.LANGUAGE_MS, tvMalay);
        languageViewsMap.put(MultiLanguageUtils.LANGUAGE_TR, tvTurkish);

        registerButton = findViewById(R.id.registerButton);
    }

    private void setupClickListeners() {
        // ... (Your setupClickListeners code is perfect, no changes needed)
        View.OnClickListener languageClickListener = v -> {
            if (v.getTag() != null) {
                selectedLanguageCode = (String) v.getTag();
                updateSelectionUI();
            }
        };

        for (TextView textView : languageViewsMap.values()) {
            textView.setOnClickListener(languageClickListener);
        }

        registerButton.setOnClickListener(v -> saveAndProceed());
    }

    private void updateSelectionUI() {
        // ... (Your updateSelectionUI code is perfect, no changes needed)
        for (Map.Entry<String, TextView> entry : languageViewsMap.entrySet()) {
            String code = entry.getKey();
            TextView view = entry.getValue();

            if (code.equals(selectedLanguageCode)) {
                view.setTextColor(ContextCompat.getColor(this, R.color.green_accent));
                view.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_check_green, 0);
                view.setTypeface(null, Typeface.BOLD);
            } else {
                view.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                view.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                view.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    private void saveAndProceed() {
        if (selectedLanguageCode == null) {
            return;
        }

        // Apply the chosen language using your utility class
        // (Your switch statement is correct, no changes needed here)
        switch (selectedLanguageCode) {
            case MultiLanguageUtils.LANGUAGE_ZH:
                MultiLanguageUtils.changeLanguage(this, MultiLanguageUtils.LANGUAGE_ZH, MultiLanguageUtils.AREA_ZH);
                break;
            // ... Add all other cases here ...
            case MultiLanguageUtils.LANGUAGE_EN:
                MultiLanguageUtils.changeLanguage(this, MultiLanguageUtils.LANGUAGE_EN, MultiLanguageUtils.AREA_EN);
                break;
            case MultiLanguageUtils.LANGUAGE_AR:
                MultiLanguageUtils.changeLanguage(this, MultiLanguageUtils.LANGUAGE_AR, MultiLanguageUtils.AREA_AR);
                break;
            // ... etc. for all languages
        }

        // *** THIS IS THE CRITICAL FIX ***
        // Mark that the first-run language selection is complete.
        sessionManager.setFirstRunComplete(true);
        Log.d(TAG, "First run complete. Language set to: " + selectedLanguageCode);

        // Now, restart the app to apply the language change and let SplashActivity
        // decide the next destination (Login or Home). This is the most reliable way.
        Intent intent = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity(); // Finish this and all parent activities
        }
    }
}