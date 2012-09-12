package org.metawatch.manager;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ThemeGallery extends Activity {
	WebView mWebView;
	Activity activity; 
	
	private class Client extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url.contains(".zip")) {
				BitmapCache.downloadAndInstallTheme(activity, url);
				return true;
			}
			else if (Uri.parse(url).getHost().equals("grapefruitopia.com")) {
				return false;
			}
			else {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(intent);
				return true;
			}
		}
	}
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        activity = this;
        mWebView = new WebView(this);
        mWebView.setBackgroundColor(Color.BLACK);
        mWebView.loadUrl("http://grapefruitopia.com/mwthm/");
        mWebView.setWebViewClient(new Client());
        
        setContentView(mWebView);
    }
}
