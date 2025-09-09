// File: com/astrivix/qr114/ui/settings/app/LanguageSetFragment.java
package com.astrivix.qr114.ui.settings.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;
import com.astrivix.qr114.MainApplication;
import com.astrivix.qr114.R;
import com.astrivix.qr114.databinding.FragmentLanguageSetBinding;
import com.astrivix.qr114.ui.CommonActivity;
import com.astrivix.qr114.ui.home.HomeActivity;
import com.astrivix.qr114.util.MultiLanguageUtils;
import com.jieli.component.ActivityManager;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.component.utils.ValueUtil;

import static com.astrivix.qr114.util.MultiLanguageUtils.LANGUAGE_AUTO;

public class LanguageSetFragment extends Jl_BaseFragment {

    private FragmentLanguageSetBinding binding;
    private String selectLanguage = null;
    private String setLanguage = null;
    private TextView mConfirmTextView;

    public static LanguageSetFragment newInstance() {
        return new LanguageSetFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_language_set, container, false);
        // Set tags
        binding.tvFollowSystem.setTag(LANGUAGE_AUTO);
        binding.tvSimplifiedChinese.setTag(MultiLanguageUtils.LANGUAGE_ZH);
        binding.tvEnglish.setTag(MultiLanguageUtils.LANGUAGE_EN);
        binding.tvJapanese.setTag(MultiLanguageUtils.LANGUAGE_JA);
        binding.tvIndonesian.setTag(MultiLanguageUtils.LANGUAGE_IN);
        binding.tvTurkish.setTag(MultiLanguageUtils.LANGUAGE_TR);
        binding.tvMalay.setTag(MultiLanguageUtils.LANGUAGE_MS);
        binding.tvArabic.setTag(MultiLanguageUtils.LANGUAGE_AR);
        initClick();
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        CommonActivity activity = (CommonActivity) getActivity();
        if (activity != null) {
            // *** FIX: CREATE THE BACK BUTTON IMAGE VIEW ***
            ImageView backButton = new ImageView(getContext());
            // Assuming you have a back arrow drawable. Replace with your actual drawable.
            backButton.setImageResource(R.drawable.ic_back_black);
            backButton.setOnClickListener(v -> activity.onBackPressed());
            TextView textView = new TextView(getContext());
            textView.setText(R.string.confirm);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0,0, ValueUtil.dp2px(getContext(),16),0);
            textView.setLayoutParams(params);
            textView.setOnClickListener(v -> saveChange());
            textView.setTextColor(getResources().getColor(R.color.gray_CECECE));
            textView.setTextSize(16);
            mConfirmTextView = textView;
            activity.updateTopBar(getString(R.string.set_language), backButton, textView);
        }
        setLanguage = PreferencesHelper.getSharedPreferences(MainApplication.getApplication()).getString(MultiLanguageUtils.SP_LANGUAGE, MultiLanguageUtils.LANGUAGE_AUTO);
        selectLanguage = setLanguage;
        updateSelectView();
        if (mConfirmTextView != null) {
            updateConfirmButton();
        }
    }

    private void initClick() {
        View.OnClickListener listener = v -> {
            if (v.getTag() != null) {
                selectLanguage = (String) v.getTag();
                updateSelectView();
                if (mConfirmTextView != null) {
                    updateConfirmButton();
                }
            }
        };

        binding.tvFollowSystem.setOnClickListener(listener);
        binding.tvSimplifiedChinese.setOnClickListener(listener);
        binding.tvEnglish.setOnClickListener(listener);
        binding.tvJapanese.setOnClickListener(listener);
        binding.tvIndonesian.setOnClickListener(listener);
        binding.tvTurkish.setOnClickListener(listener);
        binding.tvMalay.setOnClickListener(listener);
        binding.tvArabic.setOnClickListener(listener);
    }

    private void updateConfirmButton() {
        mConfirmTextView.setTextColor(getResources().getColor(!isSetLanguage(selectLanguage) ? R.color.blue_448eff : R.color.gray_CECECE));
        mConfirmTextView.setEnabled(!isSetLanguage(selectLanguage));
    }

    private void updateSelectView() {
        TextView[] views = {
                binding.tvFollowSystem, binding.tvSimplifiedChinese, binding.tvEnglish, binding.tvJapanese,
                binding.tvIndonesian, binding.tvTurkish, binding.tvMalay, binding.tvArabic
        };
        for (TextView textView : views) {
            String tag = (String) textView.getTag();
            if (tag != null && tag.equals(selectLanguage)) {
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_check_green, 0);
            } else {
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            }
        }
    }

    private boolean isSetLanguage(String language) {
        return TextUtils.equals(setLanguage, language);
    }

    private void saveChange() {
        Context context = MainApplication.getApplication();
        // *** FIX: COMPLETED THE SWITCH STATEMENT ***
        switch (selectLanguage) {
            case LANGUAGE_AUTO:
                MultiLanguageUtils.changeLanguage(context, LANGUAGE_AUTO, MultiLanguageUtils.AREA_AUTO);
                break;
            case MultiLanguageUtils.LANGUAGE_ZH:
                MultiLanguageUtils.changeLanguage(context, MultiLanguageUtils.LANGUAGE_ZH, MultiLanguageUtils.AREA_ZH);
                break;
            case MultiLanguageUtils.LANGUAGE_EN:
                MultiLanguageUtils.changeLanguage(context, MultiLanguageUtils.LANGUAGE_EN, MultiLanguageUtils.AREA_EN);
                break;
            case MultiLanguageUtils.LANGUAGE_JA:
                MultiLanguageUtils.changeLanguage(context, MultiLanguageUtils.LANGUAGE_JA, MultiLanguageUtils.AREA_JA);
                break;
            case MultiLanguageUtils.LANGUAGE_IN:
                MultiLanguageUtils.changeLanguage(context, MultiLanguageUtils.LANGUAGE_IN, MultiLanguageUtils.AREA_IN);
                break;
            case MultiLanguageUtils.LANGUAGE_TR:
                MultiLanguageUtils.changeLanguage(context, MultiLanguageUtils.LANGUAGE_TR, MultiLanguageUtils.AREA_TR);
                break;
            case MultiLanguageUtils.LANGUAGE_MS:
                MultiLanguageUtils.changeLanguage(context, MultiLanguageUtils.LANGUAGE_MS, MultiLanguageUtils.AREA_MS);
                break;
            case MultiLanguageUtils.LANGUAGE_AR:
                MultiLanguageUtils.changeLanguage(context, MultiLanguageUtils.LANGUAGE_AR, MultiLanguageUtils.AREA_AR);
                break;
        }

        // *** FIX: USING A MORE RELIABLE APP RESTART METHOD ***
        if (getActivity() == null) return;
        final Intent intent = getActivity().getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getActivity().getBaseContext().getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finishAffinity(); // Finishes all activities
        }
    }
}