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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.nineoldandroids.animation.AnimatorSet;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.bugsense.trace.BugSenseHandler;

public class MetaWatchStatus extends SherlockFragment {

    public static final String TAG = "MetaWatchStatus";
    public static Button mStatistics = null;
    public static ToggleButton toggleButton = null;
    private static SherlockFragmentActivity context = null;
    public static Messenger mService = null;
    public static long startupTime = 0;
    private TextView mStatisticsText = null;
    private AlertDialog mStatisticsDialog = null;
    private ActionBar mActionBar = null;
    private View mMainView = null;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    public static MetaWatchStatus newInstance() {
	return new MetaWatchStatus();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	context = (SherlockFragmentActivity) getActivity();
	mActionBar = context.getSupportActionBar();
	mStatisticsText = new TextView(context);
	Builder builder = new Builder(context);
	builder.setIcon(R.drawable.icon);
	builder.setTitle(R.string.statistics);
	builder.setView(mStatisticsText);
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
	toggleButton = (ToggleButton) mMainView.findViewById(R.id.toggleButton);
	toggleButton.setOnClickListener(new OnClickListener() {
	    @Override
	    public void onClick(View v) {
		wiggleButton();
		displayStatus();
		if (!MetaWatchService.isRunning())
		    startService();
		else
		    stopService();
	    }
	    private void wiggleButton() {
		ObjectAnimator right = ObjectAnimator.ofFloat(toggleButton, "translationX", 0, 5);
		right.setDuration(45);
		ObjectAnimator left = ObjectAnimator.ofFloat(toggleButton, "translationX", 5, -5);
		left.setDuration(45);
		ObjectAnimator centerHorizontal = ObjectAnimator.ofFloat(toggleButton, "translationX", -5, 0);
		centerHorizontal.setDuration(45);
		ObjectAnimator up = ObjectAnimator.ofFloat(toggleButton, "translationY", 0, -5);
		up.setDuration(45);
		ObjectAnimator down = ObjectAnimator.ofFloat(toggleButton, "translationY", -5, 5);
		down.setDuration(45);
		ObjectAnimator centerVertical = ObjectAnimator.ofFloat(toggleButton, "translationY", 5, 0);
		centerVertical.setDuration(45);
		AnimatorSet set = new AnimatorSet();
		set.playSequentially(right, left, centerHorizontal, up, down, centerVertical);
		set.start();
	    }
	});
	return mMainView;
    }

    @Override
    public void onResume() {
	super.onResume();
	startService();
	displayStatus();
    }

    @Override
    public void onPause() {
	super.onPause();
	if (mConnection != null)
	    try {
		context.unbindService(mConnection);
	    } catch (IllegalArgumentException e) {
		e.printStackTrace();
		// Service not registered
	    }
    }

    private void displayStatus() {
	setButtonState(context);
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
	Resources res = context.getResources();
	mStatisticsText.setText("");
	mStatisticsText.append("\n");
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

	mStatisticsText.append("\n");
	if (Utils.isAccessibilityEnabled(context)) {
	    if (MetaWatchAccessibilityService.accessibilityReceived) {
		Utils.appendColoredText(mStatisticsText, res.getString(R.string.status_accessibility_working), Color.GREEN);
	    } else {
		if (startupTime == 0 || System.currentTimeMillis() - startupTime < 60 * 1000) {
		    mStatisticsText.append(res.getString(R.string.status_accessibility_waiting));
		} else {
		    Utils.appendColoredText(mStatisticsText, res.getString(R.string.status_accessibility_failed), Color.RED);
		}
	    }
	} else {
	    mStatisticsText.append(res.getString(R.string.status_accessibility_disabled));
	}
	mStatisticsText.append("\n");

	mStatisticsText.append("\n" + res.getString(R.string.status_message_queue) + " " + Protocol.getInstance(context).getQueueLength());
	mStatisticsText.append("\n" + res.getString(R.string.status_notification_queue) + " " + Notification.getInstance().getQueueLength() + "\n");

	if (Preferences.showNotificationQueue) {
	    mStatisticsText.append(Notification.getInstance().dumpQueue());
	}

	mStatisticsText.append("\nStatus updated at ");
	printDate(mStatisticsText, System.currentTimeMillis());
    }

    private static void printDate(TextView textView, long ticks) {
	if (ticks == 0) {
	    textView.append(context.getResources().getString(R.string.status_loading));
	} else {
	    textView.append(Utils.ticksToText(context, ticks));
	}
	textView.append("\n");
    }

    void startService() {
	displayStatus();
	new Thread(new Runnable() {
	    @Override
	    public void run() {
		context.bindService(new Intent(context, MetaWatchService.class), mConnection, Context.BIND_AUTO_CREATE);
		context.startService(new Intent(context, MetaWatchService.class));
	    }
	}).start();
	if (!MetaWatchService.isRunning())
	    Toast.makeText(context, context.getString(R.string.main_service_toggle_starting), Toast.LENGTH_SHORT).show();
    }

    void stopService() {
	try {
	    context.stopService(new Intent(context, MetaWatchService.class));
	    context.unbindService(mConnection);
	} catch (Throwable e) {
	    // The service wasn't running
	    if (Preferences.logging)
		Log.d(TAG, e.getMessage());
	}
	displayStatus();
    }

    private static void setButtonState(Context context) {
	if (toggleButton != null)
	    toggleButton.setChecked(MetaWatchService.isRunning());
    }

    /**
     * Handler of incoming messages from service.
     */
    private class IncomingHandler extends Handler {
	@Override
	public void handleMessage(Message msg) {
	    switch (msg.what) {
	    case MetaWatchService.Msg.UPDATE_STATUS:
		displayStatus();
		break;
	    default:
		super.handleMessage(msg);
	    }
	}
    }

    private ServiceConnection mConnection = new ServiceConnection() {

	public void onServiceConnected(ComponentName className, IBinder service) {
	    mService = new Messenger(service);
	    try {
		Message msg = Message.obtain(null, MetaWatchService.Msg.REGISTER_CLIENT);
		msg.replyTo = mMessenger;
		mService.send(msg);
	    } catch (RemoteException e) {
		e.printStackTrace();
	    }
	}

	public void onServiceDisconnected(ComponentName className) {
	    // This is called when the connection with the service has been
	    // unexpectedly disconnected -- that is, its process crashed.
	    mService = null;
	}
    };
}