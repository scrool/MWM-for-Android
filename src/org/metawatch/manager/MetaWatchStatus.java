/*****************************************************************************
 *  Copyright (c) 2011 Meta Watch Ltd.                                       *
 *  www.MetaWatch.org                                                        *
 *                                                                           *
 =============================================================================
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the "License");          *
 *  you may not use this file except in compliance with the License.         *
 *  You may obtain a copy of the License at                                  *
 *                                                                           *
 *    http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an "AS IS" BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *****************************************************************************/

/*****************************************************************************
 * MetaWatch.java                                                            *
 * MetaWatch                                                                 *
 * status activity                                                           *
 *                                                                           *
 *                                                                           *
 *****************************************************************************/

package org.metawatch.manager;

import java.io.IOException;
import java.io.InputStream;

import org.metawatch.manager.MetaWatchService.GeolocationMode;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WeatherProvider;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Messenger;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.nineoldandroids.animation.AnimatorSet;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.bugsense.trace.BugSenseHandler;

public class MetaWatchStatus extends SherlockFragment implements OnClickListener {

    public static final String TAG = "MetaWatchStatus";
    public Button mStatistics = null;
    public Button startButton = null;
    public Button shutdownButton = null;
    private SherlockFragmentActivity context = null;
    public Messenger mService = null;
    public long startupTime = 0;
    private TextView mStatisticsText = null;
    private TextView mAccessibilityText = null;
    private AlertDialog mStatisticsDialog = null;
    private ActionBar mActionBar = null;
    private View mMainView = null;

    public static MetaWatchStatus newInstance() {
	return new MetaWatchStatus();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	context = (SherlockFragmentActivity) getActivity();
	mActionBar = context.getSupportActionBar();
	LinearLayout statisticsContainer = new LinearLayout(context);
	statisticsContainer.setGravity(Gravity.CENTER);
	statisticsContainer.setOrientation(LinearLayout.VERTICAL);
	mStatisticsText = new TextView(context);
	mAccessibilityText = new TextView(context);
	mAccessibilityText.setGravity(Gravity.CENTER);
	statisticsContainer.addView(mAccessibilityText);
	statisticsContainer.addView(mStatisticsText);
	Builder builder = new Builder(context);
	builder.setIcon(R.drawable.icon);
	builder.setTitle(R.string.statistics);
	builder.setView(statisticsContainer);
	builder.setPositiveButton(android.R.string.ok, null);
	mStatisticsDialog = builder.create();
	configureBugSense();
	MetaWatchService.loadPreferences(context);

	startupTime = System.currentTimeMillis();

	if (Preferences.watchMacAddress == "") {
	    // Show the watch discovery screen on first start
	    startActivity(new Intent(context, DeviceSelection.class));
	    context.finish();
	}
	Protocol.getInstance(context).configureMode();
    }

    private void configureBugSense() {
	try {
	    InputStream inputStream = context.getAssets().open("bugsense.txt");
	    String key = Utils.ReadInputStream(inputStream);
	    key = key.trim();
	    if (Preferences.logging)
		Log.d(TAG, "BugSense enabled");
	    BugSenseHandler.initAndStartSession(context, key);
	} catch (IOException e) {
	    if (Preferences.logging)
		Log.d(TAG, "No BugSense keyfile found");
	}
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	mMainView = inflater.inflate(R.layout.main, null);
	mStatistics = (Button) mMainView.findViewById(R.id.statistics);
	mStatistics.setOnClickListener(new OnClickListener() {
	    @Override
	    public void onClick(View v) {
		mStatisticsDialog.show();
	    }
	});
	startButton = (Button) mMainView.findViewById(R.id.startButton);
	startButton.setOnClickListener(this);
	shutdownButton = (Button) mMainView.findViewById(R.id.shutdownButton);
	shutdownButton.setOnClickListener(this);
	return mMainView;
    }
    
    @Override
    public void onClick(View view) {
	wiggleButton(view);
	switch(view.getId()) {
	case R.id.startButton:
	    startService();
	    break;
	case R.id.shutdownButton:
	    stopService();
	    break;
	}
    }
    
    private void wiggleButton(View view) {
	ObjectAnimator right = ObjectAnimator.ofFloat(view, "translationX", 0, 5);
	right.setDuration(45);
	ObjectAnimator left = ObjectAnimator.ofFloat(view, "translationX", 5, -5);
	left.setDuration(45);
	ObjectAnimator centerHorizontal = ObjectAnimator.ofFloat(view, "translationX", -5, 0);
	centerHorizontal.setDuration(45);
	ObjectAnimator up = ObjectAnimator.ofFloat(view, "translationY", 0, -5);
	up.setDuration(45);
	ObjectAnimator down = ObjectAnimator.ofFloat(view, "translationY", -5, 5);
	down.setDuration(45);
	ObjectAnimator centerVertical = ObjectAnimator.ofFloat(view, "translationY", 5, 0);
	centerVertical.setDuration(45);
	AnimatorSet set = new AnimatorSet();
	set.playSequentially(right, left, centerHorizontal, up, down, centerVertical);
	set.start();
    }

    @Override
    public void onResume() {
	super.onResume();
	mHandler.post(mConnectionUpdater);
	if (MetaWatchService.isRunning()) {
	    startService();
	}
    }

    @Override
    public void onPause() {
	super.onPause();
	mHandler.removeCallbacks(mConnectionUpdater);
    }
    
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mConnectionUpdater = new Runnable() {
 	@Override
 	public void run() {
 	   displayStatus();
 	    mHandler.postDelayed(mConnectionUpdater, 1000);
 	}
     };

    private void displayStatus() {
	getStatistics();
	if (mActionBar == null)
	    return;
	switch (MetaWatchService.connectionState) {
	case MetaWatchService.ConnectionState.DISCONNECTED:
	    mActionBar.setTitle(context.getString(R.string.app_name) + ": " + context.getString(R.string.connection_disconnected));
	    break;
	case MetaWatchService.ConnectionState.CONNECTING:
	    mActionBar.setTitle(context.getString(R.string.app_name) + ": " + context.getString(R.string.connection_connecting));
	    break;
	case MetaWatchService.ConnectionState.CONNECTED:
	    mActionBar.setTitle(context.getString(R.string.app_name) + ": " + context.getString(R.string.connection_connected));
	    break;
	case MetaWatchService.ConnectionState.DISCONNECTING:
	    mActionBar.setTitle(context.getString(R.string.app_name) + ": " + context.getString(R.string.connection_disconnecting));
	    break;
	}
    }

    private void getStatistics() {
	mStatisticsText.setGravity(Gravity.CENTER);
	final Resources res = context.getResources();
	mStatisticsText.setText("");
	if (Preferences.weatherProvider != WeatherProvider.DISABLED) {
	    if (Monitors.getInstance().weatherData.error) {
		Utils.appendColoredText(mStatisticsText, "ERROR: ", Color.RED);
		Utils.appendColoredText(mStatisticsText, Monitors.getInstance().weatherData.errorString, Color.RED);
		mStatisticsText.append("\n\n");
	    }
	    if (Monitors.getInstance().weatherData.received) {
		mStatisticsText.append(res.getString(R.string.status_weather_last_updated));
		mStatisticsText.append("\n");
		mStatisticsText.append(res.getString(R.string.status_weather_forecast));
		mStatisticsText.append("\n");
		printDate(mStatisticsText, Monitors.getInstance().weatherData.forecastTimeStamp);
		mStatisticsText.append("  ");
		mStatisticsText.append(res.getString(R.string.status_weather_observation));
		mStatisticsText.append("\n");
		printDate(mStatisticsText, Monitors.getInstance().weatherData.timeStamp);
	    } else {
		mStatisticsText.append(res.getString(R.string.status_weather_waiting));
	    }
	}

	if (Preferences.weatherGeolocationMode != GeolocationMode.MANUAL) {
	    mStatisticsText.append("\n");
	    if (Monitors.getInstance().mLocationData.received) {
		mStatisticsText.append(res.getString(R.string.status_location_updated));
		mStatisticsText.append("\n");
		printDate(mStatisticsText, Monitors.getInstance().mLocationData.timeStamp);
	    } else {
		mStatisticsText.append(res.getString(R.string.status_location_waiting));
		mStatisticsText.append("\n");
	    }
	}

	if (Utils.isAccessibilityEnabled(context)) {
	    if (MetaWatchAccessibilityService.accessibilityReceived) {
		mAccessibilityText.setText("\n");
		Utils.appendColoredText(mAccessibilityText, res.getString(R.string.status_accessibility_working), Color.GREEN);
		mAccessibilityText.append("\n");
	    } else {
		if (startupTime == 0 || System.currentTimeMillis() - startupTime < 10 * 1000) {
		    mAccessibilityText.setText("\n" + res.getString(R.string.status_accessibility_waiting) + "\n");
		} else {
		    mAccessibilityText.setText("\n");
		    Utils.appendColoredText(mAccessibilityText, res.getString(R.string.status_accessibility_failed), Color.RED);
		    mAccessibilityText.append("\n");
		}
	    }

	} else {
	    mAccessibilityText.setText("\n" + res.getString(R.string.status_accessibility_disabled) + "\n");
	}

	mStatisticsText.append("\n" + res.getString(R.string.status_message_queue) + " " + Protocol.getInstance(context).getQueueLength());
	mStatisticsText.append("\n" + res.getString(R.string.status_notification_queue) + " " + Notification.getInstance().getQueueLength() + "\n");

	if (Preferences.showNotificationQueue) {
	    mStatisticsText.append(Notification.getInstance().dumpQueue());
	}

	mStatisticsText.append("\nStatus updated at ");
	printDate(mStatisticsText, System.currentTimeMillis());
    }

    private void printDate(TextView textView, long ticks) {
	if (ticks == 0) {
	    textView.append(context.getResources().getString(R.string.status_loading));
	} else {
	    textView.append(Utils.ticksToText(context, ticks));
	}
	textView.append("\n");
    }

    void startService() {
	context.startService(new Intent(context, MetaWatchService.class));
    }

    void stopService() {
	try {
	    context.stopService(new Intent(context, MetaWatchService.class));
	} catch (Throwable e) {
	    // The service wasn't running
	    if (Preferences.logging)
		Log.d(TAG, e.getMessage());
	}
    }
}