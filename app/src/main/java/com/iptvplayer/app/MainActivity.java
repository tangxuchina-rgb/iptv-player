package com.iptvplayer.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

/**
 * IPTV 网页播放器 —— Android WebView 壳。
 * 加载 app/src/main/assets/iptv-player.html，并提供：
 *  - HLS / m3u8 播放所需的媒体设置
 *  - 文件选择（导入本地 m3u 播放列表）
 *  - 全屏视频（横屏播放）
 *  - 返回键回退
 */
public class MainActivity extends Activity {

    private WebView webView;
    private FrameLayout rootLayout;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;

    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 1001;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.setBackgroundColor(0xFF000000);
        setContentView(rootLayout);

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.addView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);   // 允许自动播放
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // 允许通过 file:// 访问 assets 并联网（CORS）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            s.setAllowUniversalAccessFromFileURLs(true);
            s.setAllowFileAccessFromFileURLs(true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                // 外部链接（http/https 之外的）交给系统浏览器
                if (scheme != null && (scheme.startsWith("http") || scheme.equals("file") || scheme.startsWith("data"))) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) { }
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            // 全屏视频
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullscreenView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                fullscreenView = view;
                fullscreenCallback = callback;
                view.setBackgroundColor(0xFF000000);
                rootLayout.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                webView.setVisibility(View.GONE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                setFullscreen(true);
            }

            @Override
            public void onHideCustomView() {
                if (fullscreenView == null) return;
                rootLayout.removeView(fullscreenView);
                fullscreenView = null;
                if (fullscreenCallback != null) {
                    fullscreenCallback.onCustomViewHidden();
                    fullscreenCallback = null;
                }
                webView.setVisibility(View.VISIBLE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                setFullscreen(false);
            }

            // HTML <input type="file"> 支持
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;
                try {
                    Intent intent = fileChooserParams.createIntent();
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() { request.grant(request.getResources()); }
                });
            }
        });

        webView.loadUrl("file:///android_asset/iptv-player.html");
    }

    private void setFullscreen(boolean on) {
        if (on) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback != null) {
                Uri[] results = null;
                if (resultCode == Activity.RESULT_OK && data != null && data.getDataString() != null) {
                    results = new Uri[]{ Uri.parse(data.getDataString()) };
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (fullscreenView != null) {
            // 先从全屏退出
            if (fullscreenCallback != null) {
                // 触发 WebChromeClient 的隐藏流程
                webView.evaluateJavascript(
                        "(function(){var v=document.querySelector('video');if(v&&v.webkitExitFullscreen)v.webkitExitFullscreen();})();",
                        null);
            }
            return;
        }
        webView.evaluateJavascript(
                "(function(){try{return (window.__iptvBack&&window.__iptvBack())?'1':'0';}catch(e){return '0';}})();",
                value -> {
                    if (!"\"1\"".equals(value) && !"1".equals(value)) {
                        if (webView.canGoBack()) {
                            webView.goBack();
                        } else {
                            finish();
                        }
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
