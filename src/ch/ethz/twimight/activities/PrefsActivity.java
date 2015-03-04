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
package ch.ethz.twimight.activities;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import ch.ethz.twimight.R;
import ch.ethz.twimight.net.Html.HtmlPage;
import ch.ethz.twimight.net.opportunistic.ScanningAlarm;
import ch.ethz.twimight.net.opportunistic.ScanningService;
import ch.ethz.twimight.net.twitter.TwitterAlarm;
import ch.ethz.twimight.util.Constants;

/**
 * Shows the preferences.
 * @author thossmann
 * @author pcarta
 */
public class PrefsActivity extends PreferenceActivity{

	protected static final String TAG = "PreferenceActivity";

	private OnSharedPreferenceChangeListener prefListener;
	private SharedPreferences prefs;
	BluetoothAdapter mBluetoothAdapter;
	static final int REQUEST_DISCOVERABLE = 2;

	/**
	 * Set everything up.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);		
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		prefListener = new OnSharedPreferenceChangeListener() {

			// this is where we take action after the user changes a setting!
			public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {

				if (key.equals(getString(R.string.prefDisasterMode))) { // toggle disaster mode
					if(preferences.getBoolean(getString(R.string.prefDisasterMode), Constants.DISASTER_DEFAULT_ON) == true){
						
						if (LoginActivity.getTwitterId(getBaseContext())!= null && LoginActivity.getTwitterScreenname(getBaseContext()) != null) {
							enableDisasterMode(); 							
							// Are we in disaster mode?		
								
						} 
						
					} else {						
						disableDisasterMode(getApplicationContext());						
						finish();
					}
					
				} else if(key.equals(getString(R.string.prefTDSCommunication))){
					
					// toggle TDS communication
					if(preferences.getBoolean(getString(R.string.prefTDSCommunication),	Constants.TDS_DEFAULT_ON) == true){
						//new TDSAlarm(getApplicationContext(), Constants.TDS_UPDATE_INTERVAL);
						Log.i(TAG, "start TDS communication");
					} else {
						//stopService(new Intent(getApplicationContext(), TDSService.class));
						//TDSAlarm.stopTDSCommuniction(getApplicationContext());						
					}
					
				}  else if (key.equals(getString(R.string.prefRunAtBoot))) {
					
					if (preferences.getBoolean(getString(R.string.prefRunAtBoot), Constants.TWEET_DEFAULT_RUN_AT_BOOT) == true ) {
						ListPreference updatesBackground = (ListPreference) getPreferenceScreen().findPreference(getString(R.string.prefUpdateInterval));
						updatesBackground.setEnabled(true);
						new TwitterAlarm(getBaseContext(),false);
					
					} else {
						ListPreference updatesBackground = (ListPreference) getPreferenceScreen().findPreference(getString(R.string.prefUpdateInterval));
						updatesBackground.setEnabled(false);
						TwitterAlarm.stopTwitterAlarm(getBaseContext());
					}
				} else if (key.equals(getString(R.string.prefUpdateInterval))) {					
					Constants.UPDATER_UPDATE_PERIOD = Long.parseLong(preferences.getString(getString(R.string.prefUpdateInterval), "5") ) * 60 * 1000L;
					Log.i(TAG, "new update interval: " + Constants.UPDATER_UPDATE_PERIOD );
					
					//start the twitter update alarm
					if(PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean(getString(R.string.prefRunAtBoot), Constants.TWEET_DEFAULT_RUN_AT_BOOT)==true){			
						new TwitterAlarm(getBaseContext(), false);
					}
				} else if (key.equals(getString(R.string.pref_offline_mode))) {		

					if(preferences.getBoolean(getString(R.string.pref_offline_mode), Constants.OFFLINE_DEFAULT_ON)){

						setOfflinePreference(true,PrefsActivity.this);
					}
					else{
						
						setOfflinePreference(false,PrefsActivity.this);

					}

				}
			}

			
		};

	}
	
	
	//get offline mode preference
	public static boolean getOfflinePreference(Context context) {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean offlinePref = prefs.getBoolean(HtmlPage.OFFLINE_PREFERENCE, false);
	
		return offlinePref;
	}
	
	//set offline mode preference
	public static void setOfflinePreference(boolean pref, Context context) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putBoolean(HtmlPage.OFFLINE_PREFERENCE, pref);
		prefEditor.commit();
	}	



	public static void disableDisasterMode(Context context) {
		if (getBluetoothInitialState(context) == false) {
			if (BluetoothAdapter.getDefaultAdapter().isEnabled())
				BluetoothAdapter.getDefaultAdapter().disable();
		}				
		ScanningAlarm.stopScanning(context);
		Intent in = new Intent(context, ScanningService.class);
		context.stopService(in);
		
	}
	/**
	 * Enables Bluetooth when Disaster Mode get's enabled.
	 */
	private void enableDisasterMode() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter.isEnabled())
			ScanningAlarm.setBluetoothInitialState(getBaseContext(), true);
		else
			ScanningAlarm.setBluetoothInitialState(getBaseContext(), false);
		//for statistics
		setDisasterModeUsed();
		
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {		
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);			
			startActivityForResult(discoverableIntent,REQUEST_DISCOVERABLE);           
			
		} else {
			new ScanningAlarm(getApplicationContext(),true);
			finish();
		}
				 
		
	}
	
	private void setDisasterModeUsed() {
		SharedPreferences.Editor edit = prefs.edit();
		edit.putBoolean(Constants.DIS_MODE_USED, true);
		edit.commit();
	}
	
	private static boolean getBluetoothInitialState(Context context) {
		
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);		
		return pref.getBoolean("wasBlueEnabled", true);
		
		}
	
	public static boolean isDisModeActive(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("prefDisasterMode", false);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case REQUEST_DISCOVERABLE:		
			
			if (resultCode == BluetoothAdapter.STATE_CONNECTING) {
				new ScanningAlarm(getApplicationContext(),true);
				finish();
			} else if (resultCode == BluetoothAdapter.STATE_DISCONNECTED) {
				SharedPreferences.Editor edit = prefs.edit();
				edit.putBoolean(getString(R.string.prefDisasterMode), Constants.DISASTER_DEFAULT_ON);
				edit.commit();	
				CheckBoxPreference disPref = (CheckBoxPreference)findPreference(getString(R.string.prefDisasterMode));
				disPref.setChecked(false);
			}
			
			
		}
	}  
	
	
	/**
	 * Important: register shared preference change listener here!
	 */
	@Override
	public void onResume(){
		super.onResume();
		prefs.registerOnSharedPreferenceChangeListener(prefListener);
	}
	
	/**
	 * Important: unregister shared preferece chnage listener here!
	 */
	@Override
	public void onPause(){
		super.onPause();
		prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
	}
	
	

}
