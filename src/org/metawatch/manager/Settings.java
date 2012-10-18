                                                                     
                                                                     
                                                                     
                                             
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
  * Settings.java                                                             *
  * Settings                                                                  *
  * Preference activity                                                       *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/

package org.metawatch.manager;

import java.util.Map;

import org.metawatch.communityedition.R;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.apps.AppManager;
import org.metawatch.manager.apps.ApplicationBase.AppData;
import org.metawatch.manager.widgets.WidgetManager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.util.Log;

public class Settings extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.layout.settings);

		EditTextPreference editTextMac = (EditTextPreference)findPreference("MAC");
		editTextMac.setText(MetaWatchService.Preferences.watchMacAddress);
		
		Preference discovery = findPreference("Discovery");
		discovery.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {
				
				if (Preferences.logging) Log.d(MetaWatch.TAG, "discovery click");
				
				startActivity(new Intent(Settings.this, DeviceSelection.class));
				
				return false;
			}
		});
		
		Preference theme = findPreference("Theme");
		theme.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {
				
				if (Preferences.logging) Log.d(MetaWatch.TAG, "theme click");
				
				startActivity(new Intent(Settings.this, ThemeContainer.class));
				
				return false;
			}
		});
		
		
		Preference otherAppsList = findPreference("otherAppsList");
		otherAppsList.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {
				startActivity(new Intent(Settings.this, OtherAppsList.class));
				return false;
			}
		});

		Preference resetWidgets = findPreference("ResetWidgets");
		resetWidgets.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {
				WidgetManager.resetWidgetsToDefaults(Settings.this);
				return false;
			}
		});
		
		Preference backup = findPreference("Backup");
		backup.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {
				Utils.backupUserPrefs(Settings.this);
				return false;
			}
		});
		
		Preference restore = findPreference("Restore");
		restore.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {
				if( Utils.restoreUserPrefs(Settings.this) ) {		
					// Restart				
					AlarmManager alm = (AlarmManager) Settings.this.getSystemService(Context.ALARM_SERVICE);
					alm.set(AlarmManager.RTC,
							System.currentTimeMillis() + 1000,
							PendingIntent.getActivity(Settings.this,
									0,
									new Intent(Settings.this, MetaWatch.class),
									0));					
					android.os.Process.sendSignal(android.os.Process.myPid(), android.os.Process.SIGNAL_KILL);
				}
				return false;
			}
		});
		
		// InsecureBtSocket requires API10 or higher
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion < android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			findPreference("InsecureBtSocket").setEnabled(false);	
		}
		
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		
		// Dynamically add the "Enabled app" controls
		PreferenceCategory appGroup = (PreferenceCategory) findPreference("ActiveApps");
		
		appGroup.removeAll();
		
		AppData[] data = AppManager.getAppInfos();	
		for (AppData appEntry : data) {
			
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Adding setting for "+appEntry.id);
		
			CheckBoxPreference test = new CheckBoxPreference(this);
			test.setKey(appEntry.getPageSettingName());
			test.setTitle(appEntry.name);
			
			appGroup.addPreference(test);		
		}
		
		
		ListPreferenceMultiSelect calendars = (ListPreferenceMultiSelect) findPreference("DisplayCalendars");
		final Map<String, Integer> calData = Utils.getCalendars(this);
		
    	Resources res = getResources();
		
		CharSequence[] entriesArray = new CharSequence[calData.size()+1];
		CharSequence[] entryValuesArray = new CharSequence[calData.size()+1];
		
		entriesArray[0] = res.getString(R.string.settings_calendar_all);
		entryValuesArray[0] = "#ALL#";
		
		int i=1;
		for(String name : calData.keySet())
		{
			entriesArray[i] = name;
			entryValuesArray[i] = calData.get(name).toString();
			i++;
		}
			
		calendars.setEntries(entriesArray);
		calendars.setEntryValues(entryValuesArray);
		
		super.onResume();
	}
	
	

}
