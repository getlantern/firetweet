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

package ch.ethz.twimight.net.twitter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import ch.ethz.twimight.R;
import ch.ethz.twimight.activities.LoginActivity;
import ch.ethz.twimight.activities.ShowDMListActivity;
import ch.ethz.twimight.activities.ShowDMUsersListActivity;
import ch.ethz.twimight.data.DBOpenHelper;
import ch.ethz.twimight.security.CertificateManager;
import ch.ethz.twimight.security.KeyManager;
import ch.ethz.twimight.util.Constants;

/**
 * The content provider for all kinds of direct messages (normal, disaster, local and relayed). 
 * The URIs and column names are defined in class DirectMessages.
 * @author thossmann
 *
 */
public class DirectMessagesContentProvider extends ContentProvider {

	private static final String TAG = "DirectMessagesContentProvider";

	
	private SQLiteDatabase database;
	private DBOpenHelper dbHelper;
	
	private static UriMatcher dmUriMatcher;
		
	private static final int DMS = 1;
	private static final int DM_ID = 2;
	
	private static final int LIST_ALL = 3;
	private static final int LIST_NORMAL = 4;
	private static final int LIST_DISASTER = 5;
	
	private static final int USERS = 6;
	
	private static final int USER = 7;
	
		
	// Here we define all the URIs this provider knows
	static{
		dmUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		dmUriMatcher.addURI(DirectMessages.DM_AUTHORITY, DirectMessages.DMS, DMS);
		
		dmUriMatcher.addURI(DirectMessages.DM_AUTHORITY, DirectMessages.DMS + "/#", DM_ID);
		
		dmUriMatcher.addURI(DirectMessages.DM_AUTHORITY, DirectMessages.DMS + "/" + DirectMessages.DMS_USERS, USERS);
		
		dmUriMatcher.addURI(DirectMessages.DM_AUTHORITY, DirectMessages.DMS + "/" + DirectMessages.DMS_USER + "/#", USER);
		
		dmUriMatcher.addURI(DirectMessages.DM_AUTHORITY, DirectMessages.DMS + "/" + DirectMessages.DMS_LIST + "/" + DirectMessages.DMS_SOURCE_ALL, LIST_ALL);
		dmUriMatcher.addURI(DirectMessages.DM_AUTHORITY, DirectMessages.DMS + "/" + DirectMessages.DMS_LIST + "/" + DirectMessages.DMS_SOURCE_NORMAL, LIST_NORMAL);
		dmUriMatcher.addURI(DirectMessages.DM_AUTHORITY, DirectMessages.DMS + "/" + DirectMessages.DMS_LIST + "/" + DirectMessages.DMS_SOURCE_DISASTER, LIST_DISASTER);
		
	}
	
	// for the status bar notification
	private static final int DM_NOTIFICATION_ID = 2;
	
	private static final int NOTIFY_DM = 3;
	private static final int NOTIFY_DISASTER_DM = 4;
	
	/**
	 * onCreate we initialize and open the DB.
	 */
	@Override
	public boolean onCreate() {
		dbHelper = DBOpenHelper.getInstance(getContext().getApplicationContext());
		database = dbHelper.getWritableDatabase();
		return true;
	}

	/**
	 * Returns the MIME types (defined in Tweets) of a URI
	 */
	@Override
	public String getType(Uri uri) {
		switch(dmUriMatcher.match(uri)){
			case DMS: return DirectMessages.DMS_CONTENT_TYPE;
		
			case DM_ID: return DirectMessages.DM_CONTENT_TYPE;
			
			case USER: return DirectMessages.DMS_CONTENT_TYPE;
			
			case USERS: return DirectMessages.DMUSERS_CONTENT_TYPE;
			
			case LIST_ALL: return DirectMessages.DMS_CONTENT_TYPE;
			case LIST_NORMAL: return DirectMessages.DMS_CONTENT_TYPE;
			case LIST_DISASTER: return DirectMessages.DMS_CONTENT_TYPE;
	
			default: throw new IllegalArgumentException("Unknown URI: " + uri);	
		}
	}

	/**
	 * Query the direct messages table
	 * TODO: Create the queries more elegantly..
	 */
	@Override
	public synchronized Cursor query(Uri uri, String[] projection, String where, String[] whereArgs, String sortOrder) {
				
		if(TextUtils.isEmpty(sortOrder)) sortOrder = Tweets.DEFAULT_SORT_ORDER;
		
		Cursor c = null;
		String sql = null;
		Intent i = null;
		switch(dmUriMatcher.match(uri)){
			
			case DMS: 
				Log.d(TAG, "Query DMS");
				c = database.query(DBOpenHelper.TABLE_DMS, projection, where, whereArgs, null, null, sortOrder);
				c.setNotificationUri(getContext().getContentResolver(), DirectMessages.CONTENT_URI);
				break;

			case DM_ID: 
				Log.d(TAG, "Query DM_ID");
				sql = "SELECT  * "
					+ "FROM "+DBOpenHelper.TABLE_DMS + " "
					+ "WHERE " + DBOpenHelper.TABLE_DMS+ "._id=" + uri.getLastPathSegment() + ";";
				c = database.rawQuery(sql, null);
				c.setNotificationUri(getContext().getContentResolver(), uri);
				break;
						
			case USER:
				Log.d(TAG, "Query USER");
				// get the twitter user id
				sql = "SELECT "+ DBOpenHelper.TABLE_USERS+"."+TwitterUsers.COL_TWITTERUSER_ID + " "
				+ "FROM "+ DBOpenHelper.TABLE_USERS+" "
				+ "WHERE _id=" +uri.getLastPathSegment() +";";
				c = database.rawQuery(sql, null);
				
				if(c.getCount()==0) return null; // this should not happen
				c.moveToFirst();
				long userId = c.getLong(c.getColumnIndex(TwitterUsers.COL_TWITTERUSER_ID));
				c.close();
				
				// get the direct messages
				sql = "SELECT " + DBOpenHelper.TABLE_DMS+"._id, "
				+ DBOpenHelper.TABLE_DMS+"."+DirectMessages.COL_TEXT + ", "
				+ DBOpenHelper.TABLE_DMS+"."+DirectMessages.COL_SENDER + ", "
				+ DBOpenHelper.TABLE_DMS+"."+DirectMessages.COL_CREATED + ", "
				+ DBOpenHelper.TABLE_DMS+"."+DirectMessages.COL_ISDISASTER + ", "
				+ DBOpenHelper.TABLE_DMS+"."+DirectMessages.COL_FLAGS + ", "
				+ DBOpenHelper.TABLE_DMS+"."+DirectMessages.COL_DMID+ ", "
				+ DBOpenHelper.TABLE_USERS+"._id AS userRowId, "
				+ DBOpenHelper.TABLE_USERS+"."+TwitterUsers.COL_SCREENNAME + ", "
				+ DBOpenHelper.TABLE_USERS+"."+TwitterUsers.COL_NAME + ", "
				+ DBOpenHelper.TABLE_USERS+"."+TwitterUsers.COL_PROFILEIMAGE_PATH + " "
				+ "FROM " + DBOpenHelper.TABLE_DMS + " "
				+ "JOIN "+DBOpenHelper.TABLE_USERS+" "
				+ "ON "+DBOpenHelper.TABLE_DMS + "." + DirectMessages.COL_SENDER+"="+DBOpenHelper.TABLE_USERS+"."+TwitterUsers.COL_TWITTERUSER_ID+" "				
				+ "WHERE " + DBOpenHelper.TABLE_DMS+"."+DirectMessages.COL_RECEIVER+"="+userId+" "
				+ "OR " + DBOpenHelper.TABLE_DMS+"."+DirectMessages.COL_SENDER+"="+userId+" "
				+ "ORDER BY "+ DBOpenHelper.TABLE_DMS+"."+DirectMessages.COL_CREATED +" DESC "
				+ "LIMIT 100";

				c = database.rawQuery(sql, null);
				// TODO: Correct notification URI
				c.setNotificationUri(getContext().getContentResolver(), DirectMessages.CONTENT_URI);
				
				break;
			case USERS: 
				Log.d(TAG, "Query USERS");
				// selects the last DM of which the given user is either a sender or recepient
				String subQuery1 = "SELECT " + DirectMessages.COL_TEXT + " " 
					+ "FROM " + DBOpenHelper.TABLE_DMS + " "
					+ "WHERE " + DBOpenHelper.TABLE_DMS+"."+DirectMessages.COL_RECEIVER+"="+DBOpenHelper.TABLE_USERS + "." + TwitterUsers.COL_TWITTERUSER_ID+" "
					+ "OR " + DBOpenHelper.TABLE_DMS+"."+DirectMessages.COL_SENDER+"="+DBOpenHelper.TABLE_USERS + "." + TwitterUsers.COL_TWITTERUSER_ID+" "
					+ "ORDER BY "+ DBOpenHelper.TABLE_DMS+"."+DirectMessages.COL_CREATED +" DESC "
					+ "LIMIT 1";
				// selects the timestamp of the last DM of which the given user is either a sender or recepient
				String subQuery2 = "SELECT " + DirectMessages.COL_CREATED + " " 
					+ "FROM " + DBOpenHelper.TABLE_DMS + " "
					+ "WHERE " + DBOpenHelper.TABLE_DMS+"."+DirectMessages.COL_RECEIVER+"="+DBOpenHelper.TABLE_USERS + "." + TwitterUsers.COL_TWITTERUSER_ID+" "
					+ "OR " + DBOpenHelper.TABLE_DMS+"."+DirectMessages.COL_SENDER+"="+DBOpenHelper.TABLE_USERS + "." + TwitterUsers.COL_TWITTERUSER_ID+" "
					+ "ORDER BY "+ DBOpenHelper.TABLE_DMS+"."+DirectMessages.COL_CREATED +" DESC "
					+ "LIMIT 1";

				// selects all users who have sent or received at least one DM
				sql= "SELECT "
					+ DBOpenHelper.TABLE_USERS + "._id, "
					+ DBOpenHelper.TABLE_USERS + "." + TwitterUsers.COL_TWITTERUSER_ID+ ", "
					+ DBOpenHelper.TABLE_USERS + "." + TwitterUsers.COL_SCREENNAME+ ", "
					+ DBOpenHelper.TABLE_USERS + "." + TwitterUsers.COL_NAME+ ", "
					+ DBOpenHelper.TABLE_USERS + "." + TwitterUsers.COL_PROFILEIMAGE_PATH+ ", "
					+ "("+ subQuery1 + ") AS text, "
					+ "("+ subQuery2 + ") AS created "
					+ "FROM " + DBOpenHelper.TABLE_USERS + " "
					+ "LEFT JOIN " + DBOpenHelper.TABLE_DMS + " AS sent ON " + DBOpenHelper.TABLE_USERS + "." +TwitterUsers.COL_TWITTERUSER_ID +"=sent." +DirectMessages.COL_SENDER + " "
					+ "LEFT JOIN " + DBOpenHelper.TABLE_DMS + " AS received ON " + DBOpenHelper.TABLE_USERS + "." +TwitterUsers.COL_TWITTERUSER_ID +"=received." +DirectMessages.COL_RECEIVER + " "
					+ "WHERE "+"(sent."+DirectMessages.COL_TEXT+" IS NOT NULL OR "+"received."+DirectMessages.COL_TEXT+" IS NOT NULL) "
					+ "AND "+DBOpenHelper.TABLE_USERS + "." + TwitterUsers.COL_TWITTERUSER_ID+"<>"+LoginActivity.getTwitterId(getContext())+" "
					+ "GROUP BY " + DBOpenHelper.TABLE_USERS + "." +TwitterUsers.COL_TWITTERUSER_ID + " "
					+ "ORDER BY created DESC LIMIT 100;";
				
					Log.e(TAG, sql);
				
				c = database.rawQuery(sql, null);
				// TODO: Correct notification URI
				c.setNotificationUri(getContext().getContentResolver(), DirectMessages.CONTENT_URI);
				
				// start synch service with a synch DMs request
				i = new Intent(TwitterService.SYNCH_ACTION);
				i.putExtra("synch_request", TwitterService.SYNCH_DMS);
				getContext().startService(i);
				break;
			case LIST_ALL:
				// TODO
				break;

			case LIST_DISASTER:
				
				Log.i(TAG, "Query DMS Disaster");
				c = database.query(DBOpenHelper.TABLE_DMS, projection,DirectMessages.COL_BUFFER + "&" + DirectMessages.BUFFER_MYDISASTER + "!=0"
						+ " OR " + DirectMessages.COL_BUFFER + "&" + DirectMessages.BUFFER_DISASTER_OTHERS + "!=0", whereArgs, null, null, sortOrder);
				c.setNotificationUri(getContext().getContentResolver(), DirectMessages.CONTENT_URI);
				break;

			case LIST_NORMAL:
				// TODO
				break;


			default: throw new IllegalArgumentException("Unsupported URI: " + uri);	
		}
		
		return c;
	}

	/**
	 * Insert a direct message into the DB
	 */
	@Override
	public synchronized Uri insert(Uri uri, ContentValues values) {
		int disasterId;
		Cursor c = null;
		Uri insertUri = null; // the return value;
		
		switch(dmUriMatcher.match(uri)){
			case LIST_NORMAL:
				Log.d(TAG, "Insert LIST_NORMAL");
				/*
				 *  First, we check if we already have a direct messages with the same disaster ID.
				 *  If yes, two cases are possible
				 *  1 If the existing message is a message of our own which was flagged to insert, the insert
				 *    operation may have been successful but the success was not registered locally.
				 *    In this case we update the new message with the new information
				 *  2 It may be a hash function collision (two different messages have the same hash code)
				 *    Probability of this should be small.  
				 */
				disasterId = getDisasterID(values);
				
				c = database.query(DBOpenHelper.TABLE_DMS, null, DirectMessages.COL_DISASTERID+"="+disasterId, null, null, null, null);
				if(c.getCount()>0){

					c.moveToFirst();
					while(!c.isAfterLast()){
						boolean isOurs = Long.toString(c.getLong(c.getColumnIndex(DirectMessages.COL_SENDER))).equals(LoginActivity.getTwitterId(getContext()));
						boolean isForUs = Long.toString(c.getLong(c.getColumnIndex(DirectMessages.COL_RECEIVER))).equals(LoginActivity.getTwitterId(getContext()));
						
						if(isOurs || isForUs) {
							
							// clear the to insert flag
							int flags = c.getInt(c.getColumnIndex(DirectMessages.COL_FLAGS));
							values.put(DirectMessages.COL_FLAGS, flags & (~DirectMessages.FLAG_TO_INSERT));
							
							Uri updateUri = Uri.parse("content://"+DirectMessages.DM_AUTHORITY+"/"+DirectMessages.DMS+"/"+Integer.toString(c.getInt(c.getColumnIndex("_id"))));
							update(updateUri, values, null, null);
							return updateUri;
						}
						c.moveToNext();
					}
					
				}
				c.close();
				// if none of the before was true, this is a proper new tweet which we now insert
				insertUri = insertDM(values);
				// delete everything that now falls out of the buffer
				purgeDMs(values);
				
				break;
				
			case LIST_DISASTER:
				Log.d(TAG, "Insert LIST_DISASTER");
				// in disaster mode, we set the is disaster flag, encrypt and sign the message 
				//and sign the tweet (if we have a certificate for our key pair)
				values.put(DirectMessages.COL_ISDISASTER, 1);
				
				// if we already have a disaster tweet with the same disaster ID, 
				// we discard the new one
				disasterId = getDisasterID(values);
				c = database.query(DBOpenHelper.TABLE_DMS, null, DirectMessages.COL_DISASTERID+"="+disasterId+" AND "+DirectMessages.COL_ISDISASTER+">0", null, null, null, null);
				if(c.getCount()>0){
					c.moveToFirst();
					Uri oldUri = Uri.parse("content://"+DirectMessages.DM_AUTHORITY+"/"+DirectMessages.DMS+"/"+Long.toString(c.getLong(c.getColumnIndex("_id"))));
					c.close();
					return oldUri; 
				}
				
				CertificateManager cm = new CertificateManager(getContext());
				KeyManager km = new KeyManager(getContext().getApplicationContext());
				
				//verify whether I was the author or not
				if(LoginActivity.getTwitterId(getContext()).equals(values.getAsInteger(DirectMessages.COL_SENDER).toString())){
					
					if(cm.hasCertificate()){
						
						// we put the signature
						String text = values.getAsString(DirectMessages.COL_TEXT);
						String userId = LoginActivity.getTwitterId(getContext()).toString();
						
						String signature = km.getSignature(new String(text+userId));
						values.put(DirectMessages.COL_SIGNATURE, signature);
						// and the certificate
						values.put(DirectMessages.COL_CERTIFICATE, cm.getCertificate());
						// and set the is_verified flag to show that the tweet is signed
						values.put(DirectMessages.COL_ISVERIFIED, 1);
						
						Long twitterId = getIdFromScreenName(values.getAsString(DirectMessages.COL_RECEIVER_SCREENNAME));
						
						//TODO: obtain peers public keys and encrypt
						values.put(DirectMessages.COL_CRYPTEXT,text );	
						/*if (twitterId != null) {
							String cipherText = km.encrypt(text,twitterId );
							Log.i(TAG, "ciphertext: " + cipherText );
							if (cipherText != null) {								
								values.put(DirectMessages.COL_CRYPTEXT,cipherText );															
								
							} 
						}	*/					
						
					} else 
						values.put(DirectMessages.COL_ISVERIFIED, 0);					
					
				} else {					
					//Is it for me?
					if (values.getAsLong(DirectMessages.COL_RECEIVER).toString().equals(LoginActivity.getTwitterId(getContext()))){
						
						values.put(DirectMessages.COL_BUFFER, DirectMessages.BUFFER_DISASTER_ME|DirectMessages.BUFFER_MESSAGES);
						//TODO: DECRYPT THE MESSAGE
					    //String plainText = km.decrypt(values.getAsString(DirectMessages.COL_CRYPTEXT));
						String plainText = values.getAsString(DirectMessages.COL_CRYPTEXT);
						//values.remove(DirectMessages.COL_CRYPTEXT);
						values.put(DirectMessages.COL_TEXT, plainText);
						
						// check signature
						String signature = values.getAsString(DirectMessages.COL_SIGNATURE);
						String certificate = values.getAsString(DirectMessages.COL_CERTIFICATE);
						
						String textForSignatureCheck = plainText + values.getAsString(DirectMessages.COL_SENDER);
						
						if(km.checkSignature(cm.parsePem(certificate), signature, textForSignatureCheck)){							
							values.put(DirectMessages.COL_ISVERIFIED, 1);
						} else 
							values.put(DirectMessages.COL_ISVERIFIED, 0);
												
					} else
						values.put(DirectMessages.COL_BUFFER, DirectMessages.BUFFER_DISASTER_OTHERS);
						
					
				}
				c.close();
				insertUri = insertDM(values);				
				//purgeTweets(values);		
				
				break;				
			default: throw new IllegalArgumentException("Unsupported URI: " + uri);	
		}
		
		return insertUri;
	}

	/**
	 * Update a direct message
	 */
	@Override
	public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if(dmUriMatcher.match(uri) != DM_ID) throw new IllegalArgumentException("Unsupported URI: " + uri);

		Log.d(TAG, "Update DM_ID");
		
		int nrRows = database.update(DBOpenHelper.TABLE_DMS, values, "_id="+uri.getLastPathSegment() , null);
		if(nrRows >= 0){
			getContext().getContentResolver().notifyChange(uri, null);
			getContext().getContentResolver().notifyChange(DirectMessages.CONTENT_URI, null);
			
			if(values.containsKey(DirectMessages.COL_FLAGS) && values.getAsInteger(DirectMessages.COL_FLAGS)!=0){

				Intent i = new Intent(TwitterService.SYNCH_ACTION);
				i.putExtra("synch_request", TwitterService.SYNCH_DM);
				i.putExtra("rowId", Long.valueOf(uri.getLastPathSegment()));
				getContext().startService(i);
			}
			
			return nrRows;
		} else {
			throw new IllegalStateException("Could not update direct message " + values);
		}
	}
	
	/**
	 * Delete a local direct message from the DB
	 */
	@Override
	public synchronized int delete(Uri uri, String arg1, String[] arg2) {
		if(dmUriMatcher.match(uri) != DM_ID) throw new IllegalArgumentException("Unsupported URI: " + uri);
		
		Log.d(TAG, "Delete DM_ID");
		
		int nrRows = database.delete(DBOpenHelper.TABLE_DMS, "_id="+uri.getLastPathSegment(), null);
		getContext().getContentResolver().notifyChange(DirectMessages.CONTENT_URI, null);
		return nrRows;
	}
	
	/**
	 * gives the corresponding twitter ID for a screenName
	 * @param screenName
	 * @return 
	 * @author pcarta
	 */
	private Long getIdFromScreenName(String screenName) {
		Cursor c = database.query(DBOpenHelper.TABLE_USERS, null, TwitterUsers.COL_SCREENNAME + "='" + screenName + "'", null, null, null, null);
		if(c.getCount() > 0){
			c.moveToFirst();
			return c.getLong(c.getColumnIndex(TwitterUsers.COL_TWITTERUSER_ID));
		}
		return null;
	}
	
	/**
	 * purges a provided buffer to the provided number of direct messages
	 */
	private void purgeBuffer(int buffer, int size){
		/*
		 * First, we remove the respective flag
		 * Second, we delete all direct messages which have no more buffer flags
		 */
		
		String sqlWhere;
		String sql;
		Cursor c;
		// NOTE: DELETE in android does not allow ORDER BY. Hence, the trick with the _id
		sqlWhere = "("+DirectMessages.COL_BUFFER+"&"+buffer+")!=0";
		sql = "UPDATE " + DBOpenHelper.TABLE_DMS + " "
				+"SET " + DirectMessages.COL_BUFFER +"=("+(~buffer)+"&"+DirectMessages.COL_BUFFER+") "
				+"WHERE "
				+"_id=(SELECT _id FROM "+DBOpenHelper.TABLE_DMS 
				+ " WHERE " + sqlWhere
				+ " ORDER BY "+Tweets.DEFAULT_SORT_ORDER+" "
				+ " LIMIT -1 OFFSET "
				+ size +");";
		c = database.rawQuery(sql, null);
		c.close();
		
		// now delete
		sql = "DELETE FROM "+DBOpenHelper.TABLE_DMS+" WHERE "+DirectMessages.COL_BUFFER+"=0";
		c = database.rawQuery(sql, null);
		c.close();
		
	}
	
	/**
	 * Keeps the direct messages table at acceptable size
	 */
	private void purgeDMs(ContentValues cv){
		
		// in the content values we find which buffer(s) to purge
		int bufferFlags = cv.getAsInteger(DirectMessages.COL_BUFFER);
		
		if((bufferFlags & DirectMessages.BUFFER_MESSAGES) != 0){
			Log.d(TAG, "purging local direct messages buffer");
			purgeBuffer(DirectMessages.BUFFER_MESSAGES, Constants.MESSAGES_BUFFER_SIZE);
		}
			
		if((bufferFlags & DirectMessages.BUFFER_DISASTER_OTHERS) != 0){
			Log.d(TAG, "purging relay direct messages buffer");
			purgeBuffer(DirectMessages.BUFFER_DISASTER_OTHERS, Constants.DISASTERDM_BUFFER_SIZE);
		}
		
		if((bufferFlags & DirectMessages.BUFFER_DISASTER_ME) != 0){
			Log.d(TAG, "purging relay direct messages buffer");
			purgeBuffer(DirectMessages.BUFFER_DISASTER_ME, Constants.DISASTERDM_BUFFER_SIZE);
		}

		if((bufferFlags & DirectMessages.BUFFER_MYDISASTER) != 0){
			Log.d(TAG, "purging local direct messages buffer");
			purgeBuffer(DirectMessages.BUFFER_MYDISASTER, Constants.MYDISASTERDM_BUFFER_SIZE);
		}
		
	}
	
	/**
	 * Computes the java String object hash code (32 bit) as the disaster ID of the message
	 * TODO: For security reasons (to prevent intentional hash collisions), this should be a cryptographic hash function instead of the string hash.
	 * @param cv
	 * @return
	 */
	private int getDisasterID(ContentValues cv){
		String text = cv.getAsString(DirectMessages.COL_TEXT);
		
		String senderId;
		if(!cv.containsKey(DirectMessages.COL_SENDER) || (cv.getAsString(DirectMessages.COL_SENDER)==null)){
			senderId = LoginActivity.getTwitterId(getContext()).toString();
		} else {
			senderId = cv.getAsString(DirectMessages.COL_SENDER);
		}
		
		return (new String(text+senderId)).hashCode();
	}
	
	/**
	 * Input verification for new direct messages
	 * @param values
	 * @return
	 */
	private boolean checkValues(ContentValues values){
		// TODO: Input validation
		return true;
	}
	
	  private boolean hasBeenExecuted() {
            return PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(TwitterService.TASK_DIRECT_MESSAGES_IN, false);
    }
	
	/**
	 * Inserts a direct message into the DB
	 */
	private Uri insertDM(ContentValues values){
		if(checkValues(values)){
			
			int flags = 0;
			if(values.containsKey(DirectMessages.COL_FLAGS))
				flags = values.getAsInteger(DirectMessages.COL_FLAGS);

						
			if(!values.containsKey(DirectMessages.COL_CREATED)){
				// set the current timestamp
				values.put(DirectMessages.COL_CREATED, System.currentTimeMillis());
			}
			
			values.put(DirectMessages.COL_RECEIVED, System.currentTimeMillis());
			
			// if we don't obtain the disaster ID, we compute it now
			if(!values.containsKey(DirectMessages.COL_DISASTERID)){
				values.put(DirectMessages.COL_DISASTERID, getDisasterID(values));
			}
			
			// for our own new messages, we have a receiver screenname but no ID.
			// Check if we have a corresponding ID in the TwitterUsers
			if(!values.containsKey(DirectMessages.COL_RECEIVER) && values.containsKey(DirectMessages.COL_RECEIVER_SCREENNAME)){
				String [] projection = {TwitterUsers.COL_TWITTERUSER_ID};
				String where = TwitterUsers.COL_SCREENNAME+"='"+values.getAsString(DirectMessages.COL_RECEIVER_SCREENNAME)+"'";
				Cursor c = getContext().getContentResolver().query(Uri.parse("content://" + TwitterUsers.TWITTERUSERS_AUTHORITY + "/" + TwitterUsers.TWITTERUSERS), projection, where, null, null);
				if(c.getCount()>0){
					c.moveToFirst();
					values.put(DirectMessages.COL_RECEIVER, c.getLong(c.getColumnIndex(TwitterUsers.COL_TWITTERUSER_ID)));
				}
				c.close();
			}
						
			
			long rowId = database.insert(DBOpenHelper.TABLE_DMS, null, values);
			if(rowId >= 0){				
				
				// are we the receiver?
				if(values.containsKey(DirectMessages.COL_RECEIVER) && Long.toString(values.getAsLong(DirectMessages.COL_RECEIVER)).equals(LoginActivity.getTwitterId(getContext()))){
					
					if ( (!ShowDMUsersListActivity.running && !ShowDMListActivity.running) && hasBeenExecuted()  &&
							PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("prefNotifyDirectMessages", true) == true ) {
						// notify user
						notifyUser(NOTIFY_DM, values.getAsString(DirectMessages.COL_SENDER)+": "+values.getAsString(DirectMessages.COL_TEXT));
						
					}
				}
				Uri insertUri = ContentUris.withAppendedId(DirectMessages.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(insertUri, null);
				
				if(flags>0){
					// start synch service with a synch tweet request
					Intent i = new Intent(TwitterService.SYNCH_ACTION);
					i.putExtra("synch_request", TwitterService.SYNCH_DM);
					i.putExtra("rowId", rowId);
					getContext().startService(i);
				}
				
				return insertUri;
			} else {
				throw new IllegalStateException("Could not insert direct message into DB " + values);
			}
		} else {
			throw new IllegalArgumentException("Illegal direct message: " + values);
		}
	}
	
	


	
	/**
	 * Creates and triggers the status bar notifications
	 */
	private void notifyUser(int type, String tickerText){
		
		NotificationManager mNotificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.ic_notification;
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		Context context = getContext().getApplicationContext();
		
		CharSequence contentTitle = getContext().getString(R.string.dm_content_title);
		CharSequence contentText = "New DirectMessages!";
		Intent notificationIntent = new Intent(getContext(), ShowDMUsersListActivity.class);
		PendingIntent contentIntent;
		switch(type){
		case(NOTIFY_DM):
			contentText = getContext().getString(R.string.dm_content_text);
			break;
		default:
			break;
		}
		contentIntent = PendingIntent.getActivity(getContext(), 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

		mNotificationManager.notify(DM_NOTIFICATION_ID, notification);

	}

}
