package com.astrivix.qr114.ui.launcher;

import android.content.Context;

import androidx.annotation.NonNull;

import com.astrivix.qr114.tool.configure.ConfigureKit;
import com.astrivix.qr114.util.ProductUtil;
import com.astrivix.qr114.viewmodel.base.BaseViewModel;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 运行逻辑实现
 * @since 2023/11/6
 */
public class LauncherVM extends BaseViewModel {
    private final ConfigureKit mConfigureKit = ConfigureKit.getInstance();

    public boolean isAgreeUserAgreement() {
        return mConfigureKit.isAgreeAgreement();
    }

    public void setAgreeUserAgreement(boolean isAgree) {
        mConfigureKit.setAgreeAgreement(isAgree);
    }

    public boolean isAllowRequestFloatingPermission(@NonNull Context context) {
        return !ProductUtil.isCanDrawOverlays(context) && !mConfigureKit.isBanRequestFloatingWindowPermission(context);
    }

    public void setBanRequestFloatingWindowPermission(@NonNull Context context, boolean isBan) {
        ConfigureKit.getInstance().setBanRequestFloatingWindowPermission(context, isBan);
        ProductUtil.setIsAllowFloatingWindow(!isBan);
    }
}
