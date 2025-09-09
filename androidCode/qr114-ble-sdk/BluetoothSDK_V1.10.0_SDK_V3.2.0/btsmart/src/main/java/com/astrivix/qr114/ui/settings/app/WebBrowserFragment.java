package com.astrivix.qr114.ui.settings.app;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jieli.bluetooth.utils.JL_Log;
import com.astrivix.qr114.R;
import com.astrivix.qr114.constant.SConstant;
import com.astrivix.qr114.tool.network.NetworkDetectionHelper;
import com.astrivix.qr114.ui.CommonActivity;
import com.jieli.component.base.Jl_BaseFragment;

/**
 * Des:Sasanda saumya
 * Author: Bob
 * Date:20-5-18
 * UpdateRemark: Fully updated with robust WebView configuration and error handling.
 */
public final class WebBrowserFragment extends Jl_BaseFragment {
    private WebView mWebView;
    private ProgressBar mProgressBar;
    private RelativeLayout mRootLayout;
    private View mErrorView;

    public static WebBrowserFragment newInstance() {
        return new WebBrowserFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // <<< CHANGE #1: Inflate a layout that contains a WebView and a ProgressBar >>>
        // This is cleaner than creating views programmatically.
        View view = inflater.inflate(R.layout.fragment_web_browser, container, false);
        mRootLayout = (RelativeLayout) view;
        mWebView = view.findViewById(R.id.web_view);
        mProgressBar = view.findViewById(R.id.progress_bar);

        NetworkDetectionHelper.getInstance().addOnNetworkDetectionListener(mOnNetworkDetectionListener);
        return view;
    }

    @Override
    public void onDestroyView() {
        NetworkDetectionHelper.getInstance().removeOnNetworkDetectionListener(mOnNetworkDetectionListener);
        super.onDestroyView();
    }

    @Override
    public void onViewCreated(@NonNull View view, @NonNull Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = getArguments();
        int flag = 0;
        if (bundle != null) {
            flag = bundle.getInt(SConstant.KEY_WEB_FLAG, 0);
        }

        final String url = flag == 0 ? SConstant.URL_USER_AGREEMENT : SConstant.URL_PRIVACY_POLICY;
        String title = flag == 0 ? getString(R.string.user_agreement) : getString(R.string.privacy_policy);

        // <<< CHANGE #2: Configure WebView Settings >>>
        setupWebView();

        // Load the URL and set up the client for handling events
        JL_Log.d(TAG, "Loading URL: " + url);
        mWebView.loadUrl(url);

        // Update the activity's top bar
        CommonActivity activity = (CommonActivity) getActivity();
        if (activity != null) {
            activity.updateTopBar(title, R.drawable.ic_back_black, v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }, 0, null);
        }
    }

    private void setupWebView() {
        // Get WebView settings
        WebSettings webSettings = mWebView.getSettings();

        // <<< CHANGE #3: Enable JavaScript (CRITICAL) >>>
        webSettings.setJavaScriptEnabled(true);

        // Improve performance and rendering
        webSettings.setDomStorageEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // <<< CHANGE #4: Set a robust WebViewClient for better error handling >>>
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mProgressBar.setVisibility(View.VISIBLE);
                hideErrorView(); // Hide error view when a new page starts loading
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mProgressBar.setVisibility(View.GONE);
            }

            // This is the modern way to catch errors
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                // Only show error if it's for the main page
                if (request.isForMainFrame()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        JL_Log.e(TAG, "WebView Error: " + error.getErrorCode() + " - " + error.getDescription());
                    }
                    showErrorView("Page could not be loaded.");
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Handle SSL errors (e.g., bad certificate). For production, you should
                // typically not proceed, but for debugging, you can log it.
                JL_Log.e(TAG, "SSL Error: " + error.toString());
                handler.cancel(); // Do not proceed with an invalid certificate
                showErrorView("Security certificate error.");
            }
        });
    }

    private void showErrorView(String message) {
        mWebView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);

        // Inflate and show the error layout only if it's not already visible
        if (mErrorView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            mErrorView = inflater.inflate(R.layout.view_no_network, mRootLayout, false);
            TextView errorText = mErrorView.findViewById(R.id.tv_no_network_tips); // Assuming this ID exists
            if (errorText != null) {
                errorText.setText(message);
            }
            mRootLayout.addView(mErrorView);
        }
        mErrorView.setVisibility(View.VISIBLE);
    }

    private void hideErrorView() {
        if(mErrorView != null) {
            mErrorView.setVisibility(View.GONE);
        }
        mWebView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        if (mWebView != null) {
            mWebView.loadDataWithBaseURL(null, "", "text/html",
                    "utf-8", null);
            mWebView.setTag(null);
            mWebView.clearHistory();

            if (mWebView.getParent() != null) {
                ((ViewGroup) mWebView.getParent()).removeView(mWebView);
            }
            mWebView.destroy();
            mWebView = null;
        }
        super.onDestroy();
    }

    private final NetworkDetectionHelper.OnNetworkDetectionListener mOnNetworkDetectionListener = (type, available) -> {
        JL_Log.w(TAG, "onNetworkStateChange --->" + available + "\tattach=" + (mWebView != null && mWebView.isAttachedToWindow()));
        if (available && isAdded() && mWebView != null) {
            // If the error view is showing, try reloading the page
            if (mErrorView != null && mErrorView.getVisibility() == View.VISIBLE) {
                hideErrorView();
                mWebView.reload();
            }
        }
    };
}