package com.astrivix.qr114.ui.device;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.command.tws.NotifyAdvInfoCmd;
import com.jieli.bluetooth.bean.command.tws.RequestAdvOpCmd;
import com.jieli.bluetooth.bean.device.DevBroadcastMsg;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.parameter.NotifyAdvInfoParam;
import com.jieli.bluetooth.bean.parameter.RequestAdvOpParam;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.IActionCallback;
import com.jieli.bluetooth.interfaces.OnReconnectHistoryRecordListener;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.astrivix.qr114.R;
import com.astrivix.qr114.constant.SConstant;
import com.astrivix.qr114.data.adapter.DeviceListAdapterModify;
import com.astrivix.qr114.data.model.device.HistoryDevice;
import com.astrivix.qr114.ui.CommonActivity;
import com.astrivix.qr114.ui.search.SearchDeviceFragment;
import com.astrivix.qr114.ui.settings.device.DeviceSettingsFragment;
import com.astrivix.qr114.ui.widget.LoadingDialog;
import com.astrivix.qr114.ui.widget.connect.ScanDeviceDialogModify;
import com.astrivix.qr114.util.NetworkStateHelper;
import com.astrivix.qr114.util.PermissionUtil;
import com.astrivix.qr114.util.UIHelper;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.utils.ToastUtil;
import com.jieli.jl_dialog.Jl_Dialog;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static android.content.Context.VIBRATOR_SERVICE;

/**
 * @author : Sasanda Saumya
 * @e-mail :
 * @date : 2025/07/26
 * @desc :
 */
@RuntimePermissions
public class DeviceListFragmentModify extends Jl_BaseFragment implements NetworkStateHelper.Listener {

    private DeviceListAdapterModify mAdapter;
    private ScanDeviceDialogModify scanDeviceDialog;
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private final Handler mUIHandler = new Handler(Looper.getMainLooper());
    private LoadingDialog mLoadingDialog;

    private DeviceStateReceiver mReceiver;
    // *** FIX 1: Make the button a member variable ***
    private View mAddDeviceButton;

    public static DeviceListFragmentModify newInstance() {
        return new DeviceListFragmentModify();
    }

    // In your DeviceListFragmentModify.java

    /**
     * Sasanda saumya
     * remove top bar and add add + button to scan dialog
     *
     * @return
     */
    @SuppressLint({ "ClickableViewAccessibility", "MissingPermission" })
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // 1. Inflate the layout that contains your new button
        View view = inflater.inflate(R.layout.fragment_device_list, container, false);

        // 2. Find the views from the layout
        RecyclerView recyclerView = view.findViewById(R.id.rl_device_list_container);
        // Find the new button from YOUR fragment's layout
        // *** FIX 2: Assign the button to the member variable ***
        mAddDeviceButton = view.findViewById(R.id.addDeviceButton);

        // 3. Setup the RecyclerView and Adapter (Your original code is perfect)
        mAdapter = new DeviceListAdapterModify(mRCSPController);
        mAdapter.setEmptyView(View.inflate(getContext(), R.layout.view_unconnected_device_1, null));
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // --- All your original listener setup code remains the same ---
        mAdapter.setOnItemClickListener((adapter, view13, position) -> {
            if (mAdapter.isEditMode())
                return;
            HistoryDevice historyDevice = mAdapter.getItem(position);
            BluetoothDevice device = BluetoothUtil.getRemoteDevice(historyDevice.getDevice().getAddress());
            if (mAdapter.isUsingDevice(historyDevice)) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(SConstant.KEY_ADV_INFO, null);
                CommonActivity.startCommonActivity(DeviceListFragmentModify.this,
                        SConstant.REQUEST_CODE_DEVICE_SETTINGS,
                        DeviceSettingsFragment.class.getCanonicalName(), bundle);
            } else if (mAdapter.isConnectedDevice(historyDevice)) {
                mRCSPController.switchUsingDevice(device);
            } else if (historyDevice.getState() == HistoryDevice.STATE_DISCONNECT) {
                historyDevice.setState(HistoryDevice.STATE_RECONNECT);
                mAdapter.notifyItemChanged(mAdapter.getItemPosition(historyDevice));
                mRCSPController.connectHistoryBtDevice(historyDevice.getDevice(), 0,
                        new OnReconnectHistoryRecordListener() {
                            @Override
                            public void onSuccess(HistoryBluetoothDevice history) {

                            }

                            @Override
                            public void onFailed(HistoryBluetoothDevice history, BaseError error) {
                                if (null == history)
                                    return;
                                final BluetoothDevice device1 = BluetoothUtil.getRemoteDevice(history.getAddress());
                                if (null != device1 && UIHelper.isHeadsetType(history.getChipType())) {
                                    if (history.getType() == BluetoothConstant.PROTOCOL_TYPE_SPP) {
                                        mRCSPController.getBtOperation().connectSPPDevice(device1);
                                    } else {
                                        mRCSPController.getBtOperation().connectBLEDevice(device1);
                                    }
                                }
                                if (mAdapter != null) {
                                    mAdapter.syncHistoryDevice();
                                }
                            }
                        });
            }
        });

        mAdapter.setOnItemLongClickListener((adapter, view12, position) -> {
            mAdapter.setEditMode(!mAdapter.isEditMode());
            if (getActivity() != null) {
                Vibrator vibrator = (Vibrator) requireActivity().getSystemService(VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build();
                    vibrator.vibrate(10, audioAttributes);
                }
            }
            return true;
        });

        mAdapter.addChildClickViewIds(R.id.iv_device_list_remove_history, R.id.iv_device_list_location);
        mAdapter.setOnItemChildClickListener((adapter, view1, position) -> {
            if (view1.getId() == R.id.iv_device_list_remove_history) {
                showIgnoreDeviceDialog(mAdapter.getItem(position).getDevice());
            } else if (view1.getId() == R.id.iv_device_list_location) {
                Bundle bundle = new Bundle();
                bundle.putString(SConstant.KEY_SEARCH_DEVICE_ADDR, mAdapter.getItem(position).getDevice().getAddress());
                CommonActivity.startCommonActivity(getActivity(), SearchDeviceFragment.class.getCanonicalName(),
                        bundle);
            }
        });

        // --- THIS IS THE FIX ---
        // 4. Set the click listener on the button from THIS fragment's layout
        mAddDeviceButton.setOnClickListener(v -> tryToShowScanDeviceDialog());

        // 5. REMOVE the old line that looks for the button in the activity
        // requireActivity().findViewById(R.id.tv_toolbar_add_device).setOnClickListener(v
        // -> tryToShowScanDeviceDialog());

        // --- The rest of your original code ---
        recyclerView.setOnTouchListener((v, event) -> {
            if (v.getId() != 0) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mAdapter.setEditMode(false);
                }
            }
            return false;
        });
        view.findViewById(R.id.tv_device_list_no_network)
                .setVisibility(NetworkStateHelper.getInstance().isNetworkIsAvailable() ? View.GONE : View.VISIBLE);
        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        DeviceListFragmentModifyPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);

    }

    // *** FIX 3: Create a method to control the button's visibility ***
    private void updateAddDeviceButtonVisibility() {
        if (mAddDeviceButton == null || !isAdded()) {
            return;
        }
        // Hide the button if a device is connected, show it otherwise.
        boolean isConnected = mRCSPController.isDeviceConnected();



        mAddDeviceButton.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        JL_Log.d(TAG, "updateAddDeviceButtonVisibility: isConnected=" + isConnected);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRCSPController.addBTRcspEventCallback(btEventCallback);
        NetworkStateHelper.getInstance().registerListener(this);
        // *** FIX 4: Call the update method to set the initial state ***
        updateAddDeviceButtonVisibility();
    }

    @Override
    public void onStart() {
        super.onStart();
        addReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (PermissionUtil.checkHasConnectPermission(requireContext())) {
            mAdapter.syncHistoryDevice();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        removeReceiver();
    }

    @Override
    public void onDestroyView() {
        mRCSPController.removeBTRcspEventCallback(btEventCallback);
        NetworkStateHelper.getInstance().unregisterListener(this);
        super.onDestroyView();
    }

    @NeedsPermission({ Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION, })
    public void requestLocationPermissionForScan(int type) {
        showScanDeviceDialog();
    }

    @OnShowRationale({
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
    })
    public void showRelationForLocationPermission(PermissionRequest request) {
        showLocationPermissionTipsDialog(request, 0);
    }

    @OnPermissionDenied({
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
    })
    public void onLocationDenied() {
        showAppSettingDialog(getString(R.string.permissions_tips_02) + getString(R.string.permission_location));
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @NeedsPermission({ Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN })
    public void requestBluetoothPermission(int type) {
        mAdapter.syncHistoryDevice();
        showLocationPermissionTipsDialog(null, type);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @OnShowRationale({ Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN })
    public void showBluetoothPermissionTips(PermissionRequest request) {
        showBluetoothPermissionTipsDialog(request, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @OnPermissionDenied({ Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN })
    public void bluetoothPermissionDenied() {
        showAppSettingDialog(getString(R.string.permissions_tips_02) + getString(R.string.permission_bluetooth));
    }

    private void tryToShowScanDeviceDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!PermissionUtil.checkHasScanPermission(requireContext())
                    || !PermissionUtil.checkHasConnectPermission(requireContext())) {
                showBluetoothPermissionTipsDialog(null, 1);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!PermissionUtil.isHasLocationPermission(requireContext())) {
                showLocationPermissionTipsDialog(null, 1);
                return;
            }
        }
        showScanDeviceDialog();
    }

    private void showScanDeviceDialog() {
        if (scanDeviceDialog == null) {
            scanDeviceDialog = new ScanDeviceDialogModify();
        }
        if (!scanDeviceDialog.isShow()) {
            scanDeviceDialog.show(getChildFragmentManager(), scanDeviceDialog.getClass().getCanonicalName());
        }
    }

    /**
     * Sasanda Saumya
     * permission title and colos chnage and etc
     */
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
                        DeviceListFragmentModifyPermissionsDispatcher
                                .requestLocationPermissionForScanWithPermissionCheck(this, type);
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
        dialog.show(getChildFragmentManager(), "request_location_permission");
    }

    /**
     * Sasanda Saumya
     * permission title and colos chnage and etc
     */
    private void showBluetoothPermissionTipsDialog(PermissionRequest request, int type) {
        Jl_Dialog jl_dialog = Jl_Dialog.builder()
                .title(getString(R.string.permission))
              .titleColor(getResources().getColor(android.R.color.white))
                    .contentColor(getResources().getColor(android.R.color.white))
                .content(getString(R.string.permissions_tips_01) + getString(R.string.permission_bluetooth))
                .right(getString(R.string.setting))
                .rightColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                .rightClickListener((v, dialogFragment) -> {
                    dialogFragment.dismiss();
                    if (null != request) {
                        request.proceed();
                    } else {
                        DeviceListFragmentModifyPermissionsDispatcher
                                .requestBluetoothPermissionWithPermissionCheck(this, type);
                    }
                })
                .left(getString(R.string.cancel))
                .leftColor(ContextCompat.getColor(requireContext(), R.color.gray_CECECE))
                .leftClickListener((v, dialogFragment) -> dialogFragment.dismiss())
                .cancel(false)
                .build();
        jl_dialog.show(getChildFragmentManager(), "request_bluetooth_permission");
    }

    private void showAppSettingDialog(String content) {
        Jl_Dialog jl_dialog = Jl_Dialog.builder()
                .title(getString(R.string.permission))
                .titleColor(getResources().getColor(android.R.color.white))
                    .contentColor(getResources().getColor(android.R.color.white))
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

    @Override
    public void onNetworkStateChange(int type, boolean available) {
        requireView().findViewById(R.id.tv_device_list_no_network).setVisibility(available ? View.GONE : View.VISIBLE);
    }

    private final BTRcspEventCallback btEventCallback = new BTRcspEventCallback() {
        @Override
        public void onConnection(BluetoothDevice device, int status) {
            JL_Log.i(TAG, "-onConnection- device : " + device + ", status = " + status);
            if (status == StateCode.CONNECTION_CONNECTING) {
                showConnectingDialog();
            } else {
                dismissConnectingDialog();
                if (status == StateCode.CONNECTION_OK) {
                    getAdvInfo(device);
                }
            }
            if (mAdapter != null && device != null) {
                mUIHandler.postDelayed(() -> mAdapter.updateHistoryDeviceByStatus(device, status), 300);
            }
            // *** FIX 5: Update button visibility whenever connection state changes ***
            mUIHandler.post(DeviceListFragmentModify.this::updateAddDeviceButtonVisibility);
        }

        @Override
        public void onBondStatus(BluetoothDevice device, int status) {
            JL_Log.d(TAG, "-onBondStatus- status = " + status);
            if (status == BluetoothDevice.BOND_NONE) {
                mUIHandler.postDelayed(() -> mAdapter.syncHistoryDevice(), 300);
            }
        }

        @Override
        public void onDeviceBroadcast(BluetoothDevice device, DevBroadcastMsg broadcast) {
            mAdapter.updateHistoryDeviceByBtDevice(device, UIHelper.convertADVInfoFromDevBroadcastMsg(broadcast));
        }

        @Override
        public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
            if (cmd.getId() == Command.CMD_ADV_DEVICE_NOTIFY) {
                NotifyAdvInfoCmd advInfoCmd = (NotifyAdvInfoCmd) cmd;
                NotifyAdvInfoParam param = advInfoCmd.getParam();
                BleScanMessage bleScanMessage = UIHelper.convertBleScanMsgFromNotifyADVInfo(param);
                ADVInfoResponse response = UIHelper.convertADVInfoFromBleScanMessage(bleScanMessage);
                mAdapter.updateHistoryDeviceByBtDevice(device, response);

                Log.i("sasa", "onDeviceCommand - scan device DeviceListFragment" + device);

            } else if (cmd.getId() == Command.CMD_ADV_DEV_REQUEST_OPERATION) {
                RequestAdvOpCmd requestAdvOpCmd = (RequestAdvOpCmd) cmd;
                RequestAdvOpParam param = requestAdvOpCmd.getParam();
                if (param != null) {
                    switch (param.getOp()) {
                        case Constants.ADV_REQUEST_OP_UPDATE_CONFIGURE: {
                            getAdvInfo(device);
                            break;
                        }
                        case Constants.ADV_REQUEST_OP_UPDATE_AFTER_REBOOT: {
                            // mView.devNeedReboot();
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            mAdapter.syncHistoryDevice();
            // *** FIX 6: Also update when switching between connected devices ***

            mUIHandler.post(DeviceListFragmentModify.this::updateAddDeviceButtonVisibility);

        }
    };

    private boolean isCanUseTws(BluetoothDevice device) {
        return mRCSPController.getDeviceInfo(device) != null
                && UIHelper.isCanUseTwsCmd(mRCSPController.getDeviceInfo(device).getSdkType());
    }

    private void getAdvInfo(BluetoothDevice device) {
        if (isCanUseTws(device)) {
            mRCSPController.getDeviceSettingsInfo(device, 0xffffffff, new OnRcspActionCallback<ADVInfoResponse>() {
                @Override
                public void onSuccess(BluetoothDevice device, ADVInfoResponse message) {
                    mAdapter.updateHistoryDeviceByBtDevice(device, message);
                }

                @Override
                public void onError(BluetoothDevice device, BaseError error) {

                }
            });
        }
    }

    private void showIgnoreDeviceDialog(final HistoryBluetoothDevice historyBluetoothDevice) {
        Jl_Dialog jl_dialog = Jl_Dialog.builder()
                .titleColor(getResources().getColor(R.color.white))
                .contentColor(getResources().getColor(R.color.white))
                .title(getString(R.string.tips))
                .content(getString(R.string.remove_history_device_tips))
                .cancel(false)
                .left(getString(R.string.cancel))
                .leftColor(getResources().getColor(R.color.gray_9A9A9A))
                .leftClickListener((v, dialogFragment) -> dialogFragment.dismiss())
                .right(getString(R.string.confirm))
                .rightColor(getResources().getColor(R.color.blue_text_color))
                .rightClickListener((v, dialogFragment) -> {
                    dialogFragment.dismiss();
                    mRCSPController.removeHistoryBtDevice(historyBluetoothDevice,
                            new IActionCallback<HistoryBluetoothDevice>() {
                                @Override
                                public void onSuccess(HistoryBluetoothDevice message) {
                                    mAdapter.syncHistoryDevice();
                                }

                                @Override
                                public void onError(BaseError error) {
                                    // TODO：这里也需要同步记录，可能是调用移除配对接口失败的情况
                                    mAdapter.syncHistoryDevice();
                                }
                            });
                })
                .build();
        jl_dialog.show(getChildFragmentManager(), jl_dialog.getClass().getCanonicalName());
    }

    private void showConnectingDialog() {
        if (isDetached() || !isAdded())
            return;
        if (scanDeviceDialog != null && scanDeviceDialog.isShow()) {
            scanDeviceDialog.dismiss();
            scanDeviceDialog = null;
        }
        if (mLoadingDialog == null) {
            mLoadingDialog = new LoadingDialog();
        }
        if (!mLoadingDialog.isShow()) {
            mLoadingDialog.show(getChildFragmentManager(), mLoadingDialog.getClass().getCanonicalName());
        }
    }

    private void dismissConnectingDialog() {
        if (isDetached() || !isAdded())
            return;
        if (mLoadingDialog != null) {
            if (mLoadingDialog.isShow()) {
                mLoadingDialog.dismiss();
            }
            mLoadingDialog = null;
        }
    }

    private void addReceiver() {
        if (mReceiver == null) {
            mReceiver = new DeviceStateReceiver();
            requireContext().registerReceiver(mReceiver, new IntentFilter(SConstant.ACTION_DEVICE_CONNECTION_CHANGE));
        }
    }

    private void removeReceiver() {
        if (null != mReceiver) {
            requireContext().unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    private final class DeviceStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent)
                return;
            String action = intent.getAction();
            if (TextUtils.isEmpty(action))
                return;
            if (action.equals(SConstant.ACTION_DEVICE_CONNECTION_CHANGE)) {
                int state = intent.getIntExtra(SConstant.KEY_CONNECTION, StateCode.CONNECTION_DISCONNECT);
                JL_Log.i(TAG, "[DeviceStateReceiver] >>> state = " + state);
                if (state == StateCode.CONNECTION_CONNECTING) {
                    showConnectingDialog();
                } else {
                    dismissConnectingDialog();
                }
            }
        }
    }
}
