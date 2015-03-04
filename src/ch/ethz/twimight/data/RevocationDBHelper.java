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
import ch.ethz.twimight.security.RevocationListEntry;

/**
 * Manages the revocation list in the DB.
 * @author thossmann
 *
 */
public class RevocationDBHelper {

	// Database columns
	public static final String KEY_REVOCATION_ID = "_id";
	public static final String KEY_REVOCATION_SERIAL = "serial";
	public static final String KEY_REVOCATION_UNTIL = "until";
	
	// Shared preferences
	private static final String TDS_REVOCATION_VERSION = "tds_revocation_version";

	
	private Context context;
	private DBOpenHelper dbHelper;
	private SQLiteDatabase database;
	
	public RevocationDBHelper(Context context){
		this.context = context;
	}
	
	/**
	 * Opens the DB.
	 * @return
	 * @throws SQLException
	 */
	public RevocationDBHelper open() throws SQLException {
		dbHelper = DBOpenHelper.getInstance(context);
		database = dbHelper.getWritableDatabase();
		return this;
	}
	
	/**
	 * Get's the current version of the revocation list
	 * @return
	 */
	public int getCurrentVersion(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getInt(TDS_REVOCATION_VERSION, 0);
	}
	
	/**
	 * Stores the current version (as obtained from the TDS) to shared preferences
	 * @param version
	 */
	public void setCurrentVersion(int version){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putInt(TDS_REVOCATION_VERSION, version);
		prefEditor.commit();

	}
	
	/**
	 * Deletes all entries from DB
	 */
	public void flushRevocationList(){
		database.delete(DBOpenHelper.TABLE_REVOCATIONS, null, null);
		setCurrentVersion(0);
	}
	
	/**
	 * Insert an entry into DB
	 * @param entry
	 * @return
	 */
	public void revoke(RevocationListEntry entry){
		// We have to convert from milliseconds since 1970 to seconds since 1970
		Long untilSeconds = (long) Math.round(entry.getUntil().getTime()/1000);
		// insert statement
		database.execSQL("INSERT OR IGNORE INTO "+DBOpenHelper.TABLE_REVOCATIONS+" (" +KEY_REVOCATION_SERIAL+ "," +KEY_REVOCATION_UNTIL+ ") VALUES ('" + entry.getSerial() + "'," + untilSeconds + ")");
	}
	
	/**
	 * Deletes all entries which are expired.
	 */
	public void deleteExpired(){
		Long until = (long) Math.round(System.currentTimeMillis()/1000);
		database.delete(DBOpenHelper.TABLE_REVOCATIONS, "until<" + until, null);
	}
	
	/**
	 * Returns true if the certificate serial number is in the database.
	 * @param serial
	 * @return
	 */
	public boolean isRevoked(String serial){
		Cursor c = database.query(DBOpenHelper.TABLE_REVOCATIONS, null, KEY_REVOCATION_SERIAL + "='" + serial +"'", null, null, null, null);
		boolean revoked = false;
		if(c.getCount() > 0){
			revoked = true;
		} 
		c.close();
		return revoked;
	}

	/**
	 * Revokes all entries of a list of revocation list entries
	 * @param revocationList
	 */
	public void processUpdate(List<RevocationListEntry> revocationList) {
		// insert new macs in the DB
		Iterator<RevocationListEntry> iterator = revocationList.iterator(); 
		while(iterator.hasNext()) {
		    RevocationListEntry entry = iterator.next();
		    revoke(entry);
		}

	}
	
}
