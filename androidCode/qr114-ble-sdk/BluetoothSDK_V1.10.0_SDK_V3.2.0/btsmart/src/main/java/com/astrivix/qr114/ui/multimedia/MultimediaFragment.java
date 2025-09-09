package com.astrivix.qr114.ui.multimedia;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.astrivix.qr114.R;
import com.astrivix.qr114.constant.SConstant;
import com.astrivix.qr114.data.adapter.FunctionListAdapter;
import com.astrivix.qr114.data.model.FunctionItemData;
import com.astrivix.qr114.tool.bluetooth.rcsp.BTRcspHelper;
import com.astrivix.qr114.ui.CommonActivity;
import com.astrivix.qr114.ui.ContentActivity;
import com.astrivix.qr114.ui.alarm.AlarmListFragment;
import com.astrivix.qr114.ui.home.HomeActivity;
import com.astrivix.qr114.ui.light.LightContainerFragment;
import com.astrivix.qr114.ui.multimedia.control.BlankControlFragment;
import com.astrivix.qr114.ui.multimedia.control.BlankMusicControlFragment;
import com.astrivix.qr114.ui.multimedia.control.FMControlFragment;
import com.astrivix.qr114.ui.multimedia.control.FMTXControlFragment;
import com.astrivix.qr114.ui.multimedia.control.ID3EmptyControlFragment;
import com.astrivix.qr114.ui.multimedia.control.LineInControlFragment;
import com.astrivix.qr114.ui.multimedia.control.MusicControlFragment;
import com.astrivix.qr114.ui.multimedia.control.NetRadioControlFragment;
import com.astrivix.qr114.ui.multimedia.control.id3.ID3ControlFragment;
import com.astrivix.qr114.ui.multimedia.control.id3.ID3ControlPresenterImpl;
import com.astrivix.qr114.ui.music.device.ContainerFragment;
import com.astrivix.qr114.ui.music.local.LocalMusicFragment;
import com.astrivix.qr114.ui.music.net_radio.NetRadioFragment;
import com.astrivix.qr114.ui.search.SearchDeviceListFragment;
import com.astrivix.qr114.ui.soundcard.SoundCardFragment;
import com.astrivix.qr114.ui.widget.color_cardview.CardView;
import com.astrivix.qr114.util.JLShakeItManager;
import com.astrivix.qr114.util.JL_MediaPlayerServiceManager;
import com.astrivix.qr114.util.PermissionUtil;
import com.jieli.audio.media_player.Music;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.voice.VolumeInfo;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.component.base.BasePresenter;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.utils.ToastUtil;
import com.jieli.component.utils.ValueUtil;
import com.jieli.filebrowse.FileBrowseManager;
import com.jieli.filebrowse.bean.SDCardBean;
import com.jieli.jl_dialog.Jl_Dialog;
import java.util.ArrayList;
import java.util.List;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static com.astrivix.qr114.constant.SConstant.ALLOW_SWITCH_FUN_DISCONNECT;

@RuntimePermissions
public class MultimediaFragment extends Jl_BaseFragment implements IMultiMediaContract.IMultiMediaView {
    private final JLShakeItManager mShakeItManager = JLShakeItManager.getInstance();

    private FrameLayout flMultiMediaShadow;
    private FrameLayout flControlSuspension;
    private ImageView arcView;
    private CardView cvMultimediaContainerSuspension;
    private RecyclerView rvMultiMediaFunctionList;
    private FrameLayout flControl;
    private Fragment mSuspensionFragment;
    private IMultiMediaContract.IMultiMediaPresenter mPresenter;
    private FunctionListAdapter mAdapter;
    private Fragment mCurrentControlFragment;

    //device info card
    private CardView cvComplexDeviceCard;
    private TextView tvDeviceListStatus;
    private androidx.constraintlayout.widget.Group groupDeviceLeft;
    private ImageView ivDeviceListLeft;
    private TextView tvDeviceListLeftName;
    private TextView tvDeviceListLeftQuantity;
    private androidx.constraintlayout.widget.Group groupDeviceRight;
    private ImageView ivDeviceListRight;
    private TextView tvDeviceListRightName;
    private TextView tvDeviceListRightQuantity;
    private androidx.constraintlayout.widget.Group groupDeviceChargingBin;
    private ImageView ivDeviceListChargingBin;
    private TextView tvDeviceListChargingBinName;
    private TextView tvDeviceListChargingBinQuantity;

    // NEW: Add variables for volume and mute control
    private SeekBar sbVolumeControl;
    private TextView tvVolumePercentage;
    private ImageView ivVolumeIcon;
    private ConstraintLayout cl_volume_controller;
    private long sendVolTime;

    private boolean isMuted = false;
    private int lastVolume = 0;

    public static MultimediaFragment newInstance() {
        return new MultimediaFragment();
    }

    public MultimediaFragment() { /* Required empty public constructor */ }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_multimedia, container, false);
        initView(view);
        initVolumeControlListeners();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPresenter != null) {
            mPresenter.start();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getContext() == null) return;
        try {
            RCSPController.getInstance().addBTRcspEventCallback(mBTEventCallback);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add BT listener in onActivityCreated", e);
        }
        IntentFilter filter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        getContext().registerReceiver(mVolumeReceiver, filter);
        updateVolumeUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPresenter != null) {
            mPresenter.getDeviceSupportFunctionList();
            mPresenter.refreshDevMsg();
        }
        updateComplexDeviceCardView();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPresenter != null) {
            mPresenter.stopUpdateDevMsg();
        }
    }

    @Override
    public void onDestroyView() {
        if (mPresenter != null) {
            mPresenter.destroy();
        }
        if (getContext() != null) {
            try {
                getContext().unregisterReceiver(mVolumeReceiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "VolumeReceiver was not registered or already unregistered.");
            }
        }
        try {
            RCSPController controller = RCSPController.getInstance();
            if (controller != null && mBTEventCallback != null) {
                controller.removeBTRcspEventCallback(mBTEventCallback);
                Log.d(TAG, "Successfully removed BT listener in onDestroyView.");
            }
        } catch (Exception e) {
            Log.w(TAG, "RCSPController was already destroyed. Skipping listener removal in onDestroyView to prevent crash.");
        }
        super.onDestroyView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MultimediaFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void setPresenter(BasePresenter presenter) {
        if (mPresenter == null) {
            mPresenter = (IMultiMediaContract.IMultiMediaPresenter) presenter;
        }
    }

    private void initView(@NonNull View root) {
        flMultiMediaShadow = root.findViewById(R.id.fl_multimedia_shadow);
        flControlSuspension = root.findViewById(R.id.fl_control_suspension);
        arcView = root.findViewById(R.id.view_home_top_bg);
        cvMultimediaContainerSuspension = root.findViewById(R.id.cv_multimedia_container_suspension);
        rvMultiMediaFunctionList = root.findViewById(R.id.rv_multimedia_function_list);
        flControl = root.findViewById(R.id.fl_control);

        sbVolumeControl = root.findViewById(R.id.sb_volume_control);
        tvVolumePercentage = root.findViewById(R.id.tv_volume_percentage);
        cl_volume_controller = root.findViewById(R.id.cl_volume_controller);
        ivVolumeIcon = root.findViewById(R.id.iv_volume_icon); // Initialize the icon

        cvComplexDeviceCard = root.findViewById(R.id.cv_complex_device_card);
        tvDeviceListStatus = root.findViewById(R.id.tv_device_list_status);
        groupDeviceLeft = root.findViewById(R.id.group_device_left);
        ivDeviceListLeft = root.findViewById(R.id.iv_device_list_left);
        tvDeviceListLeftName = root.findViewById(R.id.tv_device_list_left_name);
        tvDeviceListLeftQuantity = root.findViewById(R.id.tv_device_list_left_quantity);
        groupDeviceRight = root.findViewById(R.id.group_device_right);
        ivDeviceListRight = root.findViewById(R.id.iv_device_list_right);
        tvDeviceListRightName = root.findViewById(R.id.tv_device_list_right_name);
        tvDeviceListRightQuantity = root.findViewById(R.id.tv_device_list_right_quantity);
        groupDeviceChargingBin = root.findViewById(R.id.group_device_charging_bin);
        ivDeviceListChargingBin = root.findViewById(R.id.iv_device_list_charging_bin);
        tvDeviceListChargingBinName = root.findViewById(R.id.tv_device_list_charging_bin_name);
        tvDeviceListChargingBinQuantity = root.findViewById(R.id.tv_device_list_charging_bin_quantity);

        mAdapter = new FunctionListAdapter();
        mPresenter = new MultiMediaPresenterImpl(this);
        rvMultiMediaFunctionList.setLayoutManager(new GridLayoutManager(getContext(), 1));
        int spacing = ValueUtil.dp2px(getContext(), 4);
        rvMultiMediaFunctionList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.right = spacing;
            }
        });
        rvMultiMediaFunctionList.setAdapter(mAdapter);

        if (!mPresenter.isDevConnected()) {
            mAdapter.setNewInstance(getFunctionItemDataList());
            cl_volume_controller.setVisibility(View.INVISIBLE);
        } else {
            cl_volume_controller.setVisibility(View.VISIBLE);
        }

        flMultiMediaShadow.setOnClickListener(v -> onDismissSuspensionShade());
        switchTopControlFragment(mPresenter.isDevConnected(), AttrAndFunCode.SYS_INFO_FUNCTION_BT);
        mAdapter.setOnItemClickListener((adapter, view1, position) -> {
            FunctionItemData itemData = (FunctionItemData) adapter.getData().get(position);
            if (!mPresenter.isDevConnected()) {
                if (SConstant.ALLOW_SWITCH_FUN_DISCONNECT) {
                    handleItemClickOnTest(itemData.getItemType());
                    return;
                } else {
                    if (itemData.getItemType() != FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SEARCH_DEVICE) {
                        ToastUtil.showToastShort(getString(R.string.first_connect_device));
                        return;
                    }
                }
            }
            if (!itemData.isSupport()) return;
            switch (itemData.getItemType()) {
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LOCAL:
                    goToLocalMusicFragment();
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SD_CARD:
                    if (!FileBrowseManager.getInstance().isOnline(SDCardBean.INDEX_SD0) && !FileBrowseManager.getInstance().isOnline(SDCardBean.INDEX_SD1)) {
                        ToastUtil.showToastShort(getString(R.string.msg_read_file_err_offline));
                        return;
                    }
                    goToDevStorage(SDCardBean.SD);
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_USB:
                    if (!FileBrowseManager.getInstance().isOnline(SDCardBean.INDEX_USB)) {
                        ToastUtil.showToastShort(getString(R.string.msg_read_file_err_offline));
                        return;
                    }
                    goToDevStorage(SDCardBean.USB);
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM_TX:
                    switchTopControlFragment(mPresenter.isDevConnected(), AttrAndFunCode.SYS_INFO_FUNCTION_FMTX);
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM:
                    mPresenter.switchToFMMode();
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LINEIN:
                    if (!FileBrowseManager.getInstance().isOnline(SDCardBean.INDEX_LINE_IN)) {
                        ToastUtil.showToastShort(getString(R.string.msg_read_file_err_offline));
                        return;
                    }
                    mPresenter.switchToLineInMode();
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LIGHT_SETTINGS:
                    ContentActivity.startActivity(getContext(), LightContainerFragment.class.getCanonicalName(), getString(R.string.multi_media_light_settings));
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_ALARM:
                    ContentActivity.startActivity(getContext(), AlarmListFragment.class.getCanonicalName(), getString(R.string.multi_media_alarm));
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SEARCH_DEVICE:
                    tryToRequestLocationPermission(0);
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_NET_RADIO:
                    tryToRequestLocationPermission(1);
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SOUND_CARD:
                    ContentActivity.startActivity(getContext(), SoundCardFragment.class.getCanonicalName(), R.string.multi_media_sound_card);
                    break;
            }
        });
    }

    private void initVolumeControlListeners() {
        sbVolumeControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (isMuted && progress > 0) {
                        isMuted = false;
                        updateMuteIcon();
                    }
                    int max = seekBar.getMax();
                    if (max > 0) {
                        int percentage = (int) (((float) progress / max) * 100);
                        tvVolumePercentage.setText(String.format("%d%%", percentage));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* Not needed */ }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setVolumeFromSeekBar(seekBar.getProgress());
            }
        });

        ivVolumeIcon.setOnClickListener(v -> toggleMute());
    }

    private void toggleMute() {
        if (!isMuted) {
            lastVolume = sbVolumeControl.getProgress();
            if (lastVolume == 0) { // If already at 0, set a default unmute level
                lastVolume = sbVolumeControl.getMax() / 2;
            }
            setVolumeFromSeekBar(0);
            isMuted = true;
        } else {
            setVolumeFromSeekBar(lastVolume);
            isMuted = false;
        }
        updateMuteIcon();
    }

    private void updateMuteIcon() {
        if (ivVolumeIcon != null) {
            int iconRes = isMuted ? R.drawable.ic_speaker_off : R.drawable.ic_speaker;
            ivVolumeIcon.setImageResource(iconRes);
        }
    }

    private void setVolumeFromSeekBar(int value) {
        if (mPresenter == null || !mPresenter.isDevConnected()) return;

        if (value > 0 && isMuted) {
            isMuted = false;
            updateMuteIcon();
        }

        DeviceInfo deviceInfo = mPresenter.getDeviceInfo();
        if (deviceInfo != null && deviceInfo.isSupportVolumeSync()) {
            if (getContext() == null) return;
            AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        } else {
            if (deviceInfo != null && value != deviceInfo.getVolume()) {
                sendVolTime = System.currentTimeMillis();
                BTRcspHelper.adjustVolume(RCSPController.getInstance(), requireContext(), value, new OnRcspActionCallback<Boolean>() {
                    @Override
                    public void onSuccess(BluetoothDevice device, Boolean message) { /* Success */ }
                    @Override
                    public void onError(BluetoothDevice device, BaseError error) {
                        if (isAdded()) {
                            ToastUtil.showToastShort(R.string.settings_failed);
                            updateVolumeUIFromDevice();
                        }
                    }
                });
            }
        }
    }

    private void updateVolumeUI() {
        if (mPresenter != null && mPresenter.isDevConnected()) {
            DeviceInfo deviceInfo = mPresenter.getDeviceInfo();
            if (deviceInfo != null && deviceInfo.isSupportVolumeSync()) {
                updateVolumeUIFromPhone();
            } else {
                updateVolumeUIFromDevice();
            }
        } else {
            sbVolumeControl.setMax(15);
            sbVolumeControl.setProgress(0);
            tvVolumePercentage.setText("0%");
        }
        updateMuteIcon();
    }

    private void updateVolumeUIFromPhone() {
        if (getContext() == null) return;
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        if (current > 0 && isMuted) {
            isMuted = false;
            updateMuteIcon();
        }

        sbVolumeControl.setMax(max);
        sbVolumeControl.setProgress(current);
        int percentage = (int) (((float) current / max) * 100);
        tvVolumePercentage.setText(String.format("%d%%", percentage));
    }

    private void updateVolumeUIFromDevice() {
        if (mPresenter == null || !mPresenter.isDevConnected()) return;
        DeviceInfo deviceInfo = mPresenter.getDeviceInfo();
        if (deviceInfo == null || deviceInfo.isSupportVolumeSync()) return;

        int current = deviceInfo.getVolume();
        int max = deviceInfo.getMaxVol();

        if (current > 0 && isMuted) {
            isMuted = false;
            updateMuteIcon();
        }

        if (max > 0) {
            sbVolumeControl.setMax(max);
            sbVolumeControl.setProgress(current);
            int percentage = (int) (((float) current / max) * 100);
            tvVolumePercentage.setText(String.format("%d%%", percentage));
        }
    }

    private final BTRcspEventCallback mBTEventCallback = new BTRcspEventCallback() {
        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (isAdded()) updateVolumeUI();
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            if (isAdded()) updateVolumeUI();
        }

        @Override
        public void onVolumeChange(BluetoothDevice device, VolumeInfo volume) {
            if (!isAdded() || sbVolumeControl.isPressed()) return;
            if (System.currentTimeMillis() - sendVolTime > 500) {
                if (mPresenter != null && mPresenter.isDevConnected()) {
                    DeviceInfo deviceInfo = mPresenter.getDeviceInfo();
                    if (deviceInfo != null && !deviceInfo.isSupportVolumeSync()) {
                        updateVolumeUIFromDevice();
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mVolumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPresenter == null || !mPresenter.isDevConnected()) return;
            if (intent.getAction() != null && intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")) {
                DeviceInfo deviceInfo = mPresenter.getDeviceInfo();
                if (deviceInfo != null && deviceInfo.isSupportVolumeSync()) {
                    updateVolumeUIFromPhone();
                }
            }
        }
    };


    // <editor-fold desc="Permission and Navigation boilerplate - no changes needed below this line">

    @NeedsPermission({ Manifest.permission.READ_EXTERNAL_STORAGE })
    public void onExternalStoragePermission() {
        toLocalMusicFragment();
    }

    @OnShowRationale({ Manifest.permission.READ_EXTERNAL_STORAGE })
    public void showRelationForExternalStoragePermission(PermissionRequest request) {
        showStoragePermissionDialog(request);
    }

    @OnPermissionDenied({ Manifest.permission.READ_EXTERNAL_STORAGE })
    public void onStorageDenied() {
        showAppSettingDialog(getString(R.string.permissions_tips_02) + getString(R.string.permission_storage));
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @NeedsPermission({ Manifest.permission.READ_MEDIA_AUDIO })
    public void onMediaAudioPermission() {
        toLocalMusicFragment();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @OnShowRationale({ Manifest.permission.READ_MEDIA_AUDIO })
    public void onMediaAudioPermissionShowRationale(PermissionRequest request) {
        showStoragePermissionDialog(request);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @OnPermissionDenied({ Manifest.permission.READ_MEDIA_AUDIO })
    public void onMediaAudioPermissionDenied() {
        showAppSettingDialog(getString(R.string.permissions_tips_02) + getString(R.string.permission_storage));
    }

    @NeedsPermission({ Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    public void goToFragmentByType(int type) {
        toNeedLocationPermissionFragment(type);
    }

    @OnShowRationale({
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    public void showRelationForLocationPermission(PermissionRequest request) {
        showLocationPermissionTipsDialog(request, -1);
    }

    @OnPermissionDenied({
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    public void onLocationDenied() {
        showAppSettingDialog(getString(R.string.permissions_tips_02) + getString(R.string.permission_location));
    }

    private void goToLocalMusicFragment() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionUtil.isHasPermission(requireContext(), Manifest.permission.READ_MEDIA_AUDIO)) {
                showStoragePermissionDialog(null);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!PermissionUtil.isHasStoragePermission(requireContext())) {
                showStoragePermissionDialog(null);
                return;
            }
        }
        toLocalMusicFragment();
    }

    private void toLocalMusicFragment() {
        JL_MediaPlayerServiceManager.getInstance().bindService();
        ContentActivity.startActivity(getContext(), LocalMusicFragment.class.getCanonicalName(),
                getString(R.string.multi_media_local));
    }

    private void tryToRequestLocationPermission(int type) {
        if (!PermissionUtil.isHasLocationPermission(requireContext())) {
            showLocationPermissionTipsDialog(null, type);
            return;
        }
        toNeedLocationPermissionFragment(type);
    }

    private void toNeedLocationPermissionFragment(int type) {
        if (type == 1) {
            CommonActivity.startCommonActivity(getActivity(), NetRadioFragment.class.getCanonicalName());
        } else {
            CommonActivity.startCommonActivity(getActivity(), SearchDeviceListFragment.class.getCanonicalName());
        }
    }

    private void showLocationPermissionTipsDialog(PermissionRequest request, int type) {
        Jl_Dialog dialog = Jl_Dialog.builder()
                .title(getString(R.string.permission))
                .titleColor(getResources().getColor(android.R.color.white))
                .contentColor(getResources().getColor(android.R.color.white))
                .content(getString(R.string.permissions_tips_01) + getString(R.string.permission_location))
                .right(getString(R.string.setting))
                .rightColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                .rightClickListener((v, dialogFragment) -> {
                    dialogFragment.dismiss();
                    if (request != null) {
                        request.proceed();
                    } else {
                        MultimediaFragmentPermissionsDispatcher.goToFragmentByTypeWithPermissionCheck(this, type);
                    }
                })
                .left(getString(R.string.cancel))
                .leftColor(ContextCompat.getColor(requireContext(), R.color.white))
                .leftClickListener((v, dialogFragment) -> dialogFragment.dismiss())
                .cancel(false)
                .build();
        dialog.show(getChildFragmentManager(), "request_location_permission");
    }

    private void showStoragePermissionDialog(PermissionRequest request) {
        Jl_Dialog dialog = Jl_Dialog.builder()
                .title(getString(R.string.permission))
                .titleColor(getResources().getColor(android.R.color.white))
                .contentColor(getResources().getColor(android.R.color.white))
                .content(getString(R.string.permissions_tips_01) + getString(R.string.permission_storage))
                .right(getString(R.string.setting))
                .rightColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                .rightClickListener((v, dialogFragment) -> {
                    dialogFragment.dismiss();
                    if (request != null) {
                        request.proceed();
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            MultimediaFragmentPermissionsDispatcher.onMediaAudioPermissionWithPermissionCheck(this);
                        } else {
                            MultimediaFragmentPermissionsDispatcher
                                    .onExternalStoragePermissionWithPermissionCheck(this);
                        }
                    }
                })
                .left(getString(R.string.cancel))
                .leftColor(ContextCompat.getColor(requireContext(), R.color.white))
                .leftClickListener((v, dialogFragment) -> {
                    dialogFragment.dismiss();
                    if (request != null)
                        request.cancel();
                })
                .cancel(false)
                .build();
        dialog.show(getChildFragmentManager(), "request_storage_permission");
    }

    private void showAppSettingDialog(String content) {
        Jl_Dialog jl_dialog = Jl_Dialog.builder()
                .title(getString(R.string.permission))
                .content(content)
                .right(getString(R.string.setting))
                .rightClickListener((v, dialogFragment) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                    dialogFragment.dismiss();
                })
                .left(getString(R.string.cancel))
                .leftClickListener((v, dialogFragment) -> {
                    dialogFragment.dismiss();
                })
                .build();
        jl_dialog.show(getChildFragmentManager(), "showAppSettingDialog");
    }

    private void handleItemClickOnTest(int type) {
        Bundle bundle = new Bundle();
        switch (type) {
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LOCAL:
                ContentActivity.startActivity(getContext(), LocalMusicFragment.class.getCanonicalName(),
                        getString(R.string.multi_media_local));
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SD_CARD:
                bundle.putInt(ContainerFragment.KEY_TYPE, 0);
                ContentActivity.startActivity(getContext(), ContainerFragment.class.getCanonicalName(), bundle);
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_USB:
                bundle.putInt(ContainerFragment.KEY_TYPE, 1);
                ContentActivity.startActivity(getContext(), ContainerFragment.class.getCanonicalName(), bundle);
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM_TX:
                switchTopControlFragment(true, AttrAndFunCode.SYS_INFO_FUNCTION_FMTX);
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM:
                switchTopControlFragment(true, AttrAndFunCode.SYS_INFO_FUNCTION_FM);
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LINEIN:
                switchTopControlFragment(true, AttrAndFunCode.SYS_INFO_FUNCTION_AUX);
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LIGHT_SETTINGS:
                ContentActivity.startActivity(getContext(), LightContainerFragment.class.getCanonicalName(),
                        getString(R.string.multi_media_light_settings));
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_ALARM:
                ContentActivity.startActivity(getContext(), AlarmListFragment.class.getCanonicalName(),
                        getString(R.string.multi_media_alarm));
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SEARCH_DEVICE:
                CommonActivity.startCommonActivity(getActivity(), SearchDeviceListFragment.class.getCanonicalName());
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_NET_RADIO:
                CommonActivity.startCommonActivity(getActivity(), NetRadioFragment.class.getCanonicalName());
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SOUND_CARD:
                ContentActivity.startActivity(getActivity(), SoundCardFragment.class.getCanonicalName(),
                        R.string.multi_media_sound_card);
                break;
        }
    }

    public ArrayList<FunctionItemData> getFunctionItemDataList() {
        String[] nameArray = getResources().getStringArray(R.array.multi_media_function_name_arr);
        TypedArray selectIconArray = getResources().obtainTypedArray(R.array.multi_media_function_sel_icon_arr);
        TypedArray unselectedIconArray = getResources().obtainTypedArray(R.array.multi_media_function_un_sel_icon_arr);
        ArrayList<FunctionItemData> list = new ArrayList<>();
        for (int i = 0; i < nameArray.length; i++) {
            FunctionItemData itemData = new FunctionItemData();
            String name = nameArray[i];
            int selIconResId = selectIconArray.getResourceId(i, R.drawable.ic_local_music_blue);
            int unSelIconResId = unselectedIconArray.getResourceId(i, R.drawable.ic_local_music_gray);
            itemData.setName(name);
            itemData.setNoSupportIconResId(unSelIconResId);
            itemData.setSupportIconResId(selIconResId);
            itemData.setItemType(i);
            itemData.setSupport(
                    itemData.getItemType() == FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SEARCH_DEVICE);
            list.add(itemData);
        }
        selectIconArray.recycle();
        unselectedIconArray.recycle();
        return list;
    }

    @Override
    public void updateFunctionItemDataList(ArrayList<FunctionItemData> arrayList) {
        if (isAdded() && !isDetached()) {
            ArrayList<FunctionItemData> filteredList = new ArrayList<>();
            if (arrayList != null) {
                for (FunctionItemData item : arrayList) {
                    if (item.getItemType() == FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SD_CARD && item.isSupport()) {
                        item.setName("Reciters ");
                        filteredList.add(item);
                        break;
                    }
                }
            }
            mAdapter.setNewInstance(filteredList);
        }
    }

    @Override
    public void switchTopControlFragment(boolean isConnect, byte fun) {
        JL_Log.d(TAG, "switchTopControlFragment= isConnect=" + isConnect + "\tfun=" + fun);
        Fragment fragment;
        if (!isConnect) {
            if (ALLOW_SWITCH_FUN_DISCONNECT && mCurrentControlFragment != null) {
                return;
            }
            fragment = BlankControlFragment.newInstanceForCache(getChildFragmentManager());
            dismissSuspensionFragment(mSuspensionFragment);
            onDismissSuspensionShade();
        } else {
            if (fun != AttrAndFunCode.SYS_INFO_FUNCTION_FM) {
                mHandler.removeMessages(MSG_DISMISS_SUSPENSION);
                mHandler.sendEmptyMessageDelayed(MSG_DISMISS_SUSPENSION, 70);
            }
            if (fun == AttrAndFunCode.SYS_INFO_FUNCTION_BT && mPresenter.getDeviceInfo() != null
                    && (!mPresenter.getDeviceInfo().isBtEnable()
                    || mPresenter.getDeviceInfo().isSupportDoubleConnection())) {
                fun = -1;
            }
            mShakeItManager.setCutSongType(JLShakeItManager.MODE_CUT_SONG_TYPE_DEFAULT);
            JL_Log.d(TAG, "switchTopControlFragment= after fun=" + fun);
            switch (fun) {
                case AttrAndFunCode.SYS_INFO_FUNCTION_BT:
                case AttrAndFunCode.SYS_INFO_FUNCTION_LOW_POWER:
                    List<Music> dataList = JL_MediaPlayerServiceManager.getInstance().getLocalMusic();
                    if (dataList == null || dataList.isEmpty()) {
                        fragment = BlankMusicControlFragment.newInstanceForCache(getChildFragmentManager());
                    } else {
                        fragment = MusicControlFragment.newInstanceForCache(getChildFragmentManager());
                    }
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC:
                    fragment = MusicControlFragment.newInstanceForCache(getChildFragmentManager());
                    break;
                case MultiMediaPresenterImpl.FRAGMENT_ID3:
                    mShakeItManager.setCutSongType(JLShakeItManager.MODE_CUT_SONG_TYPE_ID3);
                    if (mCurrentControlFragment instanceof ID3ControlFragment) return;
                    fragment = ID3ControlFragment.newInstanceForCache(getChildFragmentManager());
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(SConstant.KEY_ID3_INFO, mPresenter.getCurrentID3Info());
                    ((ID3ControlFragment) fragment).setBundle(bundle);
                    break;
                case MultiMediaPresenterImpl.FRAGMENT_ID3_EMPTY:
                    if (mCurrentControlFragment instanceof ID3EmptyControlFragment) return;
                    fragment = ID3EmptyControlFragment.newInstanceForCache(getChildFragmentManager());
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_FMTX:
                    mShakeItManager.setCutSongType(JLShakeItManager.MODE_CUT_SONG_TYPE_FM);
                    if (mCurrentControlFragment instanceof FMTXControlFragment) return;
                    fragment = FMTXControlFragment.newInstanceForCache(getChildFragmentManager());
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_FM:
                    mShakeItManager.setCutSongType(JLShakeItManager.MODE_CUT_SONG_TYPE_FM);
                    if (mSuspensionFragment instanceof FMControlFragment) return;
                    mHandler.removeMessages(MSG_DISMISS_SUSPENSION);
                    fragment = FMControlFragment.newInstanceForCache(getChildFragmentManager());
                    ((FMControlFragment) fragment).setFragmentCallback(new FMControlFragment.FragmentCallback() {
                        @Override
                        public void showFMSuspension() {
                            onShowSuspensionShade();
                        }

                        @Override
                        public void dismissFMSuspension() {
                            onDismissSuspensionShade();
                        }
                    });
                    showSuspensionFragment(fragment);
                    switchFunctionSelect(true, fun);
                    return;
                case AttrAndFunCode.SYS_INFO_FUNCTION_AUX:
                    fragment = LineInControlFragment.newInstanceForCache(getChildFragmentManager());
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_LIGHT:
                case MultiMediaPresenterImpl.FRAGMENT_NET_RADIO:
                    fragment = NetRadioControlFragment.newInstanceForCache(getChildFragmentManager());
                    break;
                default:
                    if (mPresenter.getDeviceInfo() != null && mPresenter.getDeviceInfo().isBtEnable()
                            && !mPresenter.getDeviceInfo().isSupportDoubleConnection()) {
                        fun = AttrAndFunCode.SYS_INFO_FUNCTION_BT;
                        List<Music> musicList = JL_MediaPlayerServiceManager.getInstance().getLocalMusic();
                        if (musicList == null || musicList.isEmpty()) {
                            fragment = BlankMusicControlFragment.newInstanceForCache(getChildFragmentManager());
                        } else {
                            fragment = MusicControlFragment.newInstanceForCache(getChildFragmentManager());
                        }
                    } else {
                        fragment = BlankControlFragment.newInstanceForCache(getChildFragmentManager());
                        Bundle bundle1 = new Bundle();
                        bundle1.putString(BlankControlFragment.KEY_CONTENT_TEXT, getString(R.string.no_data));
                        ((Jl_BaseFragment) fragment).setBundle(bundle1);
                        if (fragment.isAdded()) {
                            fragment.onResume();
                        }
                    }
                    break;
            }
        }
        switchFunctionSelect(isConnect, fun);
        if (fragment != null) {
            mCurrentControlFragment = fragment;
            JL_Log.d(TAG, "switchTopControlFragment= " + fragment.getClass().getSimpleName());
            changeFragment(R.id.fl_control, fragment, fragment.getClass().getSimpleName());
        }
    }

    private void switchFunctionSelect(boolean isSelect, Byte fun) {
        if (mAdapter == null || !isAdded() || isDetached()) return;
        if (!isSelect) {
            mAdapter.setSelectedType(-1);
        } else {
            switch (fun) {
                case AttrAndFunCode.SYS_INFO_FUNCTION_BT:
                case AttrAndFunCode.SYS_INFO_FUNCTION_LOW_POWER:
                    mAdapter.setSelectedType(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LOCAL);
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_AUX:
                    mAdapter.setSelectedType(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LINEIN);
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_FM:
                    mAdapter.setSelectedType(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM);
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_FMTX:
                    mAdapter.setSelectedType(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM_TX);
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC:
                    DeviceInfo deviceInfo = mPresenter.getDeviceInfo();
                    if (deviceInfo == null) return;
                    int deviceIndex = deviceInfo.getCurrentDevIndex();
                    if (deviceIndex == SDCardBean.INDEX_USB) {
                        mAdapter.setSelectedType(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_USB);
                    } else {
                        mAdapter.setSelectedType(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SD_CARD);
                    }
                    break;
                case MultiMediaPresenterImpl.FRAGMENT_NET_RADIO:
                    mAdapter.setSelectedType(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_NET_RADIO);
                    break;
                case MultiMediaPresenterImpl.FRAGMENT_ID3:
                case MultiMediaPresenterImpl.FRAGMENT_ID3_EMPTY:
                    mAdapter.setSelectedType(-1);
                    break;
            }
        }
    }

    @Override
    public void onBtAdapterStatus(boolean enable) { }

    @Override
    public void onDeviceConnection(BluetoothDevice device, int status) {
        if (status == StateCode.CONNECTION_OK && mPresenter != null && mPresenter.isUsedDevice(device)) {
            mPresenter.getDeviceSupportFunctionList();
            mPresenter.refreshDevMsg();
            mPresenter.getCurrentModeInfo();
        }
        updateComplexDeviceCardView();
        if (isAdded()) {
            updateVolumeUI();
        }
        if (!mPresenter.isDevConnected()) {
            mAdapter.setNewInstance(getFunctionItemDataList());
            cl_volume_controller.setVisibility(View.INVISIBLE);
        } else {
            cl_volume_controller.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSwitchDevice(BluetoothDevice device) {
        if (mPresenter != null) {
            mPresenter.getDeviceSupportFunctionList();
            mPresenter.refreshDevMsg();
            mPresenter.getCurrentModeInfo();
        }
        updateComplexDeviceCardView();
    }

    @Override
    public void onChangePlayerFlag(int flag) {
        JL_Log.w(TAG, "onChangePlayerFlag : " + flag);
        if (mCurrentControlFragment instanceof ID3ControlFragment) {
            ((ID3ControlFragment) mCurrentControlFragment).updateShowPlayerFlag(flag);
        }
        boolean isDevConnect = mPresenter.isDevConnected() || ALLOW_SWITCH_FUN_DISCONNECT;
        switch (flag) {
            case ID3ControlPresenterImpl.PLAYER_FLAG_LOCAL:
                switchTopControlFragment(isDevConnect, AttrAndFunCode.SYS_INFO_FUNCTION_BT);
                break;
            case ID3ControlPresenterImpl.PLAYER_FLAG_OTHER:
                JL_Log.i(TAG, "onChangePlayerFlag: switchTopControlFragment To ID3");
                switchTopControlFragment(isDevConnect, MultiMediaPresenterImpl.FRAGMENT_ID3);
                break;
            case ID3ControlPresenterImpl.PLAYER_FLAG_NET_RADIO:
                switchTopControlFragment(isDevConnect, MultiMediaPresenterImpl.FRAGMENT_NET_RADIO);
                break;
            case ID3ControlPresenterImpl.PLAYER_FLAG_OTHER_EMPTY:
                switchTopControlFragment(isDevConnect, MultiMediaPresenterImpl.FRAGMENT_ID3_EMPTY);
                break;
        }
    }

    private void showSuspensionFragment(Fragment fragment) {
        mSuspensionFragment = fragment;
        cvMultimediaContainerSuspension.setVisibility(View.VISIBLE);
        changeFragment(R.id.fl_control_suspension, mSuspensionFragment, mSuspensionFragment.getClass().getSimpleName());
    }

    private void dismissSuspensionFragment(Fragment fragment) {
        if (mSuspensionFragment != null) {
            cvMultimediaContainerSuspension.setVisibility(View.GONE);
            FragmentManager fragmentManager = getChildFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(mSuspensionFragment);
            fragmentTransaction.commitAllowingStateLoss();
            mSuspensionFragment = null;
        }
    }

    public void onShowSuspensionShade() {
        if (getActivity() == null) return;
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) arcView.getLayoutParams();
        arcView.setLayoutParams(layoutParams);
        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        alphaAnimation.setDuration(500);
        flMultiMediaShadow.startAnimation(alphaAnimation);
        flMultiMediaShadow.setVisibility(View.VISIBLE);
        cvMultimediaContainerSuspension.setVisibility(View.VISIBLE);
        arcView.setVisibility(View.VISIBLE);
    }

    private void onDismissSuspensionShade() {
        flMultiMediaShadow.setVisibility(View.GONE);
        arcView.setVisibility(View.GONE);
        if (mSuspensionFragment instanceof FMControlFragment) {
            ((FMControlFragment) mSuspensionFragment).setFreqCollectManageState(false);
            ((FMControlFragment) mSuspensionFragment).setSuspensionState(false);
        }
    }

    private int getOnLineDev(int type) {
        int devHandler = -1;
        List<SDCardBean> onLineDevices = FileBrowseManager.getInstance().getSdCardBeans();
        if (null != onLineDevices && !onLineDevices.isEmpty()) {
            for (SDCardBean sdCardBean : onLineDevices) {
                if (sdCardBean.getType() == type) {
                    devHandler = sdCardBean.getDevHandler();
                    break;
                }
            }
        }
        return devHandler;
    }

    private void toContainer(int type) {
        Bundle bundle = new Bundle();
        bundle.putInt(ContainerFragment.KEY_TYPE, type);
        ContentActivity.startActivity(getContext(), ContainerFragment.class.getCanonicalName(), bundle);
    }

    private void goToDevStorage(int type) {
        DeviceInfo deviceInfo = mPresenter.getDeviceInfo();
        if (deviceInfo != null && deviceInfo.getDevStorageInfo() != null && deviceInfo.getDevStorageInfo().isDeviceReuse()) {
            int devHandler = getOnLineDev(type);
            JL_Log.e(TAG, "goToDevStorage : " + devHandler + ", " + type);
            mPresenter.setDevStorage(devHandler, new OnRcspActionCallback<Boolean>() {
                @Override
                public void onSuccess(BluetoothDevice device, Boolean message) {
                    toContainer(type);
                }

                @Override
                public void onError(BluetoothDevice device, BaseError error) {
                    JL_Log.e(TAG, "goToDevStorage : " + error);
                    ToastUtil.showToastLong("设置存储设备出错\n" + error.getMessage());
                }
            });
            return;
        }
        toContainer(type);
    }

    private void updateComplexDeviceCardView() {
        if (mPresenter == null || !isAdded() || getContext() == null) return;
        BluetoothDevice device = mPresenter.getConnectedDevice();
        if (device != null && mPresenter.isDevConnected()) {
            cvComplexDeviceCard.setVisibility(View.VISIBLE);
            DeviceInfo deviceInfo = mPresenter.getDeviceInfo(device);
            com.jieli.bluetooth.bean.response.ADVInfoResponse advInfo = null;
            if (deviceInfo != null) {
                com.jieli.bluetooth.bean.DeviceStatus deviceStatus = com.jieli.bluetooth.tool.DeviceStatusManager.getInstance().getDeviceStatus(device.getAddress());
                if (deviceStatus != null) {
                    advInfo = deviceStatus.getADVInfo();
                }
            }
            boolean isUsing = mPresenter.isUsedDevice(device);
            tvDeviceListStatus.setVisibility(isUsing ? View.VISIBLE : View.GONE);
            if (isUsing) {
                tvDeviceListStatus.setText(R.string.device_status_connected);
            }
            if (advInfo != null) {
                boolean hasLeft = advInfo.getLeftDeviceQuantity() > 0;
                groupDeviceLeft.setVisibility(hasLeft ? View.VISIBLE : View.GONE);
                if (hasLeft) {
                    tvDeviceListLeftName.setText(com.astrivix.qr114.util.UIHelper.getCacheDeviceName(device));
                    updateQuantityUI(tvDeviceListLeftQuantity, advInfo.isLeftCharging(), advInfo.getLeftDeviceQuantity());
                }
                boolean hasRight = advInfo.getRightDeviceQuantity() > 0;
                groupDeviceRight.setVisibility(hasRight ? View.VISIBLE : View.GONE);
                if (hasRight) {
                    tvDeviceListRightName.setText(com.astrivix.qr114.util.UIHelper.getCacheDeviceName(device));
                    updateQuantityUI(tvDeviceListRightQuantity, advInfo.isRightCharging(), advInfo.getRightDeviceQuantity());
                }
                boolean hasCase = advInfo.getChargingBinQuantity() > 0;
                groupDeviceChargingBin.setVisibility(hasCase ? View.VISIBLE : View.GONE);
                if (hasCase) {
                    tvDeviceListChargingBinName.setText(getString(R.string.device_charging_bin, com.astrivix.qr114.util.UIHelper.getCacheDeviceName(device)));
                    updateQuantityUI(tvDeviceListChargingBinQuantity, advInfo.isDeviceCharging(), advInfo.getChargingBinQuantity());
                }
            } else {
                groupDeviceLeft.setVisibility(View.VISIBLE);
                tvDeviceListLeftName.setText(com.astrivix.qr114.util.UIHelper.getCacheDeviceName(device));
                tvDeviceListLeftQuantity.setText(R.string.device_status_connected);
                tvDeviceListLeftQuantity.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_dot_green_shape, 0, 0, 0);
                groupDeviceRight.setVisibility(View.GONE);
                groupDeviceChargingBin.setVisibility(View.GONE);
            }
        } else {
            cvComplexDeviceCard.setVisibility(View.GONE);
        }
    }

    private void updateQuantityUI(TextView textView, boolean isCharging, int quantity) {
        if (textView == null || getContext() == null) return;
        int batteryIconRes;
        if (isCharging) {
            batteryIconRes = R.drawable.ic_charging;
        } else {
            if (quantity < 20) batteryIconRes = R.drawable.ic_quantity_0;
            else if (quantity <= 35) batteryIconRes = R.drawable.ic_quantity_25;
            else if (quantity <= 50) batteryIconRes = R.drawable.ic_quantity_50;
            else if (quantity <= 75) batteryIconRes = R.drawable.ic_quantity_75;
            else batteryIconRes = R.drawable.ic_quantity_100;
        }
        textView.setText(String.format("%d%%", quantity));
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(batteryIconRes, 0, 0, 0);
    }

    private final int MSG_DISMISS_SUSPENSION = 1;
    private final Handler mHandler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == MSG_DISMISS_SUSPENSION) {
            dismissSuspensionFragment(mSuspensionFragment);
        }
        return false;
    });
    // </editor-fold>
}