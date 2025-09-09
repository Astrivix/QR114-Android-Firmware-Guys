package com.astrivix.qr114.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.utils.JL_Log;
import com.astrivix.qr114.R;
import com.astrivix.qr114.constant.SConstant;
import com.astrivix.qr114.data.model.settings.AppSettingsItem;
import com.astrivix.qr114.databinding.ActivityHomeBinding;
import com.astrivix.qr114.tool.playcontroller.PlayControlImpl;
import com.astrivix.qr114.tool.product.DefaultResFactory;
import com.astrivix.qr114.tool.room.AppDatabase;
import com.astrivix.qr114.ui.CommonActivity;
import com.astrivix.qr114.ui.base.BaseActivity;
import com.astrivix.qr114.ui.device.DeviceFragment;
import com.astrivix.qr114.ui.device.DeviceListFragment;
import com.astrivix.qr114.ui.device.DeviceListFragmentModify;
import com.astrivix.qr114.ui.eq.EqFragment;
import com.astrivix.qr114.ui.multimedia.MultimediaFragment;
import com.astrivix.qr114.ui.ota.FirmwareOtaFragment;
import com.astrivix.qr114.ui.settings.app.AppSettingsFragment;
import com.astrivix.qr114.ui.widget.DevicePopDialog.DevicePopDialog;
import com.astrivix.qr114.ui.widget.mydevice.MyDeviceDialog;
import com.astrivix.qr114.ui.widget.product_dialog.BleScanMessageHandler;
import com.astrivix.qr114.ui.widget.product_dialog.FloatingViewService;
import com.astrivix.qr114.util.AppPreferences;
import com.astrivix.qr114.util.AppUtil;
import com.astrivix.qr114.util.BleScanMsgCacheManager;
import com.astrivix.qr114.util.JLShakeItManager;
import com.astrivix.qr114.util.SessionManager;
import com.astrivix.qr114.util.UIHelper;
import com.jieli.component.ActivityManager;
import com.jieli.component.base.Jl_BaseActivity;
import com.jieli.component.utils.SystemUtil;
import com.jieli.component.utils.ToastUtil;
import com.jieli.jl_dialog.Jl_Dialog;

import java.util.List;
import java.util.Locale;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static com.astrivix.qr114.util.JLShakeItManager.MODE_CUT_SONG_TYPE_DEFAULT;

@RuntimePermissions
public class HomeActivity extends BaseActivity {
    public static final String HOME_ACTIVITY_RELOAD = "com.astrivix.qr114.HOME_ACTIVITY_RELOAD";
    private boolean isNeedReload = false;

    private ActivityHomeBinding mBinding;
    private HomeVM mViewModel;
    private MyDeviceDialog mMyDeviceDialog;
    private Jl_Dialog mMandatoryUpgradeDialog;
    private Jl_Dialog mSearchPhoneDialog;
    private Jl_Dialog mSwitchClassicDevTipsDialog;
    private SessionManager sessionManager;

    private final JLShakeItManager shakeItManager = JLShakeItManager.getInstance();

    private HomeReceiver mReceiver;

    /**
     * change and add profile fragmnet
     * sasanda saumya
     */
    private final Fragment[] fragments = new Fragment[] {
            MultimediaFragment.newInstance(),
            SConstant.IS_USE_DEVICE_LIST_FRAGMENT ? (SConstant.CHANG_DIALOG_WAY ? DeviceListFragmentModify.newInstance()
                    : DeviceListFragment.newInstance()) : DeviceFragment.newInstance(),
            new ProfileFragment() // Or ProfileFragment.newInstance() if you have it
    };

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemUtil.setImmersiveStateBar(getWindow(), true);
        mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        // setSupportActionBar(mBinding.viewToolbar.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        mViewModel = new ViewModelProvider(this).get(HomeVM.class);
        initUI();
        addObserver();
        registerReceiver();
        mViewModel.fastConnect(getApplicationContext());
        startForegroundService();

        sessionManager = new SessionManager(getApplicationContext());

        // Button button = findViewById(R.id.loginStatusBtn);
        // button.setOnClickListener(
        // new View.OnClickListener() {
        // @Override
        // public void onClick(View v) {
        // sessionManager.logoutUser();
        // Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        // startActivity(intent);
        // }
        // }
        // );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // HomeActivityPermissionsDispatcher.onRequestPermissionsResult(HomeActivity.this,
        // requestCode, grantResults);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mViewModel.isDevConnected()) {
            updateTopBarUI(mViewModel.getConnectedDevice(), StateCode.CONNECTION_OK);
        }
        sendActivityResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        dismissSwitchClassicDevTipsDialog();
    }

    // In HomeActivity.java

    @Override
    protected void onDestroy() {
        dismissMyDeviceDialog();
        dismissMandatoryUpgradeDialog();
        dismissSwitchClassicDevTipsDialog();
        super.onDestroy();
        unregisterReceiver();
        if (!isNeedReload) {
            // All these cleanup calls are good and should stay.
            BleScanMessageHandler.getInstance().release();
            shakeItManager.release();
            AppDatabase.getInstance().close();
            mViewModel.release();

            // --- THIS IS THE FIX ---
            // REMOVE this line. The Android OS is already handling the destruction of
            // activities
            // because of the FLAG_ACTIVITY_CLEAR_TASK used during logout. Calling this
            // while the activity is already being destroyed causes a crash.
            // ActivityManager.getInstance().popAllActivity(); // <-- DELETE OR COMMENT OUT
            // THIS LINE

            // These are also fine.
            stopService(new Intent(HomeActivity.this, DevicePopDialog.class));
            stopService(new Intent(HomeActivity.this, FloatingViewService.class));
        } else {
            isNeedReload = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (getCustomBackPress() != null && getCustomBackPress().onBack())
            return;
        if (AppUtil.isFastDoubleClick()) {
            // super.onBackPressed();
            finish(); // Exit App
        } else {
            ToastUtil.showToastShort(R.string.double_tap_to_exit);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    // public int getToolbarHeight() {
    // return mBinding.viewToolbar.getRoot().getHeight();
    // }

    public void resetFragment() {
        if (!isDestroyed()) {
            mBinding.navView.setSelectedItemId(R.id.tab_multimedia);
        }
    }

    public int getCurrentFragment() {
        if (!isFinishing() && !isDestroyed()) {
            return mBinding.vp2Home.getCurrentItem();
        }
        return 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @NeedsPermission({ Manifest.permission.POST_NOTIFICATIONS })
    public void onPostNotificationPermissionGrant() {
        JL_Log.d(TAG, "[onPostNotificationPermissionGrant] >>> ");
        startForegroundServiceForPop();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @OnShowRationale({ Manifest.permission.POST_NOTIFICATIONS })
    public void onPostNotificationPermissionShowRationale(@NonNull PermissionRequest request) {
        request.proceed();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @OnNeverAskAgain({ Manifest.permission.POST_NOTIFICATIONS })
    public void onPostNotificationPermissionNeverAsk() {
        ToastUtil.showToastShort(String.format(Locale.ENGLISH, "%s%s", getString(R.string.permissions_tips_02),
                getString(R.string.permission)));
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @OnPermissionDenied({ Manifest.permission.POST_NOTIFICATIONS })
    public void onPostNotificationPermissionDenied() {
        JL_Log.i(TAG, "[onPostNotificationPermissionDenied] >>> ");
        ToastUtil.showToastShort(String.format(Locale.ENGLISH, "%s%s", getString(R.string.permissions_tips_02),
                getString(R.string.permission)));
    }

    @SuppressLint("NonConstantResourceId")
    private void initUI() {
        mBinding.vp2Home.setOffscreenPageLimit(4);
        mBinding.vp2Home.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return fragments[position];
            }

            @Override
            public int getItemCount() {
                return fragments.length;
            }
        });
        View childView = mBinding.vp2Home.getChildAt(0);
        if (childView instanceof RecyclerView) {
            childView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
        mBinding.vp2Home.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            public void onPageScrollStateChanged(@ViewPager2.ScrollState int state) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    updateToolBarUI(mBinding.vp2Home.getCurrentItem() == 2);
                    mBinding.navView.setSelectedItemId(
                            mBinding.navView.getMenu().getItem(mBinding.vp2Home.getCurrentItem()).getItemId());
                }
            }
        });
        mBinding.vp2Home.setUserInputEnabled(true);
        mBinding.navView.setItemIconTintList(null);
        mBinding.navView.setOnNavigationItemSelectedListener(item -> {
            int pos;
            int itemId = item.getItemId();

            if (itemId == R.id.tab_multimedia) {
                pos = 0;
            } else if (itemId == R.id.tab_device) { // Assuming tab_device is your third item
                pos = 1;
            } else if (itemId == R.id.tab_profile) {
                pos = 2;
            } else {
                // Default to the first tab if something is wrong
                pos = 0;
            }

            // switch (item.getItemId()) {
            // case R.id.tab_multimedia:
            // pos = 0;
            // break;
            //// case R.id.tab_eq:
            //// pos = 1;
            //// break;
            // case R.id.tabMode:
            // pos = 1;
            // break;
            // default:
            // pos = 2;
            // break;
            // }

            // if(item.getItemId() == R.id.tab_logout) {
            // sessionManager.logoutUser();
            // Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            // startActivity(intent);
            // }

            updateToolBarUI(pos == 2);
            mBinding.vp2Home.setCurrentItem(pos, false);

            return true;
        });
        // mBinding.viewToolbar.tvToolbarTitle.setOnClickListener(v ->
        // showMyDeviceDialog());
        // mBinding.viewToolbar.tvToolbarAddDevice.setOnClickListener(v ->
        // sendBroadcast(new Intent(SConstant.ACTION_ADD_DEVICE)));

        mBinding.navView.setSelectedItemId(R.id.tab_multimedia);
        /*
         * int mediaPlayMode =
         * PreferencesHelper.getSharedPreferences(this).getInt(KEY_MEDIA_PLAY_MODE, -1);
         * if (mediaPlayMode == PlayControlImpl.MODE_NET_RADIO) {
         * JL_MediaPlayerServiceManager.getInstance().getJl_mediaPlayer().setData(
         * JL_MediaPlayerServiceManager.getInstance().getLocalMusic());
         * JL_MediaPlayerServiceManager.getInstance().play(0);
         * PlayControlImpl.getInstance().pause();
         * }
         */
        // This line is commented out because the 'view_home_top_bg' ImageView was
        // removed from activity_home.xml as part of the UI redesign.
        // mBinding.viewHomeTopBg.setTranslationY(-AppUtil.getStatusBarHeight(this));
        shakeItManager.getOnShakeItStartLiveData().observeForever(mode -> {
            if (mode == JLShakeItManager.SHAKE_IT_MODE_CUT_SONG && mRCSPController.isDeviceConnected()) {
                // if (PlayControlImpl.getInstance().isPlay())
                if (shakeItManager.getCutSongType() == MODE_CUT_SONG_TYPE_DEFAULT) {
                    PlayControlImpl.getInstance().playNext();
                }
            }
        });
    }

    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(this, deviceConnectionData -> {
            final BluetoothDevice device = deviceConnectionData.getDevice();
            final int status = deviceConnectionData.getStatus();
            updateTopBarUI(device, status);
            if (status == StateCode.CONNECTION_OK) {
                if (!isFinishing() && !isDestroyed()) {
                    if (mBinding.vp2Home.getCurrentItem() != 2) {
                        fragments[2].onPause();
                    }
                    if (mViewModel.isUsingDevice(device.getAddress())) {
                        initShakeIt(device);
                        dismissSwitchClassicDevTipsDialog();
                    }
                }
            }
            if (status == StateCode.CONNECTION_FAILED || status == StateCode.CONNECTION_DISCONNECT) {
                dismissMandatoryUpgradeDialog();
                dismissSwitchClassicDevTipsDialog();
            }
        });
        mViewModel.btAdapterMLD.observe(this, isEnable -> {
            if (isEnable)
                return;
            updateTopBarUI(null, StateCode.CONNECTION_DISCONNECT);
            dismissSwitchClassicDevTipsDialog();
        });
        mViewModel.switchDeviceMLD.observe(this, device -> {
            updateTopBarUI(device, StateCode.CONNECTION_OK);
            if (!isFinishing() && !isDestroyed()) {
                if (mBinding.vp2Home.getCurrentItem() != 2) {
                    fragments[2].onPause();
                }
            }
        });
        mViewModel.ringPlayStateMLD.observe(this, isRingPlaying -> {
            if (isRingPlaying) {
                showSearchPhoneDialog();
            } else {
                dismissSearchPhoneDialog();
            }
        });
        mViewModel.mandatoryUpgradeMLD.observe(this, this::showMandatoryUpgradeDialog);
        mViewModel.switchEdrParamMLD.observe(this,
                switchEdrParam -> showSwitchClassicDevTipsDialog(switchEdrParam.getEdrName(),
                        switchEdrParam.getTargetEdeName()));
    }

    private void initShakeIt(BluetoothDevice device) {
        List<AppSettingsItem> appSettingsItemList = shakeItManager.getSettingList(device);
        if (appSettingsItemList != null) {
            if (appSettingsItemList.size() > 0) {
                AppSettingsItem shakeCutSong = appSettingsItemList.get(0);
                shakeItManager.setEnableSupportCutSong(shakeCutSong.isEnableState());
            }
            if (appSettingsItemList.size() > 1) {
                AppSettingsItem shakeCutLightColor = appSettingsItemList.get(1);
                shakeItManager.setEnableSupportCutLightColor(shakeCutLightColor.isEnableState());
            }
        }
    }

    private void updateTopBarUI(BluetoothDevice device, int status) {
        // TextView tvTopLeft = mBinding.viewToolbar.tvToolbarTitle;

        String deviceName = getString(R.string.unconnected_device);
        int resId = 0;
        if (status == StateCode.CONNECTION_OK || status == StateCode.CONNECTION_CONNECTED
                || mViewModel.isDevConnected()) {
            DeviceInfo deviceInfo = mViewModel.getDeviceInfo();
            final BluetoothDevice connectedDevice = mViewModel.getConnectedDevice();
            deviceName = UIHelper.getCacheDeviceName(connectedDevice);
            HistoryBluetoothDevice historyBluetoothDevice = DeviceAddrManager.getInstance()
                    .findHistoryBluetoothDevice(connectedDevice);
            BleScanMessage bleScanMessage = null;
            if (deviceInfo != null) {
                bleScanMessage = BleScanMsgCacheManager.getInstance().getBleScanMessage(deviceInfo.getBleAddr());
            }
            resId = getDevDesignResId(historyBluetoothDevice, bleScanMessage);
        }
        if (!isFinishing() && !isDestroyed()) {
            // tvTopLeft.setText(deviceName);
            // tvTopLeft.setCompoundDrawablesWithIntrinsicBounds(resId, 0,
            // R.drawable.ic_down_arrows_white, 0);
        }

    }

    private int getDevDesignResId(HistoryBluetoothDevice historyBluetoothDevice, BleScanMessage bleScanMessage) {
        JL_Log.d(TAG, "getDevDesignResId: " + historyBluetoothDevice + " BleScanMessage: " + bleScanMessage);
        int version = -1;
        if (bleScanMessage != null) {
            version = bleScanMessage.getVersion();
        } else if (historyBluetoothDevice != null) {
            version = historyBluetoothDevice.getAdvVersion();
        }
        return DefaultResFactory
                .createBySdkType(historyBluetoothDevice == null ? -1 : historyBluetoothDevice.getChipType(), version)
                .getWhiteShowIcon();
    }

    private void showMyDeviceDialog() {
        if (mMyDeviceDialog == null) {
            mMyDeviceDialog = MyDeviceDialog.newInstance();
        }
        if (!mMyDeviceDialog.isShow() && !isDestroyed() && !isFinishing()) {
            mMyDeviceDialog.show(getSupportFragmentManager(), MyDeviceDialog.class.getSimpleName());
        }
    }

    private void dismissMyDeviceDialog() {
        if (mMyDeviceDialog != null) {
            if (mMyDeviceDialog.isShow()) {
                mMyDeviceDialog.dismiss();
            }
            mMyDeviceDialog = null;
        }
    }

    // private void initBottomBarTextTypeface() {
    // View view = navView.findViewById(R.id.tab_multimedia);
    // ((TextView)
    // view.findViewById(R.id.largeLabel)).setTypeface(Typeface.DEFAULT_BOLD);
    // ((TextView)
    // view.findViewById(R.id.smallLabel)).setTypeface(Typeface.DEFAULT_BOLD);
    // view = navView.findViewById(R.id.tabMode);
    // ((TextView)
    // view.findViewById(R.id.largeLabel)).setTypeface(Typeface.DEFAULT_BOLD);
    // ((TextView)
    // view.findViewById(R.id.smallLabel)).setTypeface(Typeface.DEFAULT_BOLD);
    // view = navView.findViewById(R.id.tab_device);
    // ((TextView)
    // view.findViewById(R.id.largeLabel)).setTypeface(Typeface.DEFAULT_BOLD);
    // ((TextView)
    // view.findViewById(R.id.smallLabel)).setTypeface(Typeface.DEFAULT_BOLD);
    // }

    private void sendActivityResume() {
        sendBroadcast(new Intent(SConstant.ACTION_ACTIVITY_RESUME));
    }

    private void showMandatoryUpgradeDialog(final BluetoothDevice device) {
        if (isFinishing() || isDestroyed())
            return;
        if (mMandatoryUpgradeDialog == null) {
            mMandatoryUpgradeDialog = Jl_Dialog.builder()
                    .title(getString(R.string.tips))
                    .content(getString(R.string.device_must_mandatory_upgrade, UIHelper.getDevName(device)))
                    .contentColor(getResources().getColor(R.color.black_242424))
                    .contentGravity(Gravity.START)
                    .cancel(false)
                    .left(getString(R.string.disconnect_device))
                    .leftColor(getResources().getColor(R.color.gray_text_989898))
                    .leftClickListener((v, dialogFragment) -> {
                        dismissMandatoryUpgradeDialog();
                        mViewModel.disconnect(device);
                    })
                    .right(getString(R.string.upgrade_immediately))
                    .rightColor(getResources().getColor(R.color.blue_448eff))
                    .rightClickListener((v, dialogFragment) -> {
                        dismissMandatoryUpgradeDialog();
                        if (ActivityManager.getInstance().getTopActivity() instanceof CommonActivity) {
                            CommonActivity activity = (CommonActivity) ActivityManager.getInstance().getTopActivity();
                            if (activity.getCurrentFragment() instanceof FirmwareOtaFragment) {
                                FirmwareOtaFragment otaFragment = (FirmwareOtaFragment) activity.getCurrentFragment();
                                otaFragment.resetOtaUI(Constants.FLAG_MANDATORY_UPGRADE);
                                return;
                            }
                        }
                        Bundle bundle = new Bundle();
                        bundle.putInt(SConstant.KEY_UPGRADE_STATUS, Constants.FLAG_MANDATORY_UPGRADE);
                        CommonActivity.startCommonActivity(HomeActivity.this,
                                FirmwareOtaFragment.class.getCanonicalName(), bundle);
                        sendBroadcast(new Intent(SConstant.ACTION_DEVICE_UPGRADE));
                    })
                    .build();
        }
        if (!mMandatoryUpgradeDialog.isShow()) {
            FragmentManager fm = getSupportFragmentManager();
            if (ActivityManager.getInstance().getTopActivity() != null
                    && !(ActivityManager.getInstance().getTopActivity() instanceof HomeActivity)) {
                fm = ((Jl_BaseActivity) ActivityManager.getInstance().getTopActivity()).getSupportFragmentManager();
            }
            mMandatoryUpgradeDialog.show(fm, "MandatoryUpgradeDialog");
        }
    }

    private void dismissMandatoryUpgradeDialog() {
        if (mMandatoryUpgradeDialog != null) {
            if (mMandatoryUpgradeDialog.isShow() && !isDestroyed()) {
                mMandatoryUpgradeDialog.dismiss();
            }
            mMandatoryUpgradeDialog = null;
        }
    }

    private void showSearchPhoneDialog() {
        if (mSearchPhoneDialog == null) {
            mSearchPhoneDialog = Jl_Dialog.builder()
                    .content(getString(R.string.search_phone))
                    .left(getString(R.string.close_play))
                    .cancel(false)
                    .leftColor(getResources().getColor(R.color.blue_448eff))
                    .leftClickListener((v, dialogFragment) -> {
                        dismissSearchPhoneDialog();
                        mViewModel.stopRing();
                    })
                    .build();
        }
        if (!mSearchPhoneDialog.isShow()) {
            FragmentManager fm = getSupportFragmentManager();
            if (ActivityManager.getInstance().getTopActivity() != null
                    && !(ActivityManager.getInstance().getTopActivity() instanceof HomeActivity)) {
                fm = ((Jl_BaseActivity) ActivityManager.getInstance().getTopActivity()).getSupportFragmentManager();
            }
            mSearchPhoneDialog.show(fm, "search_phone");
        }
    }

    private void dismissSearchPhoneDialog() {
        if (mSearchPhoneDialog != null) {
            if (mSearchPhoneDialog.isShow() && !isDestroyed()) {
                mSearchPhoneDialog.dismiss();
            }
            mSearchPhoneDialog = null;
        }
    }

    private void showSwitchClassicDevTipsDialog(String devName, String targetName) {
        if (mSwitchClassicDevTipsDialog == null) {
            String message = getString(R.string.switch_classic_device_tips, devName, targetName, targetName);
            mSwitchClassicDevTipsDialog = Jl_Dialog.builder()
                    .title(getString(R.string.dialog_tips))
                    .content(message)
                    .width(0.8f)
                    .left(getString(R.string.i_known))
                    .leftColor(getResources().getColor(R.color.blue_448eff))
                    .leftClickListener((v, dialogFragment) -> dismissSwitchClassicDevTipsDialog())
                    .build();
        }
        if (!mSwitchClassicDevTipsDialog.isShow() && !isDestroyed()) {
            mSwitchClassicDevTipsDialog.show(getSupportFragmentManager(), "switch_classic_device");
        }
    }

    private void dismissSwitchClassicDevTipsDialog() {
        if (mSwitchClassicDevTipsDialog != null) {
            if (mSwitchClassicDevTipsDialog.isShow() && !isDestroyed()) {
                mSwitchClassicDevTipsDialog.dismiss();
            }
            mSwitchClassicDevTipsDialog = null;
        }
    }

    private void updateToolBarUI(boolean isDeviceFragment) {
        if (isDestroyed() || !SConstant.IS_USE_DEVICE_LIST_FRAGMENT)
            return;
        // TextView tvTitle = mBinding.viewToolbar.tvToolbarTitle;
        // TextView tvAddDev = mBinding.viewToolbar.tvToolbarAddDevice;
        // if (isDeviceFragment) {
        // tvTitle.setVisibility(View.GONE);
        // tvAddDev.setVisibility(View.VISIBLE);
        // } else {
        // tvAddDev.setVisibility(View.GONE);
        // tvTitle.setVisibility(View.VISIBLE);
        // }
    }

    private void registerReceiver() {
        if (mReceiver == null) {
            mReceiver = new HomeReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(SConstant.ACTION_ACTIVE_DEVICE_CHANGED);
            intentFilter.addAction(HOME_ACTIVITY_RELOAD);
            registerReceiver(mReceiver, intentFilter);
        }
    }

    private void unregisterReceiver() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    private void reload() {
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    private void startForegroundService() {
        JL_Log.d(TAG, "[startForegroundService] >>> ");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // HomeActivityPermissionsDispatcher.onPostNotificationPermissionGrantWithPermissionCheck(HomeActivity.this);
            return;
        }
        JL_Log.d(TAG, "[startForegroundService][startForegroundServiceForPop] >>> ");
        startForegroundServiceForPop();
    }

    private void startForegroundServiceForPop() {
        Intent intent = new Intent(this, DevicePopDialog.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private class HomeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;
            String action = intent.getAction();
            if (TextUtils.isEmpty(action))
                return;
            switch (action) {
                case SConstant.ACTION_ACTIVE_DEVICE_CHANGED: {
                    dismissSwitchClassicDevTipsDialog();
                    break;
                }
                case HOME_ACTIVITY_RELOAD: {
                    isNeedReload = true;
                    reload();
                    break;
                }
            }
        }
    }
}