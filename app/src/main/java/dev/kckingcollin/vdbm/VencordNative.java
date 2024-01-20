package dev.kckingcollin.vdbm;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

public abstract class VencordNative {

    public VencordNative(Activity activity, WebView wv) {
    }

    public abstract void yourMethod1();

    public abstract void yourMethod2();
}
