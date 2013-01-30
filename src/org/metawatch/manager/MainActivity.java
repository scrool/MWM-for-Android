package org.metawatch.manager;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

public class MainActivity extends SherlockFragmentActivity {

    ActionBar mActionBar;

    @Override
    public void onCreate(Bundle bundle) {
	super.onCreate(bundle);
	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	setContentView(R.layout.main_activity);
	Intent intent = getIntent();
	if (intent != null && intent.getBooleanExtra("shutdown", false) && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0)
	    finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	super.onCreateOptionsMenu(menu);
	com.actionbarsherlock.view.MenuInflater inflater = getSupportMenuInflater();
	inflater.inflate(R.menu.main, menu);
	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	Intent intent;
	switch (item.getItemId()) {
	case R.id.settings:
	    intent = new Intent(this, Settings.class);
	    startActivity(intent);
	    return true;
	case R.id.tests:
	    intent = new Intent(this, Test.class);
	    startActivity(intent);
	    return true;
	default:
	    return false;
	}
    }
}
