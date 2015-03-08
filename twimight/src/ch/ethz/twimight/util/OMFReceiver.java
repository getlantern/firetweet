package ch.ethz.twimight.util;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import ch.ethz.twimight.activities.LoginActivity;
import ch.ethz.twimight.activities.PrefsActivity;
import ch.ethz.twimight.activities.ShowTweetListActivity;
import ch.ethz.twimight.net.opportunistic.ScanningAlarm;

public class OMFReceiver extends BroadcastReceiver {
	
	public static final String TAG = "OMFRedeiver";
	public static final String AUTOM_ENABLE_DISASTER_MODE = "autom_enable_disaster_mode";
	public static final String SWITCH_DIS_MODE_STATUS = "switch_disaster_mode_status";
	public static final String DM_DELAY = "dm_delay";
    
	Handler handler = new Handler();
	
	@Override
	public void onReceive(Context context, Intent intent) {		

		// we only start the services if we are logged in (i.e., we have the tokens from twitter)
				if(LoginActivity.hasAccessToken(context) && LoginActivity.hasAccessTokenSecret(context)){
					
					if (intent.getAction().equals(AUTOM_ENABLE_DISASTER_MODE)) {						
						
						
						if ( intent.getStringExtra(SWITCH_DIS_MODE_STATUS).equals("on")) {							
							
							int delay = intent.getIntExtra(DM_DELAY, 60);							
							handler.postDelayed(new StartDMDelayed(context),delay * 1000);						
							
							
						} else if ( intent.getStringExtra(SWITCH_DIS_MODE_STATUS).equals("off")){
							PrefsActivity.disableDisasterMode(context);
							setPreferences(context,false);
							Toast.makeText(context, "Disaster Mode disabled by OMF", Toast.LENGTH_SHORT).show();
						}
						createActivity(context);
						
					}
								
					
				}

	}
	
	private void createActivity(Context context) {
		Intent refreshIntent = new Intent(context,ShowTweetListActivity.class);
		refreshIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);		
		context.startActivity(refreshIntent);
		
	}

	private void setPreferences(Context context, boolean value) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean("prefDisasterMode", value);
		editor.commit();
	}
	
	private class StartDMDelayed implements Runnable {
		Context context;
		
		public StartDMDelayed(Context context) {			
			this.context = context;
		}
		
		@Override
		public void run() {
			
			BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter.isEnabled())
				ScanningAlarm.setBluetoothInitialState(context, true);
			else
				ScanningAlarm.setBluetoothInitialState(context, false);
			
			mBluetoothAdapter.enable();
			setPreferences(context,true);
			new ScanningAlarm(context,true);
			Toast.makeText(context, "Disaster Mode enabled by OMF", Toast.LENGTH_SHORT).show();
			createActivity(context);
			
		}
		
	}

}
