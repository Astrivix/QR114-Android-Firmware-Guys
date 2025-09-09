package com.astrivix.qr114.ui.launcher;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.amap.api.maps.MapsInitializer;
import com.jieli.bluetooth.utils.JL_Log;
import com.astrivix.qr114.BuildConfig;
import com.astrivix.qr114.MainApplication;
import com.astrivix.qr114.R;
import com.astrivix.qr114.constant.SConstant;
import com.astrivix.qr114.databinding.ActivityLauncherBinding;
import com.astrivix.qr114.ui.CommonActivity;
import com.astrivix.qr114.ui.home.HomeActivity;
import com.astrivix.qr114.ui.home.LoginActivity;
import com.astrivix.qr114.ui.settings.app.WebBrowserFragment;
import com.astrivix.qr114.ui.widget.UserServiceDialog;
import com.astrivix.qr114.util.JL_MediaPlayerServiceManager;
import com.astrivix.qr114.util.PermissionUtil;
import com.jieli.component.ActivityManager;
import com.jieli.component.base.Jl_BaseActivity;
import com.jieli.component.network.WifiHelper;
import com.jieli.component.utils.SystemUtil;
import com.jieli.jl_dialog.Jl_Dialog;
import com.astrivix.qr114.util.SessionManager; // <-- Import SessionManager

/**
 * 启动页界面
 */
public class LauncherActivity extends Jl_BaseActivity {
    private LauncherVM mViewModel;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SessionManager sessionManager; // <-- Add SessionManager instance

    private final ActivityResultLauncher<Intent> openGpsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> toNextActivity()); // Renamed to a more generic name

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemUtil.setImmersiveStateBar(getWindow(), true);
        ActivityLauncherBinding binding = ActivityLauncherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mViewModel = new ViewModelProvider(this).get(LauncherVM.class);
        sessionManager = new SessionManager(this); // <-- Initialize SessionManager

        JL_Log.i(TAG, "APP version : " + SystemUtil.getVersionName(getApplicationContext()) + ", code = " + SystemUtil.getVersion(getApplicationContext()));
        if (BuildConfig.OPEN_LAUNCHER_ANIM) {
            Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale);
            scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    updateView();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            binding.rlLauncherLayout.startAnimation(scaleAnimation);
            return;
        }
        updateView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }

    private void updateView() {
        if (!mViewModel.isAgreeUserAgreement()) {
            MapsInitializer.updatePrivacyShow(getApplicationContext(), true, true);
            showUserServiceTipsDialog();
            return;
        }

        MainApplication.privacyPolicyAgreed();
        MapsInitializer.updatePrivacyShow(getApplicationContext(), true, true);
        MapsInitializer.updatePrivacyAgree(getApplicationContext(), true);
        if (PermissionUtil.isHasStoragePermission(this)) {
            JL_MediaPlayerServiceManager.getInstance().bindService();
        }
        MainApplication.getApplication().uploadAppInfo();

        if (mViewModel.isAllowRequestFloatingPermission(getApplicationContext())) {
            showRequestFloatingDialog(getString(R.string.request_floating_window_permission_tips));
        } else {
            toNextActivity(); // Renamed to a more generic name
        }
    }

    // --- (No changes to showUserServiceTipsDialog, showRequestFloatingDialog, or toWebFragment) ---
    // ... your existing methods here ...
    private void showUserServiceTipsDialog() {
        final String flag = UserServiceDialog.class.getName();
        UserServiceDialog dialog = (UserServiceDialog) getSupportFragmentManager().findFragmentByTag(flag);
        if (null == dialog) {
            dialog = new UserServiceDialog();
            dialog.setOnUserServiceListener(new UserServiceDialog.OnUserServiceListener() {
                @Override
                public void onUserService() {
                    toWebFragment(0);
                }

                @Override
                public void onPrivacyPolicy() {
                    toWebFragment(1);
                }

                @Override
                public void onExit(DialogFragment dialogFragment) {
                    mViewModel.setAgreeUserAgreement(false);
                    MapsInitializer.updatePrivacyAgree(getApplicationContext(), false);
                    dialogFragment.dismiss();
                    ActivityManager.getInstance().popAllActivity();
                    finish();
                }

                @Override
                public void onAgree(DialogFragment dialogFragment) {
                    JL_Log.i(TAG, " ==================== onAgree ==================");
                    dialogFragment.dismiss();
                    mViewModel.setAgreeUserAgreement(true);
                    updateView();
                }
            });
        }
        if (!dialog.isShow()) dialog.show(getSupportFragmentManager(), flag);
    }

    private void showRequestFloatingDialog(String text) {
        if (isFinishing() || isDestroyed()) return;
        final String tag = "request_floating_permission";
        Jl_Dialog dialog = (Jl_Dialog) getSupportFragmentManager().findFragmentByTag(tag);
        if (dialog == null) {
            dialog = Jl_Dialog.builder()
                    .title(getString(R.string.tips))
                    .content(text)
                    .titleColor(getResources().getColor(R.color.white))
                    .contentColor(getResources().getColor(R.color.white))
                    .rightColor(getResources().getColor(R.color.white))
                    .showProgressBar(false)
                    .width(0.8f)
                    .left(getString(R.string.allow))
                    .leftClickListener((v, dialogFragment) -> {
                        mViewModel.setBanRequestFloatingWindowPermission(getApplicationContext(), false);
                        dialogFragment.dismiss();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            openGpsLauncher.launch(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
                        } else {
                            toNextActivity();
                        }
                    })
                    .right(getString(R.string.cancel))
                    .rightClickListener((v, dialogFragment) -> {
                        mViewModel.setBanRequestFloatingWindowPermission(getApplicationContext(), true);
                        dialogFragment.dismiss();
                        toNextActivity();
                    })
                    .cancel(false)
                    .build();
        }
        if (!dialog.isShow()) {
            dialog.show(getSupportFragmentManager(), tag);
        }
    }

    private void toWebFragment(int flag) {
        Bundle bundle = new Bundle();
        bundle.putInt(SConstant.KEY_WEB_FLAG, flag);
        CommonActivity.startCommonActivity(LauncherActivity.this,
                WebBrowserFragment.class.getCanonicalName(), bundle);
    }

    /**
     * This is the final step. It checks the login state and navigates
     * to the correct activity.
     */
    private void toNextActivity() {

        //check lanuage alredy select or not
//        if(true){
//            mHandler.postDelayed(() -> {
//                startActivity(new Intent(this, LanguageSelectActivity.class));
//                mHandler.postDelayed(LauncherActivity.this::finish, 500);
//            }, 1500);
//        }

        SessionManager sessionManager = new SessionManager(this);
        // Check if the language has been selected before.
        if (sessionManager.isFirstRunComplete()) {
            // Language is already set, proceed to normal flow.
            // Check if user is logged in.
            if (sessionManager.isLoggedIn()) {
//                intent = new Intent(this, HomeActivity.class);
                mHandler.postDelayed(() -> {
                startActivity(new Intent(this, HomeActivity.class));
                mHandler.postDelayed(LauncherActivity.this::finish, 500);
            }, 1500);
            } else {
                mHandler.postDelayed(() -> {
                    startActivity(new Intent(this, LoginActivity.class));
                    mHandler.postDelayed(LauncherActivity.this::finish, 500);
                }, 1500);            }
        } else {
            // This is the first launch, show the language selection screen.
            mHandler.postDelayed(() -> {
                startActivity(new Intent(this, LanguageSelectActivity.class));
                mHandler.postDelayed(LauncherActivity.this::finish, 500);
            }, 1500);        }

        // Finish SplashActivity so the user cannot navigate back to it.

        // Use the SessionManager to check if the user is signed in
//        if (sessionManager.isLoggedIn()) {
//            // User is signed in, go to Home
//            JL_Log.d(TAG, "User is logged in. Navigating to HomeActivity.");
//            WifiHelper.getInstance(this).registerBroadCastReceiver(this);
//            mHandler.postDelayed(() -> {
//                startActivity(new Intent(this, HomeActivity.class));
//                mHandler.postDelayed(LauncherActivity.this::finish, 500);
//            }, 1500);
//        } else {
//            // User is NOT signed in, go to Login
//            JL_Log.d(TAG, "User is not logged in. Navigating to LoginActivity.");
//            WifiHelper.getInstance(this).registerBroadCastReceiver(this);
//            mHandler.postDelayed(() -> {
//                startActivity(new Intent(this, LoginActivity.class)); // Go to LoginActivity
//                mHandler.postDelayed(LauncherActivity.this::finish, 500);
//            }, 1500);
//        }
    }
}