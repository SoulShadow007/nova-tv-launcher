package com.nova.tv.launcher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Единственная активность: полноэкранный WebView с интерфейсом лаунчера
 * (assets/index.html). Спроектирована под управление ТВ-пультом:
 *
 *  - D-pad WebView транслирует в события стрелок (ArrowLeft/Right/Up/Down);
 *  - OK центрируется как Enter / клик по сфокусированному элементу;
 *  - «Назад» перехватывается и уходит в JS-шим window.__novaBack()
 *    (навигация назад по слоям интерфейса, двойное нажатие — выход);
 *  - кнопка MENU пульта открывает настройки лаунчера.
 */
public class MainActivity extends Activity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);              // загрузка file:///android_asset/
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.setWebViewClient(new WebViewClient());
        webView.setBackgroundColor(0xFF000000);
        setContentView(webView);

        // Не гасить экран, пока лаунчер открыт
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        webView.loadUrl("file:///android_asset/index.html");
        hideSystemUI();
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            webView.setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                            | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    @Override
    public void onBackPressed() {
        // Лаунчер сам решает: уйти на уровень назад или (двойное нажатие) закрыть приложение
        webView.evaluateJavascript(
                "window.__novaBack ? window.__novaBack() : 'exit'",
                value -> {
                    if (value == null || value.contains("exit")) {
                        finish();
                    }
                });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Кнопка MENU на пульте — открыть настройки лаунчера (клавиша «s» в прототипе)
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            webView.evaluateJavascript(
                    "document.dispatchEvent(new KeyboardEvent('keydown',{key:'s'}))", null);
            return true;
        }
        return super.onKeyDown(keyCode, event);
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
