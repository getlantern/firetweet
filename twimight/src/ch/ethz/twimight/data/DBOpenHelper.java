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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import ch.ethz.twimight.net.Html.HtmlPage;
import ch.ethz.twimight.net.twitter.DirectMessages;
import ch.ethz.twimight.net.twitter.Tweets;
import ch.ethz.twimight.net.twitter.TwitterUsers;

/**
 * Manages the database: creation/deletion of tables, opening the connection, etc.
 * @author theus
 *
 */
public class DBOpenHelper extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "twimight";

	// Database table names;
	static final String TABLE_REVOCATIONS = "revocations"; /** the table holding the local version of the revocation list */
	static final String TABLE_MACS = "macs"; /** table holding the bluetooth MAC addresses we know */	
	static final String TABLE_FRIENDS_KEYS = "friends_keys";
	public static final String TABLE_TWEETS = "tweets"; 	
	public static final String TABLE_USERS = "users";
	public static final String TABLE_DMS = "dms";
	public static final String TABLE_HTML = "htmls";
	static final String TABLE_STATISTICS = "statistics";
	//public static final String TABLE_HTML_TRACKERS = "html_trackers";
	
	public static final String ROW_ID = "_id";

	private static final int DATABASE_VERSION = 54;

	// Database creation sql statement
	private static final String TABLE_MACS_CREATE = "create table "+TABLE_MACS+" ("
			+ ROW_ID + " integer primary key autoincrement not null, "
			+ MacsDBHelper.KEY_MAC + " string, "
			+ MacsDBHelper.KEY_ATTEMPTS+ " integer, "
			+ MacsDBHelper.KEY_SUCCESSFUL +" integer, "
			+ MacsDBHelper.KEY_ACTIVE + " integer, "
			+ MacsDBHelper.KEY_LAST +" integer);";
	
	private static final String TABLE_STATISTICS_CREATE = "create table "+TABLE_STATISTICS+" ("
			+ ROW_ID + " integer primary key autoincrement not null, "			
			+ StatisticsDBHelper.KEY_TIMESTAMP + " bigint not null, "
			+ StatisticsDBHelper.KEY_LOCATION_LAT + " real, "
			+ StatisticsDBHelper.KEY_LOCATION_LNG + " real, "
			+ StatisticsDBHelper.KEY_LOCATION_ACCURACY + " integer, "
			+ StatisticsDBHelper.KEY_LOCATION_DATE + " integer, "
			+ StatisticsDBHelper.KEY_LOCATION_PROVIDER +" string, "
			+ StatisticsDBHelper.KEY_NETWORK + " string, "
			+ StatisticsDBHelper.KEY_EVENT + " string, "
			+ StatisticsDBHelper.KEY_ISDISASTER+ " integer default 0, "
			+ StatisticsDBHelper.KEY_LINK + " string);";
	

	private static final String TABLE_REVOCATION_CREATE = "create table "+TABLE_REVOCATIONS+" ("
			+ ROW_ID + " integer primary key autoincrement not null, "
			+ "serial string not null, "
			+ "until integer not null);";
	
		

	private static final String TABLE_FRIENDS_KEYS_CREATE = "create table "+TABLE_FRIENDS_KEYS+" ("
			+ ROW_ID + " integer primary key autoincrement not null, "
			+ "twitter_id bigint not null, "
			+ "key text not null);";
	
	// Tweets (including disaster tweets)
	private static final String TABLE_TWEETS_CREATE = "create table "+TABLE_TWEETS+" ("
			+ ROW_ID + " integer primary key autoincrement not null, "
			+ Tweets.COL_TEXT + " string not null, "
			+ Tweets.COL_TEXT_PLAIN + " string not null, "
			+ Tweets.COL_TWITTERUSER + " bigint, "			
			+ Tweets.COL_SCREENNAME+ " string, "
			+ Tweets.COL_TID + " bigint unique, "
			+ Tweets.COL_REPLYTO + " bigint, "			
			+ Tweets.COL_RETWEETED + " int, "
			+ Tweets.COL_RETWEETCOUNT + " int, "		
			+ Tweets.COL_RETWEETED_BY + " string, "
			+ Tweets.COL_MENTIONS + " int, "
			+ Tweets.COL_LAT + " real, "
			+ Tweets.COL_LNG + " real, "
			+ Tweets.COL_CREATED + " integer, "
			+ Tweets.COL_RECEIVED + " integer, "
			+ Tweets.COL_SOURCE + " string, "
			+ Tweets.COL_FLAGS + " integer default 0, "
			+ Tweets.COL_BUFFER + " integer default 0, "			
			+ Tweets.COL_MEDIA + " string, "
			+ Tweets.COL_HTML_PAGES + " integer default 0, "		
			+ Tweets.COL_DISASTERID + " integer, "
			+ Tweets.COL_ISVERIFIED + " integer, "
			+ Tweets.COL_SIGNATURE + " string, "		
			+ Tweets.COL_CERTIFICATE + " string);";

	// Twitter Users
	private static final String TABLE_USERS_CREATE = "create table "+TABLE_USERS+" ("
			+ ROW_ID + " integer primary key autoincrement not null, "
			+ TwitterUsers.COL_SCREENNAME + " string not null, "
			+ TwitterUsers.COL_TWITTERUSER_ID + " bigint unique, "
			+ TwitterUsers.COL_NAME + " string, "
			+ TwitterUsers.COL_LANG + " string, "
			+ TwitterUsers.COL_DESCRIPTION + " string, "
			+ TwitterUsers.COL_IMAGEURL + " string, "
			+ TwitterUsers.COL_STATUSES + " integer, "
			+ TwitterUsers.COL_FOLLOWERS + " integer, "
			+ TwitterUsers.COL_FRIENDS + " integer, "
			+ TwitterUsers.COL_LISTED + " integer, "
			+ TwitterUsers.COL_FAVORITES + " integer, "
			+ TwitterUsers.COL_LOCATION + " string, "
			+ TwitterUsers.COL_UTCOFFSET + " string, "
			+ TwitterUsers.COL_TIMEZONE + " string, "
			+ TwitterUsers.COL_URL + " string, "
			+ TwitterUsers.COL_CREATED + " integer, "
			+ TwitterUsers.COL_PROTECTED + " integer, "
			+ TwitterUsers.COL_VERIFIED + " integer, "
			+ TwitterUsers.COL_ISFOLLOWER + " integer, "
			+ TwitterUsers.COL_ISFRIEND + " integer, "
			+ TwitterUsers.COL_ISDISASTER_PEER + " integer default 0, "
			+ TwitterUsers.COL_IS_SEARCH_RESULT + " integer default 0, "			
			+ TwitterUsers.COL_FOLLOWREQUEST + " integer, "
			+ TwitterUsers.COL_PROFILEIMAGE_PATH + " string, "
			+ TwitterUsers.COL_LASTUPDATE + " integer, "
			+ TwitterUsers.COL_LAST_PICTURE_UPDATE + " integer, "
			+ TwitterUsers.COL_FLAGS + " integer default 0);";

	// Direct Messages (including disaster messages)
	private static final String TABLE_DMS_CREATE = "create table "+TABLE_DMS+" ("
			+ ROW_ID + " integer primary key autoincrement not null, "
			+ DirectMessages.COL_TEXT + " string, "
			+ DirectMessages.COL_SENDER + " bigint, "
			+ DirectMessages.COL_RECEIVER + " bigint, "
			+ DirectMessages.COL_RECEIVER_SCREENNAME + " string, "
			+ DirectMessages.COL_DMID + " bigint unique, "
			+ DirectMessages.COL_CREATED + " integer, "
			+ DirectMessages.COL_RECEIVED + " integer, "
			+ DirectMessages.COL_FLAGS + " integer default 0, "
			+ DirectMessages.COL_BUFFER + " integer default 0, "
			+ DirectMessages.COL_ISDISASTER + " integer default 0, "
			+ DirectMessages.COL_DISASTERID + " integer, "
			+ DirectMessages.COL_ISVERIFIED + " integer, "
			+ DirectMessages.COL_SIGNATURE + " string, "
			+ DirectMessages.COL_CRYPTEXT + " string, "
			+ DirectMessages.COL_CERTIFICATE + " string);";
	
	// html pages 
	private static final String TABLE_HTML_CREATE = "create table "+TABLE_HTML+" ("
			+ HtmlPage.COL_PAGE_ID + " integer primary key autoincrement not null, "
			+ HtmlPage.COL_URL + " string unique not null, "	
			+ HtmlPage.COL_DISASTERID + " bigint REFERENCES " + TABLE_TWEETS + "(" + Tweets.COL_DISASTERID +") ON DELETE CASCADE, "			
			+ HtmlPage.COL_FORCED + " integer default 0, "
			+ HtmlPage.COL_ATTEMPTS + " integer default 0, "
			+ HtmlPage.COL_FILENAME + " string unique);";
	
	/*
	// html pages trackers
		private static final String TABLE_HTML_TRACKERS_CREATE = "create table "+TABLE_HTML_TRACKERS+" ("
				+ HtmlPage.COL_PAGE_ID + " integer not null REFERENCES " + TABLE_HTML + "(" + HtmlPage.COL_PAGE_ID +") ON DELETE CASCADE, " 			
				+ HtmlPage.COL_TID + " bigint not null REFERENCES " + TABLE_TWEETS + "(" + HtmlPage.COL_TID +") ON DELETE CASCADE, " 
				+ "PRIMARY KEY (" + HtmlPage.COL_PAGE_ID + "," + HtmlPage.COL_TID + ")"
			   	+ ");";
	*/
	
	private static DBOpenHelper dbHelper; /** the one and only instance of this class */
	private static SQLiteDatabase myWritableDb;
	
	/**
	 * Constructorcontent://
	 * @param context
	 */
	private DBOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/**
	 * We want only one instance of a DBHelper to avoid problems.
	 * @param context
	 * @return the 
	 */
	public static synchronized DBOpenHelper getInstance(Context context){
		if(dbHelper == null)
			dbHelper = new DBOpenHelper(context);		
		return dbHelper;
		
	}
	
    /**
     * Returns a writable database instance in order not to open and close many
     * SQLiteDatabase objects simultaneously
     *
     * @return a writable instance to SQLiteDatabase
     */
    @Override
	public SQLiteDatabase getWritableDatabase() {
    	// TODO Auto-generated method stub
    	synchronized(DBOpenHelper.class) {
    		if ((myWritableDb == null) || (!myWritableDb.isOpen())) {
    			return super.getWritableDatabase();
    		}
    	}
    	return myWritableDb;
	}

	@Override
    public void close() {
        super.close();
        synchronized(DBOpenHelper.class) {
        	if (myWritableDb != null) {
                myWritableDb.close();
                myWritableDb = null;
            }
        }
        
    }
	
	private void createTables(SQLiteDatabase database) {
		database.execSQL(TABLE_MACS_CREATE);
		database.execSQL(TABLE_STATISTICS_CREATE);
		database.execSQL(TABLE_REVOCATION_CREATE);
		database.execSQL(TABLE_FRIENDS_KEYS_CREATE);
		database.execSQL(TABLE_TWEETS_CREATE);
		database.execSQL(TABLE_USERS_CREATE);
		database.execSQL(TABLE_DMS_CREATE);
		database.execSQL(TABLE_HTML_CREATE);
		//database.execSQL(TABLE_HTML_TRACKERS_CREATE);

	
	}
	
	/**
	 * Called when creating the DB
	 */
	@Override
	public void onCreate(SQLiteDatabase database) {
		createTables(database);
	}

	/**
	 * Called when upgrading the DB (new DATABASE_VERSION)
	 */
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.w(DBOpenHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS "+TABLE_MACS);
		database.execSQL("DROP TABLE IF EXISTS "+TABLE_STATISTICS);
		database.execSQL("DROP TABLE IF EXISTS "+TABLE_REVOCATIONS);
		database.execSQL("DROP TABLE IF EXISTS "+TABLE_FRIENDS_KEYS);
		database.execSQL("DROP TABLE IF EXISTS "+TABLE_TWEETS);
		database.execSQL("DROP TABLE IF EXISTS "+TABLE_USERS);
		database.execSQL("DROP TABLE IF EXISTS "+TABLE_DMS);
		database.execSQL("DROP TABLE IF EXISTS "+TABLE_HTML);
		//database.execSQL("DROP TABLE IF EXISTS "+TABLE_HTML_TRACKERS);

		createTables(database);
	}
	
	/**
	 * Empties all tables;
	 */
	public void flushDB(){
		
		SQLiteDatabase database = this.getWritableDatabase();
		database.execSQL("DELETE FROM "+TABLE_MACS);
		database.execSQL("DELETE FROM "+TABLE_STATISTICS);
		database.execSQL("DELETE FROM "+TABLE_REVOCATIONS);
		database.execSQL("DELETE FROM "+TABLE_FRIENDS_KEYS);
		database.execSQL("DELETE FROM "+TABLE_TWEETS);
		database.execSQL("DELETE FROM "+TABLE_USERS);
		database.execSQL("DELETE FROM "+TABLE_DMS);
		database.execSQL("DELETE FROM "+TABLE_HTML);
		//database.execSQL("DELETE FROM "+TABLE_HTML_TRACKERS);

	}
}
