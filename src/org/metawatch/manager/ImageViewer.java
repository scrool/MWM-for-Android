                                                                     
                                                                     
                                                                     
                                             
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
  * ImageViewer.java                                                          *
  * ImageViewer                                                               *
  * System wide "Send to watch" that shows pictures in app mode               *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/

package org.metawatch.manager;

import java.io.FileNotFoundException;
import java.io.InputStream;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.Notification.VibratePattern;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import org.metawatch.manager.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class ImageViewer extends SherlockFragmentActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        Intent i = getIntent();
        String action = i.getAction();
        Uri u = null;
        
        if (action==null) {
        	if (Preferences.logging) Log.e(MetaWatchStatus.TAG, "ImageViewer.onCreate() null action");
        	finish();
        	return;
        }

        if (action.equals(Intent.ACTION_VIEW)) {
        	u = i.getData();
        } else if (action.equals(Intent.ACTION_SEND)) {
        	u = i.getParcelableExtra(Intent.EXTRA_STREAM);
        } else {
        	if (Preferences.logging) Log.e(MetaWatchStatus.TAG, "ImageViewer.onCreate() Unknown intent: " + action);
        	finish();
        	return;
        }
        
        if (Preferences.logging) {
        	Log.d(MetaWatchStatus.TAG, "action: " + i.getAction());
        	Log.d(MetaWatchStatus.TAG, "data: "+ u.getPath() );
        }
        
        InputStream is;
		try {
			is = getContentResolver().openInputStream(u);
			
	        BitmapFactory.Options options = new BitmapFactory.Options();       
	        Bitmap bmp = BitmapFactory.decodeStream(is, null, options);
	        
	        if (bmp!=null) {
		        Bitmap scaled = Utils.resize(bmp, 96, 96);
		        Bitmap dithered = Utils.ditherTo1bit(scaled, Preferences.invertLCD);
		        
		        VibratePattern vibratePattern = new VibratePattern(false, 1,1,1);
		        		
		        Notification.addBitmapNotification(this, dithered, vibratePattern, -1, "Image viewer");
	        }
        
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
        
        finish();
	}
	
	

}
