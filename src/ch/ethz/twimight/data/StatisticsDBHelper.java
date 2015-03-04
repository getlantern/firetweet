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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.util.Log;
import ch.ethz.twimight.activities.PrefsActivity;
import ch.ethz.twimight.net.twitter.TwitterUsers;

/**
 * Manages the Location table in the DB
 * @author thossmann
 *
 */
public class StatisticsDBHelper {
	

	
	// Database fields
	public static final String KEY_LOCATION_ID = "_id";
	public static final String KEY_TIMESTAMP = "timestamp";
	public static final String KEY_LOCATION_LNG = "lng";
	public static final String KEY_LOCATION_LAT = "lat";
	public static final String KEY_LOCATION_ACCURACY = "accuracy";
	public static final String KEY_LOCATION_DATE = "loc_date";
	public static final String KEY_LOCATION_PROVIDER = "provider";
	public static final String KEY_NETWORK = "network";
	public static final String KEY_EVENT = "event";
	public static final String KEY_LINK = "link";
	public static final String KEY_ISDISASTER = "is_disaster";


	private SQLiteDatabase database;
	private DBOpenHelper dbHelper;
	private Context context;

	//EVENTS
	public static final String APP_STARTED = "app_started";
	public static final String APP_CLOSED = "app_closed";
	public static final String LINK_CLICKED = "link_clicked";
	public static final String TWEET_WRITTEN = "tweet_written";



	 public StatisticsDBHelper(Context context) {
		   this.context =  context;
	   }
		/**
		 * Opens the DB.
		 * @return
		 * @throws SQLException
		 */
		public StatisticsDBHelper open() throws SQLException {
			dbHelper = DBOpenHelper.getInstance(context);
			database = dbHelper.getWritableDatabase();
			return this;
		}

		/**
		 * We don't close the DB since there is only one instance!
		 */
		public void close() {
			
		}
		
		/**
		 * Inserts a location update into the DB
		 * @param loc
		 * @return
		 */
		public boolean insertRow(Location loc, String network, String event, String link, Long timestamp ) {
			
			ContentValues update;
			boolean isDisaster =  PrefsActivity.isDisModeActive(context);
			
			if(loc != null){
				
				 update = createContentValues(loc.getLatitude(), loc.getLongitude(), loc.getAccuracy(), loc.getTime(),
						loc.getProvider(), network, event, link, timestamp, isDisaster);		
			} else 
				 update = createContentValues(null, null, null, timestamp,
						null, network, event, link, timestamp, isDisaster);
			
		
			
			try{
				database.insert(DBOpenHelper.TABLE_STATISTICS, null, update);
				return true;
				
			} catch (SQLiteException e) {
				return false;
			}
			
		}
	
	public int deleteOldData() {
		
		return database.delete(DBOpenHelper.TABLE_STATISTICS, null, null);
	}
	
	public Cursor getData() {
		
		Cursor cursor = database.query(DBOpenHelper.TABLE_STATISTICS, null, null, null, null,null,null);
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();			
		}
			
		else 
			return null;
		
		
		return cursor;
	}
	
	public long getFollowersCount()  {
		
		Cursor cursor = database.query(DBOpenHelper.TABLE_USERS, null, TwitterUsers.COL_ISFOLLOWER + "= 1", null,null,null,null);
		if (cursor != null){
			cursor.moveToFirst();
		
			return cursor.getCount();	
		}
		return 0;
	}
	
	/**
	 * Creates an ArrayList of locations since Date d
	 * @param d Date
	 * @return ArrayList of locations
	 */
	public List<Location> getLocationsSince(Date d){
		ArrayList<Location> locationList = new ArrayList<Location>();
		
		Cursor mCursor = database.query(true, DBOpenHelper.TABLE_STATISTICS, new String[] {
				KEY_LOCATION_LNG, KEY_LOCATION_LAT, KEY_LOCATION_ACCURACY, KEY_LOCATION_PROVIDER, KEY_LOCATION_DATE},
				KEY_LOCATION_DATE + ">=" + Long.toString(d.getTime()), null, null, null, null, null);
		
		if(mCursor != null){
			mCursor.moveToFirst();
			
	
			while (mCursor.isAfterLast() == false) {
	        
				Location tmp = new Location(mCursor.getString(mCursor.getColumnIndex(StatisticsDBHelper.KEY_LOCATION_PROVIDER)));
				tmp.setLatitude(mCursor.getFloat(mCursor.getColumnIndex(StatisticsDBHelper.KEY_LOCATION_LAT)));
				tmp.setLongitude(mCursor.getFloat(mCursor.getColumnIndex(StatisticsDBHelper.KEY_LOCATION_LNG)));
				tmp.setAccuracy(mCursor.getFloat(mCursor.getColumnIndex(StatisticsDBHelper.KEY_LOCATION_ACCURACY)));
				tmp.setTime(mCursor.getLong(mCursor.getColumnIndex(StatisticsDBHelper.KEY_LOCATION_DATE)));
				
				locationList.add(tmp);
	       	    mCursor.moveToNext();
	        }
	        mCursor.close();
			
		}
		
		return locationList;
	}
	

	/**
	 * Creates a Location record to insert in the DB
	 * @return
	 */
	private ContentValues createContentValues(Double lat, Double lng, Float accuracy, long locDate, String provider,
			 String network, String event, String link, long timestamp, boolean isDisaster) {
		
		ContentValues values = new ContentValues();
		
		if (lat != null && lng != null) {
			values.put(KEY_LOCATION_LAT, lat);
			values.put(KEY_LOCATION_LNG, lng);
			values.put(KEY_LOCATION_ACCURACY, (int) Math.round(accuracy));
			values.put(KEY_LOCATION_PROVIDER, provider);
		}
		
		if (isDisaster) 
			values.put(KEY_ISDISASTER, 1);
		
		values.put(KEY_LOCATION_DATE, locDate);		
		values.put(KEY_TIMESTAMP, timestamp);
		values.put(KEY_NETWORK, network);
		values.put(KEY_EVENT, event);
		values.put(KEY_LINK, link);		
		
		return values;
	}
}
