package com.astrivix.qr114.ui.widget.DevicePopDialog;

import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.view.WindowManager;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.astrivix.qr114.constant.SConstant;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/16 11:47 AM
 * @desc : MODIFIED TO BE DISABLED. This task no longer performs any actions.
 */
class ShowTimeOutTask extends BTRcspEventCallback implements View.OnAttachStateChangeListener {

    public ShowTimeOutTask(DevicePopDialogView root) {
        // Constructor is kept for compatibility but does nothing.
    }

    @Override
    public void onViewAttachedToWindow(View v) {
        // DO NOTHING
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        // DO NOTHING
    }

    @Override
    public void onShowDialog(BluetoothDevice device, BleScanMessage bleScanMessage) {
        // DO NOTHING
    }
    
    private final Runnable task = new Runnable() {
        @Override
        public void run() {
            // DO NOTHING
        }
    };
}