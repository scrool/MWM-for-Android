package org.metawatch.manager;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.SherlockFragment;

public class ThemeGallery extends SherlockFragment {
	WebView mWebView;
	ThemeContainer activity; 
	
	public static ThemeGallery newInstance() {
		return new ThemeGallery();
	}
	
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
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (ThemeContainer) getActivity();
        activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);
        mWebView = new WebView(activity);
        mWebView.setBackgroundColor(Color.BLACK);
        mWebView.loadUrl("http://grapefruitopia.com/mwthm/");
        mWebView.setWebViewClient(new Client() {
        	@Override
        	public void onPageFinished(WebView view, String url) {
        		activity.setProgressBarIndeterminateVisibility(Boolean.FALSE);
        	}
        });
        return mWebView;
    }
}