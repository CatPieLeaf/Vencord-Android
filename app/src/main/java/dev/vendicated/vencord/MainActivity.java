package dev.vendicated.vencord;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Objects;

public class MainActivity extends Activity {
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 123;
    public static final int FILECHOOSER_RESULTCODE = 8485;
    private boolean wvInitialized = false;
    private WebView wv;
    public ValueCallback<Uri[]> filePathCallback;

    @SuppressLint("SetJavaScriptEnabled") // mad? watch this swag
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        wv = findViewById(R.id.webview);

        // Set user agent to simulate a desktop browser
        WebSettings settings = wv.getSettings();
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        // Enable necessary features
        settings.setJavaScriptEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false); // Allows autoplay
        settings.setUseWideViewPort(true);

        // Set WebChromeClient with onPermissionRequest
        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("WebView Console", consoleMessage.message());
                return super.onConsoleMessage(consoleMessage);
            }
        });

        explodeAndroid();

        wv.setWebViewClient(new VWebviewClient());
        wv.setWebChromeClient(new VChromeClient(this));

        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);

        try {
            HttpClient.fetchVencord(this);
        } catch (IOException ex) {
            Logger.e("Failed to fetch Vencord", ex);
            return;
        }

        Intent intent = getIntent();
        if (Objects.equals(intent.getAction(), Intent.ACTION_VIEW)) {
            Uri data = intent.getData();
            if (data != null) handleUrl(intent.getData());
        } else {
            wv.loadUrl("https://discord.com/app");
        }
        wvInitialized = true;
        setupWebView();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && wv != null) {
            runOnUiThread(() -> wv.evaluateJavascript("VencordMobile.onBackPress()", r -> {
                if ("false".equals(r))
                    this.onBackPressed ();
            }));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode != FILECHOOSER_RESULTCODE || filePathCallback == null)
            return;

        if (resultCode != RESULT_OK || intent == null) {
            filePathCallback.onReceiveValue(null);
        } else {
            Uri[] uris;
            try {
                var clipData = intent.getClipData();
                if (clipData != null) { // multiple items
                    uris = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        uris[i] = clipData.getItemAt(i).getUri();
                    }
                } else { // single item
                    uris = new Uri[]{intent.getData()};
                }
            } catch (Exception ex) {
                Logger.e("Error during file upload", ex);
                uris = null;
            }

            filePathCallback.onReceiveValue(uris);
        }
        filePathCallback = null;
    }

    private void explodeAndroid() {
        StrictMode.setThreadPolicy(
                new StrictMode.ThreadPolicy.Builder()
                        // trolley
                        .permitNetwork()
                        .build()
        );
    }
    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void setupWebView() {
        // Your existing WebView setup code here

        // Check for microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            // Permission already granted, proceed with WebView setup
            configureWebView();
        }
        wv.addJavascriptInterface(new VencordNative(this, wv) {
            @Override
            public void yourMethod1() {

            }

            @Override
            public void yourMethod2() {

            }
        }, "VencordMobileNative");
    }
    private void configureWebView() {
        // WebView configuration code

        // Set WebChromeClient with onPermissionRequest
        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }

            // Other WebChromeClient methods
        });

        // Your other WebView setup code
    }
    public void handleUrl(Uri url) {
        if (url != null) {
            if (!Objects.equals(url.getAuthority(), "discord.com")) return;
            if (!wvInitialized) {
                wv.loadUrl(url.toString());
            } else {
                wv.evaluateJavascript("Vencord.Webpack.Common.NavigationRouter.transitionTo(\"" + url.getPath() + "\")", null);
            }
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri data = intent.getData();
        if (data != null) handleUrl(data);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with WebView setup
                configureWebView();
            }
        }
    }
}
