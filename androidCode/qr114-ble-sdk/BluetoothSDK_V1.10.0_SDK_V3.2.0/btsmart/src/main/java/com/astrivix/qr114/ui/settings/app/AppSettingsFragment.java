package com.astrivix.qr114.ui.settings.app;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.astrivix.qr114.ui.home.HomeActivity;
import com.astrivix.qr114.ui.home.LoginActivity;
import com.astrivix.qr114.util.SessionManager;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.astrivix.qr114.MainApplication;
import com.astrivix.qr114.R;
import com.astrivix.qr114.constant.SConstant;
import com.astrivix.qr114.data.adapter.AppSettingsAdapter;
import com.astrivix.qr114.data.model.settings.AppSettingsItem;
import com.astrivix.qr114.ui.CommonActivity;
import com.astrivix.qr114.ui.ContentActivity;
import com.astrivix.qr114.util.JLShakeItManager;
import com.astrivix.qr114.util.MultiLanguageUtils;
import com.astrivix.qr114.util.PermissionUtil;
import com.astrivix.qr114.util.ProductUtil;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.component.utils.ToastUtil;
import com.jieli.jl_http.bean.ProductModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Des:App设置
 * Author: Bob
 * Date:20-5-16
 * UpdateRemark:
 */
public final class AppSettingsFragment extends Jl_BaseFragment {
    private static final int REQUEST_CODE_HIGH_SENSOR = 1299;

    private RecyclerView rvAppSettings;
    private AppSettingsAdapter adapter;
    private List<AppSettingsItem> mAppSettingsItemList;
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private final JLShakeItManager mShakeItManager = JLShakeItManager.getInstance();

    public static AppSettingsFragment newInstance() {
        return new AppSettingsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_settings, container, false);
        rvAppSettings = view.findViewById(R.id.app_settings_list);
        rvAppSettings.setLayoutManager(new LinearLayoutManager(MainApplication.getApplication()));
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mShakeItManager.saveSettingList(getConnectedDevice(), mAppSettingsItemList);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CommonActivity activity = (CommonActivity) getActivity();
        if (activity == null) {
            requireActivity().finish();
            return;
        }

        mRCSPController.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onConnection(BluetoothDevice device, int status) {
                updateAdapter(mRCSPController.isDeviceConnected());
            }
        });
        mAppSettingsItemList = mShakeItManager.getSettingList(getConnectedDevice());
        activity.updateTopBar(getString(R.string.setting), R.drawable.ic_back_black, v -> requireActivity().finish(), 0,
                null);
        adapter = new AppSettingsAdapter();
        adapter.setListener((appSettingsItem, isChecked) -> {
            if (!isHasSensorsPermission()) {
                JL_Log.d(TAG, "No HasSensorsPermission >>>>>>>>");
                requestSensorsPermission();
                appSettingsItem.setEnableState(false);
                adapter.notifyItemChanged(adapter.getItemPosition(appSettingsItem));
                return;
            } else {
                JL_Log.d(TAG, "HasSensorsPermission >>>>>>>>");
            }
            Integer settingNameSrc = appSettingsItem.getSettingNameSrc();
            if (settingNameSrc == R.string.shake_cut_song) {
                // sasanda saumya remove shake to play next song
                // AppSettingsItem changeSettingsItem = mAppSettingsItemList.get(0);
                // changeSettingsItem.setEnableState(isChecked);
                // mShakeItManager.setEnableSupportCutSong(isChecked);
            } else if (settingNameSrc == R.string.shake_change_light_color) {
                AppSettingsItem changeSettingsItem = mAppSettingsItemList.get(1);
                changeSettingsItem.setEnableState(isChecked);
                mShakeItManager.setEnableSupportCutLightColor(isChecked);
            }
        });
        adapter.setOnItemClickListener((adapter, v, position) -> {
            List<AppSettingsItem> dataList = (List<AppSettingsItem>) adapter.getData();
            AppSettingsItem appSettingsItem = dataList.get(position);

            if (appSettingsItem.getSettingNameSrc().equals(R.string.about)) {
                CommonActivity.startCommonActivity(getActivity(), AboutFragment.class.getCanonicalName());
                return;
            } else if (appSettingsItem.getSettingNameSrc().equals(R.string.device_instructions)) {
                if (SConstant.CHANG_DIALOG_WAY) {
                    ContentActivity.startActivity(getContext(),
                            DeviceInstructionFragmentModify.class.getCanonicalName(), R.string.device_instructions);
                    return;
                }

                if (mRCSPController.isDeviceConnected()) {
                    DeviceInfo deviceInfo = getDeviceInfo();
                    String scene = ProductUtil.isChinese() ? ProductModel.MODEL_PRODUCT_INSTRUCTIONS_CN.getValue()
                            : ProductModel.MODEL_PRODUCT_INSTRUCTIONS_EN.getValue();
                    String url = ProductUtil.findCacheDesign(MainApplication.getApplication(), deviceInfo.getVid(),
                            deviceInfo.getUid(),
                            deviceInfo.getPid(), scene);
                    JL_Log.d(TAG, ">>>> DeviceInstructionFragment : " + deviceInfo.getVid() + ", " + deviceInfo.getUid()
                            + ", " + deviceInfo.getPid());
                    Bundle bundle = new Bundle();
                    bundle.putString(SConstant.KEY_DEV_INSTRUCTION_PATH, url);
                    CommonActivity.startCommonActivity(getActivity(),
                            DeviceInstructionFragment.class.getCanonicalName(), bundle);
                } else {
                    ToastUtil.showToastShort(R.string.first_connect_device);
                }
                return;
            } else if (appSettingsItem.getSettingNameSrc().equals(R.string.multilingual)) {
                Log.d(TAG, "onActivityCreated: LanguageSetFragment");
                CommonActivity.startCommonActivity(getActivity(), LanguageSetFragment.class.getCanonicalName());
                return;
            } else if (appSettingsItem.getSettingNameSrc().equals(R.string.feedback)) {
                CommonActivity.startCommonActivity(getActivity(), FeedBackFragment.class.getCanonicalName());
                return;
            }

            // switch (appSettingsItem.getSettingNameSrc()) {
            // case R.string.about:
            // CommonActivity.startCommonActivity(getActivity(),
            // AboutFragment.class.getCanonicalName());
            // break;
            // case R.string.device_instructions:
            // if (SConstant.CHANG_DIALOG_WAY) {
            // ContentActivity.startActivity(getContext(),
            // DeviceInstructionFragmentModify.class.getCanonicalName(),
            // R.string.device_instructions);
            // return;
            // }
            //
            // if (mRCSPController.isDeviceConnected()) {
            // DeviceInfo deviceInfo = getDeviceInfo();
            // String scene = ProductUtil.isChinese() ?
            // ProductModel.MODEL_PRODUCT_INSTRUCTIONS_CN.getValue()
            // : ProductModel.MODEL_PRODUCT_INSTRUCTIONS_EN.getValue();
            // String url = ProductUtil.findCacheDesign(MainApplication.getApplication(),
            // deviceInfo.getVid(), deviceInfo.getUid(),
            // deviceInfo.getPid(), scene);
            // JL_Log.d(TAG, ">>>> DeviceInstructionFragment : " + deviceInfo.getVid() + ",
            // " + deviceInfo.getUid() + ", " + deviceInfo.getPid());
            // Bundle bundle = new Bundle();
            // bundle.putString(SConstant.KEY_DEV_INSTRUCTION_PATH, url);
            // CommonActivity.startCommonActivity(getActivity(),
            // DeviceInstructionFragment.class.getCanonicalName(), bundle);
            // } else {
            // ToastUtil.showToastShort(R.string.first_connect_device);
            // }
            // break;
            // case R.string.multilingual:
            // Log.d(TAG, "onActivityCreated: LanguageSetFragment");
            // CommonActivity.startCommonActivity(getActivity(),
            // LanguageSetFragment.class.getCanonicalName());
            // break;
            // case R.string.feedback:
            // CommonActivity.startCommonActivity(getActivity(),
            // FeedBackFragment.class.getCanonicalName());
            // break;
            // }
        });
        rvAppSettings.setAdapter(adapter);

        updateAdapter(mRCSPController.isDeviceConnected());
    }

    private void updateAdapter(boolean isConnected) {
        if (!isAdded() || isDetached())
            return;
        ArrayList<AppSettingsItem> tempList = new ArrayList<>();
        JL_Log.d(TAG,
                "updateAdapter : isConnected = " + isConnected + ", mAppSettingsItemList = " + mAppSettingsItemList);
        if (isConnected) {
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
            JL_Log.d(TAG, "updateAdapter : " + deviceInfo.isBtEnable() + ", " + deviceInfo.isDevMusicEnable());
            if (!deviceInfo.isSupportDoubleConnection() && (deviceInfo.isBtEnable() || deviceInfo.isDevMusicEnable())) {
                // sasanda saumya remove shake to play next song
                // AppSettingsItem shakeCutSongSettingsItem =
                // getSettingItem(R.string.shake_cut_song);
                // if (shakeCutSongSettingsItem == null) {
                // shakeCutSongSettingsItem = new AppSettingsItem();
                // shakeCutSongSettingsItem.setSettingNameSrc(R.string.shake_cut_song);
                // shakeCutSongSettingsItem.setSettingType(AppSettingsItem.TYPE_SWITCH);
                // }
                // tempList.add(shakeCutSongSettingsItem);
            }
            JL_Log.d(TAG, "updateAdapter : " + deviceInfo.isLightEnable());
            if (deviceInfo.isLightEnable()) {
                AppSettingsItem shakeCutLightColorSettingsItem = getSettingItem(R.string.shake_change_light_color);
                if (shakeCutLightColorSettingsItem == null) {
                    shakeCutLightColorSettingsItem = new AppSettingsItem();
                    shakeCutLightColorSettingsItem.setSettingNameSrc(R.string.shake_change_light_color);
                    shakeCutLightColorSettingsItem.setSettingType(AppSettingsItem.TYPE_SWITCH);
                    shakeCutLightColorSettingsItem.setSettingNoteSrc(R.string.shake_change_light_color_note);
                }
                tempList.add(shakeCutLightColorSettingsItem);
            }
        }
        TypedArray titles = getResources().obtainTypedArray(R.array.app_settings_list);
        for (int i = 0; i < titles.length(); i++) {
            int nameResId = titles.getResourceId(i, R.string.about);
            if (nameResId == R.string.device_instructions && !mRCSPController.isDeviceConnected()) {
                JL_Log.i(TAG, "-onActivityCreated- skip");
                Log.i("sasa", titles.getString(i) + " click");
                continue;
            }
            AppSettingsItem item = getSettingItem(nameResId);
            if (item == null) {
                item = new AppSettingsItem();
                if (nameResId == R.string.multilingual) {// 多语言
                    Log.i("sasa", nameResId + " click");

                    String languageTailString = getString(R.string.follow_system);
                    String language = PreferencesHelper.getSharedPreferences(MainApplication.getApplication())
                            .getString(MultiLanguageUtils.SP_LANGUAGE, MultiLanguageUtils.LANGUAGE_AUTO);
                    Log.i("sasa", language + " language");

                    // sasa edit
                    if (TextUtils.equals(language, MultiLanguageUtils.LANGUAGE_ZH)) {
                        Log.i("sasa", language + " LANGUAGE_ZH");
                        languageTailString = getString(R.string.simplified_chinese);
                    } else if (TextUtils.equals(language, MultiLanguageUtils.LANGUAGE_EN)) {
                        Log.i("sasa", language + " LANGUAGE_EN");
                        languageTailString = getString(R.string.english);
                    } else if (TextUtils.equals(language, MultiLanguageUtils.LANGUAGE_JA)) {
                        Log.i("sasa", language + " LANGUAGE_JA");
                        languageTailString = getString(R.string.japanese);
                    } else if (TextUtils.equals(language, MultiLanguageUtils.LANGUAGE_IN)) {
                        Log.i("sasa", language + " LANGUAGE_IN");
                        languageTailString = getString(R.string.indonesian);
                    }
                    item.setTailString(languageTailString);
                }
                item.setSettingNameSrc(nameResId);
            }
            tempList.add(item);
        }
        titles.recycle();
        mAppSettingsItemList = new ArrayList<>(tempList);
        adapter.setNewInstance(mAppSettingsItemList);
    }

    private BluetoothDevice getConnectedDevice() {
        return mRCSPController.getUsingDevice();
    }

    private DeviceInfo getDeviceInfo() {
        return mRCSPController.getDeviceInfo();
    }

    private boolean isHasSensorsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PermissionUtil.isHasPermission(requireContext(), Manifest.permission.HIGH_SAMPLING_RATE_SENSORS);
        }
        return true;
    }

    private void requestSensorsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            JL_Log.d(TAG, "requestSensorsPermission >>>>>>>>");
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[] { Manifest.permission.HIGH_SAMPLING_RATE_SENSORS }, REQUEST_CODE_HIGH_SENSOR);
        }
    }

    private AppSettingsItem getSettingItem(int srcName) {
        if (mAppSettingsItemList == null || mAppSettingsItemList.isEmpty())
            return null;
        AppSettingsItem item = null;
        for (AppSettingsItem settingsItem : mAppSettingsItemList) {
            if (settingsItem.getSettingNameSrc() != null && settingsItem.getSettingNameSrc() == srcName) {
                item = settingsItem;
                break;
            }
        }
        return item;
    }
}
