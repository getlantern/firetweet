package ch.ethz.twimight.net.opportunistic;

import java.util.Observable;

public class BluetoothStatus extends Observable {

	private static final BluetoothStatus INSTANCE = new BluetoothStatus();
	
	private String mStatusDescription;
	private int mNeighborCount;
	
	/**
	 * Singleton.
	 */
	private BluetoothStatus(){
		super();
		mStatusDescription = "";
		mNeighborCount = 0;
	}
	
	public static BluetoothStatus getInstance(){
		return INSTANCE;
	}
	
	public String getStatusDescription(){
		return mStatusDescription;
	}
	
	public int getNeighborCount(){
		return mNeighborCount;
	}
	
	public void setStatusDescription(String statusDescription) {
		mStatusDescription = statusDescription;
		setChanged();
		notifyObservers();
	}
	
	public void setNeighborCount(int neighborCount) {
		mNeighborCount = neighborCount;
		setChanged();
		notifyObservers();
	}
}

