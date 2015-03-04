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
/**
 * 
 */
package ch.ethz.twimight.net.twitter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import ch.ethz.twimight.util.Constants;

/**
 * Regularly schedules and handles alarms to fetch updates from twitter
 * @author pcarta
 *
 */
public class TwitterAlarm extends BroadcastReceiver {
	
	private static final String WAKE_LOCK = "TwitterLock";
	private static final String TAG = "TwitterAlarm";
	private static WakeLock wakeLock;
	Intent intent;
	private boolean isLogin = false;
	
	public TwitterAlarm(){}
	
	public TwitterAlarm(Context context, boolean isLogin) {	
		
	    AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		this.isLogin=isLogin;
		intent = new Intent(context, TwitterAlarm.class);		
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);	
		
		alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, 0, Constants.UPDATER_UPDATE_PERIOD, pendingIntent);
		Log.i(TAG, "alarm set");
	}

	/**
	 * This is executed when the alarm goes off.	  
	 * 
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		   //getWakeLock(context);					
			
			Intent i = new Intent(TwitterService.SYNCH_ACTION);
			i.putExtra("synch_request", TwitterService.SYNCH_ALL);
			
			if (isLogin) {
				i.putExtra("isLogin",true );
				isLogin=false;				
			} 
			
			context.startService(i);			
			
		
	}
	
	
	
	/**
	 * Stop the scheduled alarm
	 * @param context
	 */
	public static void stopTwitterAlarm(Context context) {
		AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		
		Intent intent = new Intent(context, TwitterAlarm.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		alarmMgr.cancel(pendingIntent);
	}
	
	/**
	 * Acquire the Wake Lock
	 * @param context
	 */
	public static void getWakeLock(Context context){
		
		//releaseWakeLock();
		
		PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK); 
		wakeLock.acquire();
	}

	/**
	 * release the wake lock after onReceive is done!
	 * @param context
	 */
	public static void releaseWakeLock(){
		if(wakeLock != null)
			if(wakeLock.isHeld())
				wakeLock.release();
	}

	

}
