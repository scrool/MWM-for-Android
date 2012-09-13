package org.metawatch.manager;

import org.metawatch.communityedition.R;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;

public class ThemeContainer extends TabActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        final Resources res = getResources();
        final TabHost tabHost = getTabHost();

        tabHost.addTab(tabHost.newTabSpec("tab1")
                .setIndicator(res.getString(R.string.ui_tab_downloaded_themes),res.getDrawable(R.drawable.ic_tab_downloaded))
                .setContent(new Intent(this, ThemePicker.class)));

        tabHost.addTab(tabHost.newTabSpec("tab2")
                .setIndicator(res.getString(R.string.ui_tab_theme_gallery),res.getDrawable(R.drawable.ic_tab_gallery))
                .setContent(new Intent(this, ThemeGallery.class)));
        
    }
}
