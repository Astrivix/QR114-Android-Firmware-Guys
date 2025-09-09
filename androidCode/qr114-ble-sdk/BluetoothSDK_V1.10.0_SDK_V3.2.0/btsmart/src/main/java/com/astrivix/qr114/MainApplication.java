package com.astrivix.qr114;

import android.app.Application;
import android.content.Context;

import com.github.gzuliyujiang.oaid.DeviceIdentifier;
import com.jieli.bluetooth.bean.BluetoothOption;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.astrivix.qr114.constant.SConstant;
import com.astrivix.qr114.tool.bluetooth.rcsp.RcspEventHandleTask;
import com.astrivix.qr114.tool.bluetooth.rcsp.ScanBleDeviceTask;
import com.astrivix.qr114.tool.bluetooth.rcsp.UploadDeviceInfoTask;
import com.astrivix.qr114.tool.network.NetworkDetectionHelper;
import com.astrivix.qr114.tool.product.ProductCacheManager;
import com.astrivix.qr114.tool.room.AppDatabase;
import com.astrivix.qr114.tool.room.DataRepository;
import com.astrivix.qr114.util.AppUtil;
import com.astrivix.qr114.util.BleScanMsgCacheManager;
import com.astrivix.qr114.util.EqCacheUtil;
import com.astrivix.qr114.util.MultiLanguageUtils;
import com.astrivix.qr114.util.NetworkStateHelper;
import com.jieli.component.ActivityManager;
import com.jieli.component.Logcat;
import com.jieli.component.utils.FileUtil;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.component.utils.SystemUtil;
import com.jieli.component.utils.ToastUtil;
import com.jieli.component.utils.ValueUtil;
import com.jieli.jl_http.BuildConfig;
import com.jieli.jl_http.JL_HttpClient;
import com.jieli.jl_http.bean.AppInfo;
import com.jieli.jl_http.bean.LogResponse;
import com.jieli.jl_http.interfaces.IActionListener;
import com.jieli.jl_http.util.Constant;
import com.tencent.bugly.crashreport.CrashReport;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Main program entrance
 *
 * <p>
 * Function: Mainly completes the initialization of the library and other operations. No time-consuming operations required.
 * </p>
 *
 * @author sasanda saumya
 * @date 2020/5/12
 */
public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.getSimpleName();
    private static MainApplication sApplication;
    private LogResponse mLogResponse;
    AppDatabase database;
    DataRepository dataRepository;
    Executor mDiskIO;

    public static String OAID;
    private boolean isNeedBanDialog = false; //Do you need to disable pop-ups?
    private boolean isOTA = false; //Whether to perform OTA

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
        init();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        RCSPController.getInstance().destroy();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        handleLog(false, false);
    }

    public void init() {
        EqCacheUtil.clear();//Reset the eq cache when temporarily reopening the app. You can release it if necessary.
        Logcat.setIsLog(BuildConfig.DEBUG);
        CommonUtil.setMainContext(this);
        if (!DeviceAddrManager.isInit()) {
            DeviceAddrManager.init(this);
        }
        ToastUtil.init(this);
        NetworkDetectionHelper.init(this);
        ActivityManager.init(this);
        handleLog(BuildConfig.DEBUG, BuildConfig.DEBUG);
        JL_HttpClient.getInstance(this);

        mDiskIO = Executors.newSingleThreadExecutor();
        database = AppDatabase.getInstance(getApplicationContext(), mDiskIO);
        dataRepository = DataRepository.getInstance(database);
        AppUtil.getAllDeviceSupportSearchStatus();
        //init rcsp wrapper
        if (!RCSPController.isInit()) {
            //config RCSPController
            BluetoothOption bluetoothOption = BluetoothOption.createDefaultOption()
                    .setPriority(BluetoothOption.PREFER_BLE)
                    .setUseDeviceAuth(PreferencesHelper.getSharedPreferences(MainApplication.getApplication())
                            .getBoolean(SConstant.KEY_USE_DEVICE_AUTH, SConstant.IS_USE_DEVICE_AUTH));
            RCSPController.init(this, bluetoothOption);
        }

        if (SConstant.CHANG_DIALOG_WAY) {

            NetworkStateHelper.getInstance();

            RCSPController controller = RCSPController.getInstance();
            controller.addBTRcspEventCallback(ProductCacheManager.getInstance());
            controller.addBTRcspEventCallback(new RcspEventHandleTask(controller));
            controller.addBTRcspEventCallback(new ScanBleDeviceTask(this, controller));
            controller.addBTRcspEventCallback(new UploadDeviceInfoTask());
            controller.addBTRcspEventCallback(BleScanMsgCacheManager.getInstance());

        }
        registerActivityLifecycleCallbacks(MultiLanguageUtils.callbacks);
    }

    public void uploadAppInfo() {
        if (PreferencesHelper.getSharedPreferences(getApplicationContext()).
                getBoolean(SConstant.KEY_LOCAL_OTA_TEST, SConstant.IS_LOCAL_OTA_TEST)) {
            FileUtil.createFilePath(sApplication, sApplication.getPackageName(), SConstant.DIR_UPDATE);
        }
        if (mLogResponse == null) {
            AppInfo appInfo = new AppInfo();
            appInfo.setUuid(OAID);
            appInfo.setAppName(SystemUtil.getAppName(this));
            appInfo.setAppVersion(SystemUtil.getVersionName(this));
            appInfo.setPlatform(Constant.PLATFORM_ANDROID_L);
            appInfo.setBrand(SystemUtil.getDeviceBrand());
            appInfo.setSystem(SystemUtil.getDeviceManufacturer());
            appInfo.setVersion(SystemUtil.getSystemVersion());
            appInfo.setPhoneName(SystemUtil.getSystemModel());
            JL_Log.i("zzc", "uploadAppInfo : " + appInfo);
            JL_HttpClient.getInstance().uploadAppInfo(appInfo, new IActionListener<LogResponse>() {
                @Override
                public void onSuccess(LogResponse response) {
                    JL_Log.i(TAG, "uploadAppInfo>> onSuccess : " + response);
                    mLogResponse = response;
                }

                @Override
                public void onError(int code, String message) {
                    JL_Log.i(TAG, "uploadAppInfo>> onError : " + code + ", " + message);
                }
            });
        }
    }

    public LogResponse getLogResponse() {
        return mLogResponse;
    }

    public static MainApplication getApplication() {
        return sApplication;
    }

    public boolean isNeedBanDialog() {
        return isNeedBanDialog;
    }

    public void setNeedBanDialog(boolean needBanDialog) {
        isNeedBanDialog = needBanDialog;
    }

    public boolean isOTA() {
        return isOTA;
    }

    public void setOTA(boolean OTA) {
        isOTA = OTA;
    }

    private void handleLog(boolean isLog, boolean isSaveLog) {
        JL_Log.setTagPrefix("home");
        JL_Log.configureLog(getApplicationContext(), isLog, isSaveLog);
        com.jieli.jl_bt_ota.util.JL_Log.setLog(isLog);
        com.jieli.jl_bt_ota.util.JL_Log.setLogOutput(JL_Log::addLogOutput);
//        com.jieli.jl_bt_ota.util.JL_Log.setIsSaveLogFile(getApplicationContext(), isSaveLog);
    }

    public static void privacyPolicyAgreed() {
        DeviceIdentifier.register(MainApplication.getApplication());
        CrashReport.initCrashReport(MainApplication.getApplication(), "71c9922361", true);
        String tempOAID = DeviceIdentifier.getOAID(MainApplication.getApplication());
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(tempOAID.getBytes());
            OAID = ValueUtil.byte2HexStr(md5.digest());
            JL_Log.d("ZHM", "privacyPolicyAgreed: " + OAID);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
