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
 * CallVibrate.java                                                          *
 * CallVibrate                                                               *
 * While in incoming call mode                                               *
 *                                                                           *
 *                                                                           *
 *****************************************************************************/

package org.metawatch.manager;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WatchType;

import android.content.Context;

public class CallVibrate implements Runnable {

    private Context mContext;

    public CallVibrate(Context context) {
	mContext = context;
    }

    public void run() {

	while (Call.isRinging) {
	    Protocol.getInstance(mContext).vibrate(1000, 1000, 1);
	    if (Preferences.notifyLight)
		Protocol.getInstance(mContext).ledChange(true);
	    if (MetaWatchService.watchType == WatchType.DIGITAL)
		Protocol.getInstance(mContext).updateLcdDisplay(MetaWatchService.WatchBuffers.NOTIFICATION);
	    else
		Protocol.getInstance(mContext).updateOledsNotification();

	    try {
		Thread.sleep(2000);
	    } catch (InterruptedException e) {
	    }
	}

    }

}
