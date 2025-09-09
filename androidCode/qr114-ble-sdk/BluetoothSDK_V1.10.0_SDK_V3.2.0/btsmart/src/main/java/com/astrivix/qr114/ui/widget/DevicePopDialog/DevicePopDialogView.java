package com.astrivix.qr114.ui.widget.DevicePopDialog;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.command.tws.NotifyAdvInfoCmd;
import com.jieli.bluetooth.bean.parameter.NotifyAdvInfoParam;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.ProductAction;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.astrivix.qr114.R;
import com.astrivix.qr114.constant.SConstant;
import com.astrivix.qr114.tool.product.DefaultResFactory;
import com.astrivix.qr114.tool.product.ProductCacheManager;
import com.astrivix.qr114.util.UIHelper;
import com.jieli.jl_http.bean.ProductMessage;
import com.jieli.jl_http.bean.ProductModel;


/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/4 4:04 PM
 * @desc : MODIFIED TO BE DISABLED. This view will no longer show a pop-up.
 */
public class DevicePopDialogView extends ConstraintLayout implements ProductCacheManager.OnUpdateListener {

    private final String tag = getClass().getSimpleName();
    private final RCSPController mRCSPController = RCSPController.getInstance();
    BleScanMessage bleScanMessage = null;
    BluetoothDevice device = null;

    public DevicePopDialogView(@NonNull Context context) {
        super(context);
        // We will not inflate any layout to keep this view disabled.
        // This constructor is intentionally left sparse.
    }


    // MODIFIED: This method is now empty to prevent crashes from findViewById().
    @SuppressLint("MissingPermission")
    private void initView() {
        // DO NOTHING
    }

    private void dismissWithIgnore() {
//        if (isActivated()) {
//            dismiss();
//            if(bleScanMessage != null) { // Add null check for safety
//                DevicePopDialogFilter.getInstance().addSeqIgnore(bleScanMessage);
//            }
//        }
    }

    void dismiss() {
        if (isActivated()) {
            // Immediately dismiss the view if it's ever added to the window.
//            try {
//                WindowManager wm = (WindowManager) getTag();
//                if (null != wm) {
//                    wm.removeViewImmediate(this);
//                }
//            } catch (Exception e) {
//                JL_Log.e(tag, "Error dismissing view: " + e.getMessage());
//            }
//            setTag(null);
//            setActivated(false);
        }
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // MODIFIED: Instead of initializing, we immediately dismiss.
        // This ensures the view never actually appears.
        dismiss();
    }

    @Override
    protected void onDetachedFromWindow() {
        // MODIFIED: Removed all listener un-registrations as they are never registered.
        setActivated(false);
        super.onDetachedFromWindow();
    }

    @Override
    public void onImageUrlUpdate(BleScanMessage message) {
        // DO NOTHING
    }

    @Override
    public void onJsonUpdate(BleScanMessage bleScanMessage, String path) {
        // DO NOTHING
    }

    // MODIFIED: This method is now empty.
    private void refreshView(BleScanMessage bleScanMessage) {
        // DO NOTHING
    }

    // MODIFIED: This method is now empty.
    private void refreshBottomView(BleScanMessage bleScanMessage) {
        // DO NOTHING
    }

    // MODIFIED: This method is now empty.
    @SuppressLint("MissingPermission")
    private void refreshContent(BleScanMessage bleScanMessage) {
        // DO NOTHING
    }

    // MODIFIED: This method is now empty.
    @SuppressLint("CheckResult")
    private void showByScene(BleScanMessage bleScanMessage, ImageView iv, String scene, int failedRes) {
        // DO NOTHING
    }

    // MODIFIED: This method is now empty.
    private void showQuantity(TextView tv, boolean isCharing, int quantity) {
        // DO NOTHING
    }

    // MODIFIED: This method is now empty.
    private String getTipText(BleScanMessage bleScanMessage) {
        return "";
    }

    // MODIFIED: This callback is now empty to prevent any logic from running.
    private final BTRcspEventCallback mBtEventCallback = new BTRcspEventCallback() {
        @Override
        public void onShowDialog(BluetoothDevice device, BleScanMessage message) {
            // DO NOTHING
        }

        @Override
        public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
            // DO NOTHING
        }
    };
}