package com.astrivix.qr114.ui.widget.DevicePopDialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.command.tws.NotifyAdvInfoCmd;
import com.jieli.bluetooth.bean.parameter.NotifyAdvInfoParam;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.ProductAction;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.astrivix.qr114.R;
import com.astrivix.qr114.constant.SConstant;
import com.astrivix.qr114.tool.bluetooth.rcsp.DeviceOpHandler;
import com.astrivix.qr114.util.AppUtil;
import com.astrivix.qr114.util.PermissionUtil;
import com.astrivix.qr114.util.UIHelper;
import com.jieli.component.ActivityManager;
import com.jieli.component.utils.SystemUtil;

import java.util.List;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/4 3:44 PM
 * @desc : MODIFIED TO BE DISABLED. This service no longer performs any actions.
 */
public class DevicePopDialog extends Service {
    private final String tag = getClass().getSimpleName();
    private final RCSPController mRCSPController = RCSPController.getInstance();
    // MODIFIED: We no longer need a reference to the view.
    // private DevicePopDialogView root;

    @Override
    public void onCreate() {
        super.onCreate();
        // MODIFIED: All logic from onCreate is removed to prevent the service
        // from doing anything, including creating views or listening to events.
        JL_Log.w(tag, "DevicePopDialog service is DISABLED.");
        
        // We still need to create a notification to avoid being killed on newer Android,
        // but we will stop the service immediately.
        createNotification();
        stopSelf(); // Stop the service right after it's created.
    }

    @Override
    public void onDestroy() {
        // MODIFIED: Removed all listener un-registrations.
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    public boolean isShowing() {
        // MODIFIED: Always return false as nothing will be shown.
        return false;
    }

    @SuppressLint("WrongConstant")
    private void createNotification() {
        String CHANNEL_ONE_ID = getPackageName();
        String CHANNEL_ONE_NAME = "Channel_Two";
        NotificationChannel notificationChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_LOW);
            notificationChannel.enableLights(false);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(false);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(notificationChannel);
            }
        }
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        Class<?> clazz = AppUtil.getConnectActivityClass();
        Intent nfIntent = new Intent(getApplicationContext(), clazz);
        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_MUTABLE;
        }
        builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, nfIntent, flags))
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_btsmart_logo)
                .setContentText(getString(R.string.app_is_running))
                .setWhen(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ONE_ID);
        }
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(135, notification);
    }

    // MODIFIED: Method body is empty.
    private void dismissDialog(BleScanMessage bleScanMessage) {
        // DO NOTHING
    }

    // MODIFIED: Method body is empty.
    private void showDialog(BluetoothDevice device, BleScanMessage bleScanMessage) {
        // DO NOTHING
    }

    // MODIFIED: Method body returns true to ignore all pop-ups.
    private boolean shouldIgnore(BleScanMessage bleScanMessage) {
        return true;
    }
    
    // MODIFIED: Method body is empty.
    private boolean isConnectedDevice(String addr) {
        return false;
    }

    // MODIFIED: Callback is empty.
    private final BTRcspEventCallback mBtEventCallback = new BTRcspEventCallback() {
        // All methods are left empty intentionally.
    };
}