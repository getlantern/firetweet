package org.getlantern.firetweet.extension.streaming;

import org.getlantern.firetweet.Firetweet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class FiretweetLaunchReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		final Intent service_intent = new Intent(context, StreamingService.class);
		if (Firetweet.BROADCAST_HOME_ACTIVITY_ONCREATE.equals(action)) {
			context.startService(service_intent);
		} else if (Firetweet.BROADCAST_HOME_ACTIVITY_ONDESTROY.equals(action)) {
			context.stopService(service_intent);
		}
	}

}
