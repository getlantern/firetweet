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
package ch.ethz.twimight.net.tds;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import ch.ethz.twimight.util.Constants;

/**
 * TDSAlarm, together with TDSThread 
 * form the controller for the communication with the Twimight
 * Disaster Server.
 * TDSAlarm periodically schedules (and receives) Alarms to
 * launch a TDSThread.
 * @author thossmann
 *
 */
public class TDSAlarm extends BroadcastReceiver {

	private static final String WAKE_LOCK = "TDSLock";
	private static final String TAG = "TDSAlarm";
	private static WakeLock wakeLock;
	
	/**
	 * This constructor is called the alarm manager.
	 */
	public TDSAlarm() {};

	/**
	 * Call this constructor to trigger set everything up and schedule the first alarm.
	 * @param context
	 * @param timeOut
	 */
	public TDSAlarm(Context context, long timeOut){
		
		// determine the time of the last successful update
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Long lastUpdate = prefs.getLong("LastUpdate", 0);

		AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		
		Intent intent = new Intent(context, TDSAlarm.class);		
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// cancel scheduled alarms
		alarmMgr.cancel(pendingIntent);

		if(System.currentTimeMillis() - lastUpdate >= Constants.TDS_UPDATE_INTERVAL){	
			// schedule one immediately
			alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (90 * 1000L), pendingIntent);
		
		} else {
			// schedule update interval after last update
			alarmMgr.set(AlarmManager.RTC_WAKEUP, lastUpdate + Constants.TDS_UPDATE_INTERVAL, pendingIntent);
			
		}
	}

	/**
	 * Stop the scheduled alarm
	 * @param context
	 */
	public static void stopTDSCommuniction(Context context) {
		AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		
		Intent intent = new Intent(context, TDSAlarm.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		alarmMgr.cancel(pendingIntent);
	}
	
	/**
	 * Schedule a new alarm
	 * @param context
	 * @param delay
	 */
	public static void scheduleCommunication(Context context, long delay) {
		if(isTdsEnabled(context)){
			AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			
			Intent intent = new Intent(context, TDSAlarm.class);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+delay, pendingIntent);
			
		}
	}


	/**
	 * This is executed when the alarm goes off.
	 * @author thossmann
	 * @author pcarta
	 */
	@Override
	public void onReceive(Context context, Intent intent) {

		
		// Acquire wake lock so we don't fall asleep after onReceive returns
		//getWakeLock(context);

		if(isTdsEnabled(context)){
			
			if(PreferenceManager.getDefaultSharedPreferences(context).getString("mac", null) != null){ 
				// Request the sync
				Intent synchIntent = new Intent(context, TDSService.class);
				synchIntent.putExtra("synch_request", TDSService.SYNCH_ALL);
				context.startService(synchIntent);
			}
			
			// can we obtain the Bluetooth MAC?
			else if(BluetoothAdapter.getDefaultAdapter() !=null && BluetoothAdapter.getDefaultAdapter().isEnabled()){
				
				getMacFromAdapter(context);				
				Intent synchIntent = new Intent(context, TDSService.class);
				synchIntent.putExtra("synch_request", TDSService.SYNCH_ALL);
				context.startService(synchIntent);
			}
			// do we have a MAC address now? if not, we have to ask the user to switch on bluetooth, since we cannot obtain the address from the BluetoothAdapter when Bluetooth is off
			else {				
				
				enableBluetooth(context); // this will also schedule a TDSThread, once bluetooth is done.
				
			} 
		}

	}

	/**
	 * Acquire the Wake Lock
	 * @param context
	 */
	public static void getWakeLock(Context context){
		
		releaseWakeLock();
		
		PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK , WAKE_LOCK); 
		wakeLock.acquire();
	}

	/**
	 * We have to make sure to release the wake lock after the TDSThread is done!
	 * @param context
	 */
	public static void releaseWakeLock(){
		if(wakeLock != null)
			if(wakeLock.isHeld())
				wakeLock.release();
	}

	/**
	 * Enable Bluetooth for 2 seconds to read the local mac address
	 * Note: this is an ugly hack, please close both eyes here!
	 */
	protected static void enableBluetooth(final Context context) {

		new Thread(new Runnable() {
			@Override
			public void run() {				
					int attempts = 0;
					while(PreferenceManager.getDefaultSharedPreferences(context).getString("mac", null) == null && attempts <= 3){
						BluetoothAdapter.getDefaultAdapter().enable();
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							
						}
						// can we now get an address?
						getMacFromAdapter(context);
						attempts++;
					}
					BluetoothAdapter.getDefaultAdapter().disable();

					if(PreferenceManager.getDefaultSharedPreferences(context).getString("mac", null) != null && 
							PreferenceManager.getDefaultSharedPreferences(context).getBoolean("prefTDSCommunication", Constants.TDS_DEFAULT_ON) == true ){
						// Request the sync
						Intent synchIntent = new Intent(context, TDSService.class);
						synchIntent.putExtra("synch_request", TDSService.SYNCH_ALL);
						context.startService(synchIntent);
					} else {
					
						scheduleCommunication(context, Constants.TDS_UPDATE_INTERVAL);
					}
				
			}
		}).start();

	}

	/**
	 * Saves the MAC address to SharedPreferences
	 * @return false if we can't get a MAC address (Bluetooth off)
	 */
	private static boolean getMacFromAdapter(Context context){
		String mac = BluetoothAdapter.getDefaultAdapter().getAddress();
		if(mac != null){
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
			editor.putString("mac", mac);
			editor.commit();
			return true;
		}
		return false;

	}

	/**
	 * Checks the settings
	 * @return true if TDS updates are enabled, false otherwise
	 */
	public static boolean isTdsEnabled(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("prefTDSCommunication", Constants.TDS_DEFAULT_ON);
	}
	
}
