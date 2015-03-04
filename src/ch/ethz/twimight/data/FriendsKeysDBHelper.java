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

package ch.ethz.twimight.data;

import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import ch.ethz.twimight.net.tds.TDSPublicKey;

/**
 * Manages the FriendsKeys table in the DB.
 * @author thossmann
 *
 */
public class FriendsKeysDBHelper {

	// Database columns
	public static final String KEY_FRIENDS_KEY_ID = "_id";
	public static final String KEY_FRIENDS_KEY_TWITTER_ID = "twitter_id";
	public static final String KEY_FRIENDS_KEY = "key";
	
	// Shared preferences
	private static final String TDS_LAST_FRIENDS_KEYS_UPDATE = "tds_last_friends_keys_update";

	
	private Context context;
	private DBOpenHelper dbHelper;
	private SQLiteDatabase database;
	
	public FriendsKeysDBHelper(Context context){
		this.context = context;
	}
	
	/**
	 * Opens the DB.
	 * @return
	 * @throws SQLException
	 */
	public FriendsKeysDBHelper open() throws SQLException {
		dbHelper = DBOpenHelper.getInstance(context);
		database = dbHelper.getWritableDatabase();
		return this;
	}
	
	/**
	 * Get's the time (seconds since 1970) of the last update
	 * @return
	 */
	public long getLastUpdate(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getInt(TDS_LAST_FRIENDS_KEYS_UPDATE, 0);
	}
	
	/**
	 * Stores the number of seconds since 1970 of the last update
	 * @param version
	 */
	public void setLastUpdate(long secs){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putLong(TDS_LAST_FRIENDS_KEYS_UPDATE, secs);
		prefEditor.commit();

	}
	
	/**
	 * Deletes all entries from DB
	 */
	public void flushKeyList(){
		database.delete(DBOpenHelper.TABLE_FRIENDS_KEYS, null, null);
		setLastUpdate(0);
	}
	
	
	/**
	 * Do we have a public key of a given Twitter user?
	 * @param twitterID
	 * @return
	 */
	public boolean hasKey(long twitterID){
		Cursor c = database.query(DBOpenHelper.TABLE_FRIENDS_KEYS, null, KEY_FRIENDS_KEY_TWITTER_ID + "=" + twitterID, null, null, null, null);
		boolean key = false;
		if(c.getCount() > 0){
			key = true;
		} 
		c.close();
		return key;
	}
	
	/**
	 * gives the public key for the specified twitter user
	 * @param screenName
	 * @return
	 * @author pcarta
	 */
	public String getKey(long twitterID){
		

		Cursor c = database.query(DBOpenHelper.TABLE_FRIENDS_KEYS, null, KEY_FRIENDS_KEY_TWITTER_ID + "=" + twitterID, null, null, null, null);

		if(c.getCount() > 0){
			c.moveToFirst();
			String key = c.getString(c.getColumnIndex("key"));
			c.close();
			return key;
		} 


		return null;
		
	}
	
	
	/**
	 * Insert a key into DB. If we already have a key, we delete the old one.
	 * @param entry
	 * @return
	 */
	public void insertKey(TDSPublicKey key){
		// insert statement
		if(!hasKey(key.getTwitterID())){
			deleteKey(key.getTwitterID());
		}
		database.execSQL("INSERT OR IGNORE INTO "+DBOpenHelper.TABLE_FRIENDS_KEYS+" (" +KEY_FRIENDS_KEY_TWITTER_ID+ "," +KEY_FRIENDS_KEY+ ") VALUES ('" + key.getTwitterID() + "','" + key.getPemKey() + "')");
	}
	
	/**
	 * Deletes a key of a given twitter ID
	 */
	public void deleteKey(long twitterID){
		database.delete(DBOpenHelper.TABLE_FRIENDS_KEYS, KEY_FRIENDS_KEY_TWITTER_ID+"="+twitterID, null);
	}

	/**
	 * Revokes all entries of a list of revocation list entries
	 * @param revocationList
	 */
	public void insertKeys(List<TDSPublicKey> keyList) {
		// insert new macs in the DB
		Iterator<TDSPublicKey> iterator = keyList.iterator(); 
		while(iterator.hasNext()) {
		    TDSPublicKey key = iterator.next();
		    insertKey(key);
		}

	}
	
}
