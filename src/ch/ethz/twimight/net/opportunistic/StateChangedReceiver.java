package ch.ethz.twimight.net.opportunistic;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StateChangedReceiver extends BroadcastReceiver {

	private static final String T = "btdebug";
	
	public static interface BtSwitchingFinished {

		public void onSwitchingFinished();
	}

	private BtSwitchingFinished sf;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED))	{
			int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
			Log.d(T, "bt state changed "+ state);
			if (state == BluetoothAdapter.STATE_OFF){
				BluetoothAdapter.getDefaultAdapter().enable();				
				Log.d(T, "state: off");
			} else if (state == BluetoothAdapter.STATE_ON) {
				sf.onSwitchingFinished();
				Log.d(T, "state: on");
			}

		}

	}
	
	public void setListener(BtSwitchingFinished sf) {
		this.sf = sf;
	}

}
