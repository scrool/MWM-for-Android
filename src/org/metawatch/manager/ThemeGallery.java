package org.metawatch.manager;

import org.metawatch.communityedition.R;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class ThemeGallery extends Activity {
	WebView mWebView;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mWebView = new WebView(this);
        mWebView.loadUrl("http://grapefruitopia.com/mwthm/");

        setContentView(mWebView);
    }
}
