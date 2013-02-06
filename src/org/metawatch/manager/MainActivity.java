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
    }

    private ObjectAnimator downFromTop(final View view) {
	ObjectAnimator downFromTop = ObjectAnimator.ofFloat(view, "translationY", -1000, 0);
	downFromTop.setDuration(1250);
	downFromTop.setStartDelay(500);
	downFromTop.setInterpolator(new DecelerateInterpolator());
	downFromTop.addListener(new AnimatorListener() {
	    @Override
	    public void onAnimationStart(Animator animation) {
		view.setVisibility(View.VISIBLE);
	    }
	    public void onAnimationEnd(Animator animation) {
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
    
    private ObjectAnimator inFromLeft(final View view) {
	ObjectAnimator inFromLeft = ObjectAnimator.ofFloat(view, "translationX", -1000, 0);
	inFromLeft.setDuration(1000);
	inFromLeft.setStartDelay(500);
	inFromLeft.setInterpolator(new DecelerateInterpolator());
	inFromLeft.addListener(new AnimatorListener() {
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
	return inFromLeft;
    }
    
    @Override
    public void onResume() {
	super.onResume();
	View status = findViewById(R.id.status);
	status.setVisibility(View.INVISIBLE);
	downFromTop(status).start();
	View widgets = findViewById(R.id.widget_setup);
	widgets.setVisibility(View.INVISIBLE);
	inFromLeft(widgets).start();

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
