                                                                     
                                                                     
                                                                     
                                             
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
import org.metawatch.manager.Monitors.LocationData;
import org.metawatch.manager.apps.AppManager;

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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.bugsense.trace.BugSenseHandler;

public class MetaWatchStatus extends SherlockFragment {
	
	public static final String TAG = "MetaWatchStatus";
	public static Button mStatistics = null;	
	public static ToggleButton toggleButton = null;
	private static SherlockFragmentActivity context = null;
    public static Messenger mService = null;
    public static long startupTime = 0;
    final static IncomingHandler mIncomingHandler = new IncomingHandler();
    final static Messenger mMessenger = new Messenger(new IncomingHandler());
	
	public static MetaWatchStatus newInstance() {
		return new MetaWatchStatus();
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = (SherlockFragmentActivity) getActivity();    
        configureBugSense();
		MetaWatchService.loadPreferences(context);
		AppManager.initApps(context);
        
        startupTime = System.currentTimeMillis();
        
		if (Preferences.watchMacAddress == "") {
			// Show the watch discovery screen on first start
			startActivity(new Intent(context, DeviceSelection.class));
		}
		
		Protocol.configureMode();
		
		MetaWatchService.autoStartService(context);
    }

    private void configureBugSense() {
        try {
			InputStream inputStream = context.getAssets().open("bugsense.txt");
			String key = Utils.ReadInputStream(inputStream);
			key=key.trim();
			if (Preferences.logging) Log.d(TAG, "BugSense enabled");
			BugSenseHandler.initAndStartSession(context, key);
		} catch (IOException e) {
			if (Preferences.logging) Log.d(TAG, "No BugSense keyfile found");
		}		
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View view = inflater.inflate(R.layout.main, null);
    	mStatistics = (Button) view.findViewById(R.id.statistics);
    	mStatistics.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Builder builder = new Builder(context);
				builder.setIcon(R.drawable.icon);
				builder.setTitle(R.string.statistics);
				builder.setView(getStatistics());
				builder.setPositiveButton(android.R.string.ok, null);
				builder.show();
			}
    	});
		toggleButton = (ToggleButton) view.findViewById(R.id.toggleButton);
		toggleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if(toggleButton.isChecked())
            		startService();
            	else
            		stopService();
            }
        });
    	return view;
    }
    
    @Override
	public void onResume() {
    	super.onResume();
    	context.bindService(new Intent(context, MetaWatchService.class), mConnection, 0);
		context.startService(new Intent(context, MetaWatchService.class));
		displayStatus();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	try {
        	if (mConnection != null)
        		context.unbindService(mConnection);
    	} catch(Throwable e) {
    		//Service not running
    	}
    }
    
    static void displayStatus() {
    	setButtonState(context);
    	ActionBar actionBar = context.getSupportActionBar();
    	if (actionBar == null)
    		return;
    	switch (MetaWatchService.connectionState) {
	    	case MetaWatchService.ConnectionState.DISCONNECTED:
	    		actionBar.setTitle(context.getString(R.string.app_name) + ": " + context.getString(R.string.connection_disconnected));
	    		break;
	    	case MetaWatchService.ConnectionState.CONNECTING:
	    		actionBar.setTitle(context.getString(R.string.app_name) + ": " + context.getString(R.string.connection_connecting));
	    		break;
	    	case MetaWatchService.ConnectionState.CONNECTED:
	    		actionBar.setTitle(context.getString(R.string.app_name) + ": " + context.getString(R.string.connection_connected));
	    		break;
	    	case MetaWatchService.ConnectionState.DISCONNECTING:
	    		actionBar.setTitle(context.getString(R.string.app_name) + ": " + context.getString(R.string.connection_disconnecting));
	    		break;
    	}
    }
    
    public TextView getStatistics() {
    	TextView textView = new TextView(context);
    	textView.setGravity(Gravity.CENTER);
    	Resources res = context.getResources();
    	if (Preferences.weatherProvider != WeatherProvider.DISABLED) {
    		if (Monitors.weatherData.error) {
    			Utils.appendColoredText(textView, "ERROR: " , Color.RED);
    			Utils.appendColoredText(textView, Monitors.weatherData.errorString, Color.RED);
    			textView.append("\n\n");
    		}
    		if (Monitors.weatherData.received) {
    			textView.append(res.getString(R.string.status_weather_last_updated));
    			textView.append("\n");
    			textView.append(res.getString(R.string.status_weather_forecast));
    			textView.append("\n");
    			printDate(textView, Monitors.weatherData.forecastTimeStamp);
    			textView.append("  ");
    			textView.append(res.getString(R.string.status_weather_observation));
    			textView.append("\n");
    			printDate(textView, Monitors.weatherData.timeStamp);
    		}
    		else {
    			textView.append(res.getString(R.string.status_weather_waiting));
    		}
    	}
    	
    	if (Preferences.weatherGeolocationMode != GeolocationMode.MANUAL) {
    		textView.append("\n");
    		if (LocationData.received) {
    			textView.append(res.getString(R.string.status_location_updated));
    			textView.append("\n");
    			printDate(textView, LocationData.timeStamp);
    		}
    		else {
    			textView.append(res.getString(R.string.status_location_waiting));
    			textView.append("\n");
    		}
    	}
    	
    	textView.append("\n");
    	if (Utils.isAccessibilityEnabled(context)) {    		
	    	if (MetaWatchAccessibilityService.accessibilityReceived) {
	    		Utils.appendColoredText(textView, res.getString(R.string.status_accessibility_working), Color.GREEN);
	    	}
	    	else {
	    		if(startupTime==0 || System.currentTimeMillis()-startupTime<60*1000) {
	    			textView.append(res.getString(R.string.status_accessibility_waiting));
	    		}
	    		else {
	    			Utils.appendColoredText(textView, res.getString(R.string.status_accessibility_failed), Color.RED);
	    		}
	    	}
	    }
    	else {
    		textView.append(res.getString(R.string.status_accessibility_disabled));
    	}
    	textView.append("\n");
    
    	textView.append("\n"+res.getString(R.string.status_message_queue)+" " + Protocol.getQueueLength());
    	textView.append("\n"+res.getString(R.string.status_notification_queue)+" " + Notification.getQueueLength() + "\n");
    	
    	if(Preferences.showNotificationQueue) {
    		textView.append(Notification.dumpQueue());
    	}
    	
    	textView.append("\nStatus updated at ");
		printDate(textView, System.currentTimeMillis());
		return textView;
    }
    
    private static void printDate(TextView textView, long ticks) {
    	if(ticks==0) {
    		textView.append(context.getResources().getString(R.string.status_loading));
    	}
    	else {
	    	textView.append(Utils.ticksToText(context, ticks));
    	}
    	textView.append("\n");
    }
    
	void startService() {
		if(!MetaWatchService.isRunning()) {
			context.bindService(new Intent(context, MetaWatchService.class), mConnection, Context.BIND_AUTO_CREATE);
			context.startService(new Intent(context, MetaWatchService.class));
		}
        setButtonState(context);
	}
	
    void stopService() {
        try {
        	context.stopService(new Intent(context, MetaWatchService.class));
            context.unbindService(mConnection);            	
        }
        catch(Throwable e) {
        	// The service wasn't running
        	if (Preferences.logging) Log.d(TAG, e.getMessage());          	
        }
        setButtonState(context);
    }

	private static void setButtonState(Context context) {
		if (toggleButton!=null)
			toggleButton.setChecked(MetaWatchService.isRunning());
	}
      
    /**
     * Handler of incoming messages from service.
     */
    static class IncomingHandler extends Handler {
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

    public static ServiceConnection mConnection = new ServiceConnection() {
    	   	
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