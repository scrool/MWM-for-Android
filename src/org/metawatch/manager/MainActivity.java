package org.metawatch.manager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator.AnimatorListener;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

public class MainActivity extends SherlockFragmentActivity {
    @Override
    public void onCreate(Bundle bundle) {
	super.onCreate(bundle);
	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	setContentView(R.layout.main_activity);
	Intent intent = getIntent();
	if (intent != null && intent.getBooleanExtra("shutdown", false) && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0)
	    finish();
	View status = findViewById(R.id.status);
	if (MetaWatchService.isRunning()) {
	    status.setVisibility(View.INVISIBLE);
	    downFromTop(status).start();
	}
    }
    
    private ObjectAnimator downFromTop(final View view) {
	ObjectAnimator downFromTop = ObjectAnimator.ofFloat(view, "translationY", -1000, 0);
	downFromTop.setDuration(1250);
	downFromTop.setInterpolator(new DecelerateInterpolator());
	downFromTop.addListener(new AnimatorListener() {
	    @Override
	    public void onAnimationStart(Animator animation) {
		view.setVisibility(View.VISIBLE);
		MainActivity.this.setProgressBarIndeterminateVisibility(Boolean.TRUE);
	    }
	    public void onAnimationEnd(Animator animation) {
		setProgressBarIndeterminateVisibility(Boolean.FALSE);
	    }
	    @Override
	    public void onAnimationCancel(Animator animation) {
	    }
	    @Override
	    public void onAnimationRepeat(Animator animation) {
	    }
	});
	return downFromTop;
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
