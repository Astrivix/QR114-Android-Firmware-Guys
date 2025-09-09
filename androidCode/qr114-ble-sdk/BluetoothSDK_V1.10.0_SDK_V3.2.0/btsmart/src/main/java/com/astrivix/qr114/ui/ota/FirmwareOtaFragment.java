package com.astrivix.qr114.ui.ota;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.tool.DeviceStatusManager;
import com.jieli.bluetooth.utils.JL_Log;
import com.astrivix.qr114.R;
import com.astrivix.qr114.constant.SConstant;
import com.astrivix.qr114.data.adapter.FirmwareOtaAdapter;
import com.astrivix.qr114.data.model.ota.FirmwareOtaItem;
import com.astrivix.qr114.data.model.ota.OtaStageInfo;
import com.astrivix.qr114.ui.CommonActivity;
import com.astrivix.qr114.ui.widget.upgrade_dialog.NotifyDialog;
import com.astrivix.qr114.ui.widget.upgrade_dialog.UpgradeProgressDialog;
import com.astrivix.qr114.util.AppUtil;
import com.astrivix.qr114.util.UIHelper;
import com.jieli.component.base.BasePresenter;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.utils.FileUtil;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.component.utils.StringUtil;
import com.jieli.component.utils.ToastUtil;
import com.jieli.jl_http.bean.OtaMessage;

// Make sure you have your OtaPresenterImpl class available.
// If it's in a different package, you may need to add an import statement for it.
// e.g., import com.your.package.name.OtaPresenterImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * 固件更新界面
 */
public class FirmwareOtaFragment extends Jl_BaseFragment implements IOtaContract.IOtaView {

    private RecyclerView rcFuncList;
    private FirmwareOtaAdapter mAdapter;
    private CommonActivity mActivity;
    private IOtaContract.IOtaPresenter mPresenter;
    private UpgradeProgressDialog mProgressDialog;
    private NotifyDialog mUpgradeDescDialog;
    private NotifyDialog mUpgradeResultDialog;
    private NotifyDialog mNotifyDialog;
    private int mUpgradeState = Constants.FLAG_NORMAL_UPGRADE;
    private String updateFilePath;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    public static final int FLAG_RESOURCE_UPGRADE = 3;

    public static FirmwareOtaFragment newInstance() {
        return new FirmwareOtaFragment();
    }

    public FirmwareOtaFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (mActivity == null && context instanceof CommonActivity) {
            mActivity = (CommonActivity) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_firmware_ota, container, false);
        rcFuncList = view.findViewById(R.id.rc_firmware_ota_func_list);
        initView(); // This will create the presenter
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mActivity == null && getActivity() instanceof CommonActivity) {
            mActivity = (CommonActivity) getActivity();
        }
        if (getBundle() != null) {
            mUpgradeState = getBundle().getInt(SConstant.KEY_UPGRADE_STATUS, Constants.FLAG_NORMAL_UPGRADE);
            updateFilePath = getBundle().getString(SConstant.KEY_UPGRADE_PATH);
            if (mUpgradeState == Constants.FLAG_MANDATORY_UPGRADE) {
                checkUpdateFileMessage();
            } else if (mUpgradeState == FLAG_RESOURCE_UPGRADE) {
                mPresenter.startFirmwareOta(updateFilePath);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPresenter != null) {
            mPresenter.start();
            // This is the correct, safe place to populate the UI.
            updateFuncList(mPresenter.getDeviceInfo());
        }
    }

    @Override
    public void onDestroyView() {
        if (mActivity != null && mActivity.getWindow() != null) {
            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        super.onDestroyView();
        JL_Log.w(TAG, "----onDestroyView >>>>>>");
        mHandler.removeCallbacksAndMessages(null);
        if (mPresenter != null) {
            mPresenter.destroy();
        }
        dismissUpgradeDescDialog();
        dismissUpgradeProgressDialog();
        dismissUpgradeResultDialog();
        dismissNotifyDialog();
        mActivity = null;
    }

    @Override
    public void onBtAdapterStatus(boolean enable) {
        if (!enable) {
            if (mActivity != null) {
                mActivity.onBackPressed();
            }
        }
    }

    @Override
    public void onDeviceConnection(BluetoothDevice device, int status) {
        if (isAdded() && mPresenter != null) {
            // Update UI when connection status changes to ensure it's always accurate
            mHandler.post(() -> updateFuncList(mPresenter.getDeviceInfo()));
        }

        if (status != StateCode.CONNECTION_OK && mPresenter != null && !mPresenter.isFirmwareOta()) {
            boolean isMandatoryUpgrade = DeviceStatusManager.getInstance().isMandatoryUpgrade(device);
            if (mActivity != null && !isMandatoryUpgrade) {
                mActivity.onBackPressed();
            }
        }
    }

    @Override
    public void onSwitchDevice(BluetoothDevice device) {
        if (isAdded() && mPresenter != null) {
            mHandler.post(() -> updateFuncList(mPresenter.getDeviceInfo()));
        }
    }

    @Override
    public void setPresenter(BasePresenter presenter) {
        if (mPresenter == null) {
            mPresenter = (IOtaContract.IOtaPresenter) presenter;
        }
    }

    private void initView() {
        // <<< FIX 1: Presenter is created here, just like your original code >>>
        mPresenter = new OtaPresenterImpl(this);

        if (mActivity == null && getActivity() instanceof CommonActivity) {
            mActivity = (CommonActivity) getActivity();
        }
        rcFuncList.setLayoutManager(new LinearLayoutManager(getContext()));
        if (mActivity != null) {
            mActivity.updateTopBar(getString(R.string.firmware_update), R.drawable.ic_back_black, v -> {
                if (mActivity != null) {
                    mActivity.onBackPressed();
                }
            }, 0, null);

            mActivity.setCustomBackPress(() -> {
                if (mPresenter != null && mPresenter.isFirmwareOta()) {
                    ToastUtil.showToastShort(R.string.ota_error_msg_disconnect_tip);
                    return true;
                } else if (mUpgradeState == Constants.FLAG_MANDATORY_UPGRADE) {
                    if (!AppUtil.isFastDoubleClick()) {
                        String message = getString(R.string.device_must_mandatory_upgrade, UIHelper.getDevName(mPresenter.getConnectedDevice())) + "\n" + getString(R.string.double_click_to_disconnect);
                        ToastUtil.showToastLong(message);
                        return true;
                    } else {
                        if (mPresenter != null && mPresenter.isDevConnected()) {
                            mPresenter.disconnectDevice();
                        }
                    }
                }
                return false;
            });
        }
        // <<< FIX 2: We do NOT update the list here. It's too early. It is now done in onStart(). >>>
    }

    private void updateFuncList(DeviceInfo deviceInfo) {
        if (!isAdded() || getContext() == null) return; // Add safety check

        // This check prevents a crash if device is null and correctly hides the view
        rcFuncList.setVisibility(deviceInfo == null ? View.GONE : View.VISIBLE);
        if (deviceInfo == null) {
            JL_Log.w(TAG, "updateFuncList: deviceInfo is null. Hiding the list.");
            return;
        }

        JL_Log.i(TAG, "updateFuncList: deviceInfo found. Populating UI.");
        String[] array = getResources().getStringArray(R.array.firmware_upgrade);
        List<FirmwareOtaItem> list = new ArrayList<>();
        for (String name : array) {
            FirmwareOtaItem item = new FirmwareOtaItem();
            item.setContent(name);
            if (getString(R.string.firmware_current_version).equals(name)) {
                item.setItemType(FirmwareOtaItem.LAYOUT_ONE);
                item.setValue(deviceInfo.getVersionName());
            } else if (getString(R.string.firmware_update).equals(name)) {
                item.setItemType(FirmwareOtaItem.LAYOUT_TWO);
                item.setOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_IDLE));
            }
            list.add(item);
        }

        if (mAdapter == null) {
            mAdapter = new FirmwareOtaAdapter(list);
            mAdapter.setOnFirmwareOtaListener(mOnFirmwareOtaListener);
            rcFuncList.setAdapter(mAdapter);
        } else {
            mAdapter.setNewInstance(list);
        }
    }

    private final FirmwareOtaAdapter.OnFirmwareOtaListener mOnFirmwareOtaListener = new FirmwareOtaAdapter.OnFirmwareOtaListener() {
        @Override
        public void onCheckUpgrade() {
            if (mPresenter != null) {
                String firmwareUrl = "https://qr-114-firmware.vercel.app/update.ufw";
                String localFilePath = mPresenter.getUpgradeFilePath();
                JL_Log.i(TAG, "onCheckUpgrade: Starting download from " + firmwareUrl + " to " + localFilePath);
                mPresenter.downloadFile(firmwareUrl, localFilePath);
            } else {
                ToastUtil.showToastShort("Error: Presenter not initialized.");
            }
        }

        @Override
        public void onDownload(OtaStageInfo info) {
            onCheckUpgrade();
        }

        @Override
        public void onShowMessage(String message) {
            if (mPresenter != null) {
                String title = getString(R.string.new_version_available);
                String explain = getString(R.string.update_file_ready);
                showUpgradeDescDialog(title, explain);
            }
        }

        @Override
        public void onStartOta() {
            if (mPresenter != null) {
                JL_Log.i(TAG, "onStartOta >> ");
                String filePath = mPresenter.getUpgradeFilePath();
                if (updateFilePath != null) {
                    filePath = updateFilePath;
                }
                mPresenter.startFirmwareOta(filePath);
            }
        }
    };

    // All the other methods (onOtaProgress, onOtaStop, dialogs, etc.) remain unchanged.
    // They are omitted here for brevity but should be in your file.

    @Override
    public void onOtaMessageChange(OtaMessage message) {
        if (mPresenter.judgeDeviceNeedToOta(mPresenter.getConnectedDevice(), message)) {
            updateOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_IDLE_DOWNLOAD, 0, getString(R.string.found_new_version, message.getVersion())));
        } else {
            ToastUtil.showToastShort(R.string.laster_version);
            updateOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_IDLE));
        }
    }

    @Override
    public void onGetOtaMessageError(int code, String message) {
        updateOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_IDLE));
        ToastUtil.showToastShort(R.string.network_err_tips);
    }

    @Override
    public void onDownloadStart(String path) {
        JL_Log.i(TAG, "---->onDownloadStart --- >" + path);
        updateOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_PREPARE, 0));
    }

    @Override
    public void onDownloadProgress(float progress) {
        JL_Log.d(TAG, "---->onDownloadProgress --- >" + progress);
        updateOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_PREPARE, getProgress(progress)));
    }

    @Override
    public void onDownloadStop(String path) {
        JL_Log.i(TAG, "---->onDownloadStop --- >" + path);
        updateOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_PREPARE, 100));
        mHandler.postDelayed(() -> {
            String message;
            if (mPresenter.getOtaMessage() != null && isAdded() && !isDetached()) {
                message = getString(R.string.found_new_version, mPresenter.getOtaMessage().getVersion());
            } else {
                message = getString(R.string.download_completed);
            }
            updateOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_UPGRADE, 100, message));
        }, 300);
    }

    @Override
    public void onDownloadError(int code, String message) {
        JL_Log.e(TAG, "---->onDownloadError --- >" + message);
        updateOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_IDLE));
        String msg = AppUtil.getContext().getString(R.string.ota_error_msg_download_ota_file_failed) + message;
        showNotifyDialog(msg);
    }

    @Override
    public void onOtaStart() {
        JL_Log.i(TAG, "---->onOtaStart --- >");
        mPresenter.setUpgradeState(mUpgradeState);
        if (mActivity != null && mActivity.getWindow() != null) {
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        dismissUpgradeDescDialog();
        updateOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_UPGRADING));
    }

    @Override
    public void onOtaProgress(int type, float progress) {
        JL_Log.i(TAG, "---->onOtaProgress --- > " + type + ", progress : " + progress);
        String text = type == 0 ? getString(R.string.upgrade_progress_prepare, getProgressValue(getProgress(progress)))
                : getString(R.string.upgrade_progress_update, getProgressValue(getProgress(progress)));
        showUpgradeProgressDialog(text, getProgress(progress));
    }

    @Override
    public void onOtaStop() {
        JL_Log.i(TAG, "---->onOtaStop --- > ");
        if (mUpgradeState == FLAG_RESOURCE_UPGRADE) {
            JL_Log.e(TAG, "send action : ACTION_DEVICE_UPGRADE_RESULT >>>> ret is ok. ");
            FileUtil.deleteFile(new File(updateFilePath));
            updateFilePath = null;

            Intent intent = new Intent(SConstant.ACTION_DEVICE_UPGRADE_RESULT);
            intent.putExtra(SConstant.KEY_UPGRADE_RESULT, 0);
            if (getActivity() != null) getActivity().sendBroadcast(intent);
        }
        dismissUpgradeProgressDialog();
        updateOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_IDLE));
        showUpgradeResultDialog(getString(R.string.upgrade_success), null, 0, 0,
                getString(R.string.confirm), R.color.blue_49A7FF, mUpgradeResultListener);
    }

    @Override
    public void onOtaCancel() {
        JL_Log.i(TAG, "---->onOtaCancel --- > ");
        dismissUpgradeProgressDialog();
        updateOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_IDLE));
        showUpgradeResultDialog(getString(R.string.upgrade_cancel), null, 0, 0,
                getString(R.string.confirm), R.color.blue_49A7FF, v -> {
                    dismissUpgradeResultDialog();
                    if (mActivity != null) {
                        mActivity.onBackPressed();
                    }
                });
    }

    @Override
    public void onOtaError(int code, String message) {
        JL_Log.w(TAG, "---->onOtaError --- > " + code + ", " + message);
        if (mUpgradeState == FLAG_RESOURCE_UPGRADE) {
            JL_Log.e(TAG, "send action : ACTION_DEVICE_UPGRADE_RESULT >>>> ret is failure. ");
            Intent intent = new Intent(SConstant.ACTION_DEVICE_UPGRADE_RESULT);
            intent.putExtra(SConstant.KEY_UPGRADE_RESULT, 1);
            if (getActivity() != null) getActivity().sendBroadcast(intent);
        }
        dismissUpgradeProgressDialog();
        updateOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_IDLE));
        showUpgradeResultDialog(message, null, 0, R.drawable.ic_yellow_fail,
                getString(R.string.confirm), R.color.blue_448eff, mUpgradeResultListener);
    }

    public void resetOtaUI(int state) {
        dismissNotifyDialog();
        dismissUpgradeResultDialog();
        mUpgradeState = state;
        if (mUpgradeState == Constants.FLAG_MANDATORY_UPGRADE) {
            checkUpdateFileMessage();
        }
    }

    private void checkUpdateFileMessage() {
        if (getActivity() != null && PreferencesHelper.getSharedPreferences(getActivity()).
                getBoolean(SConstant.KEY_LOCAL_OTA_TEST, SConstant.IS_LOCAL_OTA_TEST)) {
            checkLocalUpgradeFile();
            return;
        }
        if (mPresenter == null) return;
        DeviceInfo info = mPresenter.getDeviceInfo();
        if (info != null) {
            String md5 = DeviceStatusManager.getInstance().getDevMD5(mPresenter.getConnectedDevice());
            updateOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_IDLE, 50));
            mPresenter.checkFirmwareOtaService(info.getAuthKey(), info.getProjectCode(), md5);
        }
    }

    private void checkLocalUpgradeFile() {
        if (mPresenter == null || !isAdded() || isDetached()) return;
        String updateFilePath = mPresenter.getUpgradeFilePath();
        if (this.updateFilePath != null) {
            updateFilePath = this.updateFilePath;
        }
        JL_Log.d(TAG, "checkLocalUpgradeFile : " + updateFilePath);
        if (FileUtil.checkFileExist(updateFilePath)) {
            String tips = getString(R.string.found_new_version, "V_0.0.0.0");
            updateOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_UPGRADE, 0, tips));
        } else {
            ToastUtil.showToastLong(getString(R.string.ota_error_msg_file_not_found));
            updateOtaStageInfo(new OtaStageInfo(OtaStageInfo.STAGE_IDLE));
        }
    }

    private void updateOtaStageInfo(OtaStageInfo otaStageInfo) {
        if (getActivity() == null || isDetached() || !isAdded()) return;
        if (otaStageInfo != null && mAdapter != null) {
            FirmwareOtaItem firmwareOtaItem = null;
            for (FirmwareOtaItem item : mAdapter.getData()) {
                if (getString(R.string.firmware_update).equals(item.getContent())) {
                    firmwareOtaItem = item;
                    break;
                }
            }
            if (firmwareOtaItem != null) {
                firmwareOtaItem.setOtaStageInfo(otaStageInfo);
                final FirmwareOtaItem item = firmwareOtaItem;
                mHandler.post(() -> mAdapter.notifyItemChanged(mAdapter.getItemPosition(item)));
            }
        }
    }

    private void showUpgradeProgressDialog(String progressText, int progress) {
        if (mProgressDialog == null) {
            mProgressDialog = new UpgradeProgressDialog.Builder()
                    .setWidth(1f)
                    .setProgressText(progressText)
                    .setProgress(progress)
                    .setTips(getString(R.string.upgrade_warning))
                    .create();
        }
        mProgressDialog.updateView(mProgressDialog.getBuilder()
                .setProgressText(progressText)
                .setProgress(progress));
        if (!mProgressDialog.isShow() && !isDetached() && getActivity() != null) {
            mProgressDialog.show(getActivity().getSupportFragmentManager(), "upgrade_progress");
        }
    }

    private void dismissUpgradeProgressDialog() {
        if (mProgressDialog != null) {
            if (mProgressDialog.isShow()) {
                mProgressDialog.dismiss();
            }
            mProgressDialog = null;
        }
    }

    private void showUpgradeDescDialog(String title, String message) {
        if (mUpgradeResultDialog != null && mUpgradeResultDialog.isShow()) return;
        if (mUpgradeDescDialog == null) {
            mUpgradeDescDialog = new NotifyDialog.Builder()
                    .setWidth(1f)
                    .setCancel(true)
                    .setTitle(title)
                    .setMessage(message)
                    .setHasCloseBtn(true)
                    .create();
        }
        if (!mUpgradeDescDialog.isShow() && !isDetached() && getActivity() != null) {
            mUpgradeDescDialog.show(getActivity().getSupportFragmentManager(), "upgrade_desc");
        }
    }

    private void dismissUpgradeDescDialog() {
        if (mUpgradeDescDialog != null) {
            if (mUpgradeDescDialog.isShow()) {
                mUpgradeDescDialog.dismiss();
            }
            mUpgradeDescDialog = null;
        }
    }

    private void showUpgradeResultDialog(String title, String message, int messageColor, int messageImage,
                                         String btnText, int btnColor, View.OnClickListener listener) {
        if (mUpgradeState != 0) mUpgradeState = 0;
        if (mUpgradeResultDialog == null) {
            mUpgradeResultDialog = new NotifyDialog.Builder()
                    .setWidth(1f)
                    .setCancel(false)
                    .setTitleImg(messageImage)
                    .setTitle(title)
                    .setMessage(message)
                    .setMessageColor(messageColor)
                    .setLeftText(btnText)
                    .setLeftTextColor(btnColor)
                    .setLeftClickListener(listener)
                    .create();
        }
        if (!mUpgradeResultDialog.isShow() && !isDetached() && getActivity() != null) {
            mUpgradeResultDialog.show(getActivity().getSupportFragmentManager(), "upgrade_result");
            if (mActivity != null && mActivity.getWindow() != null) {
                mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    private void dismissUpgradeResultDialog() {
        if (mUpgradeResultDialog != null) {
            if (mUpgradeResultDialog.isShow()) {
                mUpgradeResultDialog.dismiss();
            }
            mUpgradeResultDialog = null;
        }
    }

    private void showNotifyDialog(String message) {
        if (!isAdded() || isDetached()) return;
        if (mNotifyDialog == null) {
            mNotifyDialog = new NotifyDialog.Builder()
                    .setWidth(1f)
                    .setCancel(false)
                    .setTitle(getString(R.string.dialog_tips))
                    .setMessage(message)
                    .setLeftText(getString(R.string.confirm))
                    .setLeftClickListener(v -> dismissNotifyDialog())
                    .create();
        }
        if (!mNotifyDialog.isShow() && isAdded() && !isDetached() && getActivity() != null) {
            mNotifyDialog.show(getActivity().getSupportFragmentManager(), "notify_dialog");
        }
    }

    private void dismissNotifyDialog() {
        if (mNotifyDialog != null) {
            if (mNotifyDialog.isShow() && !isDetached()) {
                mNotifyDialog.dismiss();
            }
            mNotifyDialog = null;
        }
    }

    private int getProgress(float progress) {
        int value = (int) Math.floor(progress);
        if (value > 100) {
            value = 100;
        }
        return value;
    }

    private String getProgressValue(int progress) {
        return progress + " %";
    }

    private final View.OnClickListener mUpgradeResultListener = v -> {
        dismissUpgradeResultDialog();
        if (mPresenter != null) {
            mPresenter.disconnectDevice();
        }
        if (mActivity != null) {
            mActivity.onBackPressed();
        }
    };
}