package com.astrivix.qr114.ui.settings.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;

import com.astrivix.qr114.constant.SConstant;
import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.astrivix.qr114.R;
import com.astrivix.qr114.tool.product.ProductCacheManager;
import com.astrivix.qr114.util.NetworkStateHelper;
import com.astrivix.qr114.util.ProductUtil;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.jl_http.bean.ProductModel;

/**
 * 设备使用说明
 */
public class DeviceInstructionFragmentModify extends Jl_BaseFragment implements ProductCacheManager.OnUpdateListener, NetworkStateHelper.Listener {

    // The variable type is changed to WebView, but the name is kept as requested.
    WebView ivDeviceInstruction;
    private View mNoDataView;
    private View mNoNetworkView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_instruction, container, false);
        mNoDataView = view.findViewById(R.id.view_device_instruction_no_data);
        mNoNetworkView = view.findViewById(R.id.view_device_instruction_no_network);
        // The cast is now to WebView
        ivDeviceInstruction = view.findViewById(R.id.iv_device_instruction);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadImage(); // This method now loads the website
        ProductCacheManager.getInstance().registerListener(this);
        NetworkStateHelper.getInstance().registerListener(this);
    }

    @Override
    public void onDestroyView() {
        NetworkStateHelper.getInstance().unregisterListener(this);
        ProductCacheManager.getInstance().unregisterListener(this);
        super.onDestroyView();
    }

    // This method is no longer used by loadImage, but is kept to avoid breaking changes.
    private String getUrl() {
        DeviceInfo deviceInfo = RCSPController.getInstance().getDeviceInfo();
        if (deviceInfo == null) return "";
        String scene = ProductUtil.isChinese() ? ProductModel.MODEL_PRODUCT_INSTRUCTIONS_CN.getValue() : ProductModel.MODEL_PRODUCT_INSTRUCTIONS_EN.getValue();
        return ProductCacheManager.getInstance().getProductUrl(deviceInfo.getUid(), deviceInfo.getPid(), deviceInfo.getVid(), scene);
    }

    @Override
    public void onImageUrlUpdate(BleScanMessage bleScanMessage) {
        if (mNoDataView.getVisibility() == View.VISIBLE) {
            loadImage();
        }
    }

    @Override
    public void onJsonUpdate(BleScanMessage bleScanMessage, String path) {

    }

    @Override
    public void onNetworkStateChange(int type, boolean available) {
        // If network becomes available and the error view is showing, try reloading.
        if (available && mNoNetworkView.getVisibility() == View.VISIBLE) {
            loadImage();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void loadImage() {
        // The URL is now hardcoded as requested.

        final String url = SConstant.DEVICE_INSTRUCTION;
        JL_Log.d(TAG, "Loading website:" + url);

        // First, check for network availability.
        // **FIXED THE METHOD NAME HERE**
        if (!NetworkStateHelper.getInstance().isNetworkIsAvailable()) {
            mNoNetworkView.setVisibility(View.VISIBLE);
            ivDeviceInstruction.setVisibility(View.GONE);
            mNoDataView.setVisibility(View.GONE);
            return;
        }

        // If network is available, proceed to load the WebView.
        mNoDataView.setVisibility(View.GONE);
        mNoNetworkView.setVisibility(View.GONE);
        ivDeviceInstruction.setVisibility(View.VISIBLE);

        // Configure WebView settings
        ivDeviceInstruction.getSettings().setJavaScriptEnabled(true);
        ivDeviceInstruction.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                JL_Log.d(TAG, "onPageFinished: " + url);
                // Page has finished loading, make sure WebView is visible.
                ivDeviceInstruction.setVisibility(View.VISIBLE);
                mNoNetworkView.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                JL_Log.e(TAG, "onReceivedError: " + description);
                // Show the no-network view if there's an error loading the page.
                mNoNetworkView.setVisibility(View.VISIBLE);
                ivDeviceInstruction.setVisibility(View.GONE);
            }
        });

        // Load the specified URL.
        ivDeviceInstruction.loadUrl(url);
    }
}