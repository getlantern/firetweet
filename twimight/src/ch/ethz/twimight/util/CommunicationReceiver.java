/*******************************************************************************
 * Copyright (c) 2011 ETH Zurich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Paolo Carta - Implementation
 *     Theus Hossmann - Implementation
 *     Dominik Schatzmann - Message specification
 ******************************************************************************/
package ch.ethz.twimight.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import ch.ethz.twimight.activities.LoginActivity;
import ch.ethz.twimight.net.Html.StartServiceHelper;
import ch.ethz.twimight.net.tds.TDSAlarm;
import ch.ethz.twimight.net.twitter.TwitterService;

/**
 * Listends for changes in connectivity and starts the TDSThread if a new connection
 * is detected.
 * @author thossmann
 *
 */
public class CommunicationReceiver extends BroadcastReceiver {

	private static final String TAG = "CommunicationReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.i(TAG,"CALLED");
		// connectivity changed!
		StartServiceHelper.startService(context);

		// TDS communication
		if(TDSAlarm.isTdsEnabled(context)){
			// remove currently scheduled updates and schedule an immediate one
			new TDSAlarm();
		}

		Intent i = new Intent(TwitterService.SYNCH_ACTION);
		if (!LoginActivity.hasTwitterId(context)) {					
			i.putExtra("synch_request", TwitterService.SYNCH_VERIFY);					
		} else {					
			i.putExtra("synch_request", TwitterService.SYNCH_TRANSACTIONAL);

		}
		context.startService(i);


	}			

}







