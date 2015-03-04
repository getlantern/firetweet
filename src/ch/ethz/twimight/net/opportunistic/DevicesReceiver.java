package ch.ethz.twimight.net.opportunistic;

import java.util.ArrayList;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.util.Log;
import ch.ethz.twimight.activities.ShowTweetListActivity;
import ch.ethz.twimight.data.MacsDBHelper;

public class DevicesReceiver extends BroadcastReceiver {

	public static interface ScanningFinished {

		public void onScanningFinished();
	}

	private static final String TAG = "DevicesReceiver";
	private static final String T = "btdebug";

	private ScanningFinished sf;
	MacsDBHelper dbHelper;
	BluetoothAdapter mBtAdapter;
	SharedPreferences sharedPref;

	private static final String DISCOVERY_FINISHED_TIMESTAMP = "discovery_finished_timestamp";
	private ArrayList<String> newDeviceList = null;

	public static final String SCAN_PROBABILITY = "scan_probability";
	public static final String DEVICE_LIST = "device_list";

	private static final float INIT_PROB = (float) 0.5;
	private static final float MAX_PROB = (float) 1.0;
	private static final float MIN_PROB = (float) 0.1;
	
	private int discoveredSmartphonesCount = 0;

	public DevicesReceiver(Context context) {
		dbHelper = new MacsDBHelper(context);
		dbHelper.open();
		sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();

	}

	private void addPairedDevices() {
		// Get a set of currently paired devices
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		if (pairedDevices != null) {
			// If there are paired devices, add each one to the ArrayAdapter
			if (pairedDevices.size() > 0) {

				for (BluetoothDevice device : pairedDevices) {
					if (!dbHelper.updateMacActive(device.getAddress()
							.toString(), 1)) {
						dbHelper.createMac(device.getAddress().toString(), 1);
					}

				}
			}
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(T, "DevicesReceiver onReceive()");
		Log.d(T, "discovery running: " + mBtAdapter.isDiscovering());
		String action = intent.getAction();
		// When discovery finds a device
		if (BluetoothDevice.ACTION_FOUND.equals(action)) {

			// Get the BluetoothDevice object from the Intent
			BluetoothDevice device = intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

			// newDeviceList.add(device.getAddress().toString());
			if (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_SMART
					&& device.getBondState() != BluetoothDevice.BOND_BONDED) {
				Log.d(T, "ACTION_FOUND special: " + device.getName() + " ("
						+ device.getAddress() + ")");
				discoveredSmartphonesCount++;
				ParcelUuid[] uuids = device.getUuids();
				if (uuids != null) {
					for (ParcelUuid uuid : uuids) {
						Log.d(T, uuid.toString());
					}
				}
				if (!dbHelper
						.updateMacActive(device.getAddress().toString(), 1)) {
					dbHelper.createMac(device.getAddress().toString(), 1);
				}

			}

			// When discovery is finished...
		} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
			Log.d(T, "ACTION_DISCOVERY_FINISHED");
			Log.i(TAG, "received discovery finished");

			if ((System.currentTimeMillis() - sharedPref.getLong(
					DISCOVERY_FINISHED_TIMESTAMP, 0)) > 10000) {
				setDiscoveryFinishedTimestamp(sharedPref,
						System.currentTimeMillis());
				// addPairedDevices();
				if (sf != null)
					sf.onScanningFinished();
				// compareDevice();
			}

		}

	}

	public static void setDiscoveryFinishedTimestamp(
			SharedPreferences sharedPref, Long time) {
		SharedPreferences.Editor edit = sharedPref.edit();
		edit.putLong(DevicesReceiver.DISCOVERY_FINISHED_TIMESTAMP,
				System.currentTimeMillis());
		edit.commit();
	}

	private void compareDevice() {
		Bundle scanInfo = getScanInfo();
		ArrayList<String> oldDeviceList = scanInfo
				.getStringArrayList(DEVICE_LIST);
		float oldProb = scanInfo.getFloat(SCAN_PROBABILITY);
		int newDevice = 0;
		int oldDevice = 0;
		for (String device : newDeviceList) {
			if (oldDeviceList.indexOf(device) != -1) {
				oldDevice++;
			} else {
				newDevice++;
			}
		}
		float dp = 0;

		if (oldDevice > 0 || newDevice > 0) {
			if (oldDeviceList.size() == 0) {
				dp = oldProb;
			} else {
				dp = (float) oldProb
						* (float) 0.5
						* ((float) newDevice / (float) (newDeviceList.size()) + (1 - (float) oldDevice
								/ (float) (oldDeviceList.size())));
			}

			Log.i(TAG, "delta p is:" + String.valueOf(dp));
			oldProb = Math.min(oldProb + dp, MAX_PROB);

		} else {
			oldProb = Math.max((float) oldProb / (float) 2, MIN_PROB);
		}
		setScanInfo(oldProb, newDeviceList);
	}

	public void setListener(ScanningFinished sf) {
		this.sf = sf;
	}

	// set scan info(device list and probability) to current scan
	private void setScanInfo(float probability, ArrayList<String> deviceList) {

		Log.i(TAG, "set scan probability to:" + String.valueOf(probability));

		SharedPreferences.Editor prefEditor = sharedPref.edit();
		prefEditor.putFloat(SCAN_PROBABILITY, probability);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < deviceList.size(); i++) {
			if (i == 0) {
				sb.append(deviceList.get(i));
			} else {
				sb.append(",");
				sb.append(deviceList.get(i));
			}

		}
		Log.i(TAG, "set device list to:" + sb.toString());
		prefEditor.putString(DEVICE_LIST, sb.toString());
		prefEditor.commit();
	}

	// get scan info(device list and probability) of last scan
	public Bundle getScanInfo() {

		float probability = sharedPref.getFloat(SCAN_PROBABILITY, INIT_PROB);
		String[] tmpList = sharedPref.getString(DEVICE_LIST, "").split(",");

		ArrayList<String> deviceList = new ArrayList<String>();
		for (String device : tmpList) {
			deviceList.add(device);
		}
		Log.i(TAG, "current scan probability is:" + String.valueOf(probability));
		Log.i(TAG,
				"devices scanned during last time are:" + deviceList.toString());
		Bundle mBundle = new Bundle();
		mBundle.putFloat(SCAN_PROBABILITY, probability);
		mBundle.putStringArrayList(DEVICE_LIST, deviceList);
		return mBundle;
	}

	// initialize device list for a new scan
	public void initDeivceList() {

		newDeviceList = new ArrayList<String>();
	}

}
