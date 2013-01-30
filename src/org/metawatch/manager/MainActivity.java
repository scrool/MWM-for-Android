package org.metawatch.manager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator.AnimatorListener;
import com.actionbarsherlock.internal.nineoldandroids.animation.AnimatorSet;
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
	View widget_setup = findViewById(R.id.widget_setup);
	View status = findViewById(R.id.status);
	
	if (MetaWatchService.isRunning()) {
	    widget_setup.setVisibility(View.INVISIBLE);
	    status.setVisibility(View.INVISIBLE);
	    AnimatorSet set = new AnimatorSet();
	    set.play(fadeIn(widget_setup)).after(fadeIn(status));
	    set.addListener(new AnimatorListener() {
		@Override
		public void onAnimationStart(Animator animation) {
		    MainActivity.this.setProgressBarIndeterminateVisibility(Boolean.TRUE);
		}
		@Override
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
	    set.start();
	}
    }
    
    private ObjectAnimator fadeIn(final View view) {
	ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0, 1);
	fadeIn.setDuration(1250);
	fadeIn.setInterpolator(new DecelerateInterpolator());
	fadeIn.addListener(new AnimatorListener() {
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
	return fadeIn;
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
