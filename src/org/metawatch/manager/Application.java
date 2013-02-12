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
 * Application.java                                                          *
 * Application                                                               *
 * Application watch mode                                                    *
 *                                                                           *
 *                                                                           *
 *****************************************************************************/

package org.metawatch.manager;

import org.metawatch.manager.apps.ApplicationBase;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

public class Application {
    // FIXME This class has next to NO support for analog watches...

    public final static byte EXIT_APP = 100;
    public final static byte TOGGLE_APP = 101;

    private static ApplicationBase currentApp = null;

    public static void startAppMode(Context context) {
	startAppMode(context, null);
    }

    public static void startAppMode(Context context, ApplicationBase internalApp) {
	if (currentApp != null) {
	    stopAppMode(context);
	}

	MetaWatchService.WatchModes.APPLICATION = true;
	currentApp = internalApp;
    }

    public static void stopAppMode(Context context) {
	MetaWatchService.WatchModes.APPLICATION = false;

	if (currentApp != null) {
	    currentApp.deactivate(context, MetaWatchService.watchType);
	    currentApp.setInactive();
	}
	currentApp = null;

	if (MetaWatchService.WatchModes.IDLE == true) {
	    Idle.getInstance().toIdle(context);
	}
    }

    public static void updateAppMode(Context context) {
	Bitmap bitmap;
	if (currentApp != null) {
	    bitmap = currentApp.update(context, false, MetaWatchService.watchType);
	} else {
	    bitmap = Protocol.getInstance(context).createTextBitmap(context, "Starting application mode ...");
	}

	updateAppMode(context, bitmap);
    }

    public static void updateAppMode(Context context, Bitmap bitmap) {
	MetaWatchService.WatchModes.APPLICATION = true;

	if (MetaWatchService.WatchModes.APPLICATION == true) {

	    // enable app mode if there is no parent mode currently active
	    if (MetaWatchService.watchState < MetaWatchService.WatchStates.APPLICATION)
		MetaWatchService.watchState = MetaWatchService.WatchStates.APPLICATION;

	    if (MetaWatchService.watchState == MetaWatchService.WatchStates.APPLICATION) {
		Protocol.getInstance(context).sendLcdBitmap(bitmap, MetaWatchService.WatchBuffers.APPLICATION);
		Protocol.getInstance(context).configureIdleBufferSize(false);
		Protocol.getInstance(context).updateLcdDisplay(MetaWatchService.WatchBuffers.APPLICATION);
	    }
	}
    }

    public static void toApp(final Context context) {
	MetaWatchService.watchState = MetaWatchService.WatchStates.APPLICATION;

	// Idle app pages uses the same button mode, so disable those buttons.
	Idle.getInstance().deactivateButtons(context);

	int watchType = MetaWatchService.watchType;
	if (currentApp != null) {
	    currentApp.activate(context, watchType);
	    updateAppMode(context);
	}
	if (watchType == MetaWatchService.WatchType.DIGITAL) {
	    Protocol.getInstance(context).enableButton(0, 1, EXIT_APP, MetaWatchService.WatchBuffers.APPLICATION); // right
	} else if (watchType == MetaWatchService.WatchType.ANALOG) {
	    Protocol.getInstance(context).enableButton(1, 1, EXIT_APP, MetaWatchService.WatchBuffers.APPLICATION); // right
	}
	
	Protocol.getInstance(context).configureIdleBufferSize(false);
	// update screen with cached buffer
	Protocol.getInstance(context).updateLcdDisplay(MetaWatchService.WatchBuffers.APPLICATION);
    }

    public static void buttonPressed(Context context, int button) {
	if (button == EXIT_APP) {
	    stopAppMode(context);

	} else if (currentApp != null) {
	    if (currentApp.buttonPressed(context, button) != ApplicationBase.BUTTON_USED_DONT_UPDATE) {
		updateAppMode(context);
	    }

	} else {
	    // Broadcast button to external app
	    Intent intent = new Intent("org.metawatch.manager.BUTTON_PRESS");
	    intent.putExtra("button", button);
	    context.sendBroadcast(intent);
	}
    }
}
