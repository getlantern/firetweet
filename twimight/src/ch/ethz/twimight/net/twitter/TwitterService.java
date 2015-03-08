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

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import winterwell.jtwitter.OAuthSignpostClient;
import winterwell.jtwitter.Status;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.Twitter.KEntityType;
import winterwell.jtwitter.Twitter.TweetEntity;
import winterwell.jtwitter.TwitterException;
import winterwell.jtwitter.Twitter_Account;
import winterwell.jtwitter.User;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;
import ch.ethz.bluetest.credentials.Obfuscator;
import ch.ethz.twimight.R;
import ch.ethz.twimight.activities.LoginActivity;
import ch.ethz.twimight.activities.NewDMActivity;
import ch.ethz.twimight.activities.SearchableActivity;
import ch.ethz.twimight.activities.ShowDMUsersListActivity;
import ch.ethz.twimight.activities.ShowTweetListActivity;
import ch.ethz.twimight.activities.ShowUserActivity;
import ch.ethz.twimight.activities.ShowUserListActivity;
import ch.ethz.twimight.activities.ShowUserTweetListActivity;
import ch.ethz.twimight.activities.TwimightBaseActivity;
import ch.ethz.twimight.data.HtmlPagesDbHelper;
import ch.ethz.twimight.net.Html.StartServiceHelper;
import ch.ethz.twimight.util.Constants;

/**
 * The service to send all kinds of API calls to Twitter. 
 * This is the only place where calling Twitter is allowed!
 * @author thossman
 * @author pcarta
 */
public class TwitterService extends Service {

	static final String TAG = "TwitterService";

	public static final String SYNCH_ACTION = "twimight_synch";

	public static final int SYNCH_LOGIN = 0;
	public static final int SYNCH_ALL = 1;
	public static final int SYNCH_TIMELINE = 2;
	public static final int SYNCH_FAVORITES = 3;
	public static final int SYNCH_MENTIONS = 4;
	public static final int SYNCH_FRIENDS = 7;
	public static final int SYNCH_FOLLOWERS = 8;
	public static final int SYNCH_USER = 9;
	public static final int SYNCH_TWEET = 10;
	public static final int SYNCH_DMS = 11;
	public static final int SYNCH_DM = 12;
	public static final int SYNCH_USERTWEETS = 13;
	public static final int SYNCH_SEARCH_TWEETS = 14;
	public static final int SYNCH_VERIFY = 15;
	public static final int SYNCH_TRANSACTIONAL = 16;
	public static final int SYNCH_SEARCH_USERS = 18;
	public static final int SYNCH_HTML_PAGE = 19;
	
	public static final int OVERSCROLL_TOP = 100;	
	public static final int OVERSCROLL_BOTTOM = -100;
	
	
	public static final long TRUE = 1;
	public static final long FALSE = 0;
	
	public static final String FORCE_FLAG = "force";
	public static final String TASK_MENTIONS = "mentions";
	public static final String TASK_DIRECT_MESSAGES_IN = "direct_messages_in";	
	public static final String OVERSCROLL_TYPE = "overscroll_type";		
	public static final String URL = "url";	
	
	private static boolean goStarted = false;
	
	Twitter twitter;
	NetworkInfo currentNetworkInfo;
	
	public static final boolean D = true;


	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	/**
	 * Executed when the service is started. We return START_STICKY to not be stopped immediately.
	 * Here we decide which async task(s) to launch, depending on the synch_request in the intent.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){

		//TwitterAlarm.releaseWakeLock();
		// Do we have connectivity?
		ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		currentNetworkInfo = cm.getActiveNetworkInfo();
		if(currentNetworkInfo==null || !currentNetworkInfo.isConnected()){
			
			Log.w(TAG, "Error synching: no connectivity");
			return START_NOT_STICKY;
			
		} else {
			// Create twitter object
			if(twitter == null){
				
				String token = LoginActivity.getAccessToken(this);
				String secret = LoginActivity.getAccessTokenSecret(this);
				if(!(token == null || secret == null) ) {
					// get ready for OAuth
					OAuthSignpostClient client = new OAuthSignpostClient(Obfuscator.getKey(), Obfuscator.getSecret(), token, secret);
					twitter = new Twitter(null, client);
				} else {
					Log.e(TAG, "Error synching: no access token or secret");
					return START_NOT_STICKY;
				}				
				twitter.setIncludeTweetEntities(true);
			}
			
			twitter.setSinceId(null);
			twitter.setUntilId(null);

			if (intent != null) {
				// check what we are asked to synch
				int synchRequest = intent.getIntExtra("synch_request", SYNCH_ALL);				
				
				switch(synchRequest){
				
				case SYNCH_TRANSACTIONAL:			
				    synchTransactional();
					break;
				case SYNCH_LOGIN:
					synchLogin();
					break;
				case SYNCH_VERIFY:
					synchVerify();
					break;
				case SYNCH_ALL:					
					if (!intent.hasExtra("isLogin")) {						
						synchTimeline(intent);
						synchMentions(intent.getBooleanExtra(FORCE_FLAG, false));									
						synchMessages();
						synchTransactional();	
					} else {
						 
						 Handler handler = new Handler();
						 handler.postDelayed(new GetFriendsFollowersDelayed( ), Constants.FRIENDS_FOLLOWERS_DELAY );
					}					
					break;
				case SYNCH_TIMELINE:				
						synchTimeline(intent);				
					break;
				case SYNCH_MENTIONS:
					synchMentions(intent.getBooleanExtra(FORCE_FLAG, false));
					break;
				case SYNCH_FAVORITES:				
					synchFavorites(intent.getBooleanExtra(FORCE_FLAG, false));
					break;
				case SYNCH_FRIENDS:
					synchFriends(TRUE);
					break;
				case SYNCH_FOLLOWERS:
					synchFollowers(TRUE);
					break;			
				case SYNCH_SEARCH_TWEETS:
					if(intent.getStringExtra("query") != null){
						synchSearchTweets(intent.getStringExtra("query"));
					}
					break;
				case SYNCH_SEARCH_USERS:
					if(intent.getStringExtra("query") != null){
						synchSearchUsers(intent.getStringExtra("query"));
					}
					break;
				case SYNCH_USER:
					Log.i(TAG,"SYNCH_USER");
					if(intent.hasExtra("rowId")){
						long rowId = intent.getLongExtra("rowId", -1);
						new UserQueryTask().execute(rowId);
					} 
					break;
				case SYNCH_TWEET:			
					if(intent.hasExtra("rowId")){
						// get the flags
						long rowId = intent.getLongExtra("rowId", -1);
						if (rowId >= 0) {
							new TweetQueryTask().execute(rowId);										
						}				
					}
					break;
				case SYNCH_DMS:
					synchMessages();
					break;
				case SYNCH_DM:
					if(intent.getLongExtra("rowId", 0) != 0){
						new SynchTransactionalMessagesTask().execute();
						//synchMessage(intent.getLongExtra("rowId", 0) , TRUE);
					}
					break;
				case SYNCH_USERTWEETS:
					if(intent.getStringExtra("screenname") != null){
						synchUserTweets(intent.getStringExtra("screenname"));
					}
					break;
				case SYNCH_HTML_PAGE:
					if (intent.getStringExtra(URL) != null){
						
					}					
					break;
				default:
					throw new IllegalArgumentException("Exception: Unknown synch request");
				}

				return START_STICKY;
				
			} else
				return START_NOT_STICKY;
		
			
			
		}

		
	}
	
	private class UserQueryTask extends AsyncTask<Long, Void, Cursor> {	
		

		@Override
		protected Cursor doInBackground(Long... params) {

			Uri queryUri = Uri.parse("content://"+TwitterUsers.TWITTERUSERS_AUTHORITY+"/"+TwitterUsers.TWITTERUSERS+"/"+params[0]);
			Cursor c = null;

			c = getContentResolver().query(queryUri, null, null, null, null);

			if(c.getCount() == 0)
				Log.w(TAG, "Synch Tweet: Tweet not found " + params[0]);					

			c.moveToFirst();
			return c;
		}

		@Override
		protected void onPostExecute(Cursor c) {
			synchUser(c,true);
			c.close();

		}
	}

	private class TweetQueryTask extends AsyncTask<Long, Void, Cursor> {	


		@Override
		protected Cursor doInBackground(Long... params) {

			Uri queryUri = Uri.parse("content://"+Tweets.TWEET_AUTHORITY+"/"+Tweets.TWEETS+"/"+params[0]);
			Cursor c = null;					
			c = getContentResolver().query(queryUri, null, null, null, null);

			if(c.getCount() == 1){
				c.moveToFirst();
				return c;				
			}				

			return null;			
		}

		@Override
		protected void onPostExecute(Cursor c) {
			Log.d(TAG, "synchTweet");
			synchTweet(c,TRUE);
			if(c!=null) c.close();	
		}
	}	

	private class GetFriendsFollowersDelayed implements Runnable {
		@Override
		public void run() {
			synchFriends(FALSE);
			synchFollowers(FALSE);
		}
	}



	public static void setTaskExecuted(String which, Context context){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putBoolean(which,true );
		prefEditor.commit();
	}
	
	/**
	 * Starts a thread to search Twitter users
	 */
	private void synchSearchUsers(String query) {
			Log.i(TAG, "SYNCH_SEARCH USERS");
			(new SearchUsersTask()).execute(query);
		
	}

	private void synchTransactional() {
		new SynchTransactionalMessagesTask().execute();
		new SynchTransactionalTweetsTask().execute();
	    new SynchTransactionalUsersTask(false).execute(false);
		
	}



	/**
	 * Creates the thread to update friends
	 */
	private void synchFriends(long notify) {
		Log.d(TAG, "SYNCH_FRIENDS");
		if(System.currentTimeMillis() - getLastFriendsUpdate(getBaseContext()) > Constants.FRIENDS_MIN_SYNCH){
			(new UpdateFriendsTask()).execute(notify);
		} 

	}

	/**
	 * Creates the thread to update followers
	 */
	private void synchFollowers(long notify) {
		Log.i(TAG, "SYNCH_FOLLOWERS");
		if(System.currentTimeMillis() - getLastFollowerUpdate(getBaseContext()) > Constants.FOLLOWERS_MIN_SYNCH){
			(new UpdateFollowersTask()).execute(notify);
		} else {
			Log.i(TAG, "Last followers synch too recent.");
		}		
	}

	/**
	 * Verifies the user credentials by a verifyCredentials call.
	 * Stores the user ID in the shared preferences.
	 */
	private void synchLogin(){
		Log.i(TAG, "SYNCH_LOGIN");
		Integer [] params = {Constants.LOGIN_ATTEMPTS, 1}; // nr of attempts, notify login activity about result
		(new VerifyCredentialsTask()).execute(params);
	}

	/**
	 * Verifies the user credentials by a verifyCredentials call.
	 * Like synchLogin but does not start the timeline afterwards.
	 * Stores the user ID in the shared preferences.
	 */
	private void synchVerify(){
		Log.i(TAG, "SYNCH_VERIFY");
		Integer [] params = {Constants.LOGIN_ATTEMPTS, 0}; // nr of attempts, do not notify login activity about result		
		new VerifyCredentialsTask().execute(params);
	}	
	

	
	/**
	 * Syncs all tweets which have transactional flags set
	 */
	private class SynchTransactionalTweetsTask extends AsyncTask<Void, Void, Void> {

		

		@Override
		protected Void doInBackground(Void... params) {
			
			if (D) Log.i(TAG, "SYNCH_TRANSACTIONAL_TWEETS");
			// get the flagged tweets
			Uri queryUri = Uri.parse("content://"+Tweets.TWEET_AUTHORITY+"/"+Tweets.TWEETS);
			Cursor c = null;

			c = getContentResolver().query(queryUri, null, Tweets.COL_FLAGS+"!=0", null, null);		
			if(c!=null && c.getCount() >= 0) {
				
				c.moveToFirst();
				while(!c.isAfterLast()){					
					synchTweet(c,FALSE);
					c.moveToNext();
				}


				c.close();
			}


			return null;
		}


	}

	/**
	 * Syncs all messages which have transactional flags set
	 */
	private class SynchTransactionalMessagesTask extends AsyncTask<Void, Void, Void> {		

		@Override
		protected Void doInBackground(Void... params) {

			// get the flagged messages
			Uri queryUri = Uri.parse("content://"+DirectMessages.DM_AUTHORITY+"/"+DirectMessages.DMS);
			Cursor c = null;
			
				c = getContentResolver().query(queryUri, null, DirectMessages.COL_FLAGS+"!=0", null, null);
				
				Log.i(TAG, c.getCount()+" transactional messages to synch");
				if(c.getCount() >= 0){
					c.moveToFirst();
					while(!c.isAfterLast()){
						synchMessage(c.getLong(c.getColumnIndex("_id")) , FALSE);
						c.moveToNext();
					}
				}			
				c.close();
			
			return null;
		}
		
		
	}

	/**
	 * Syncs all users which have transactional flags set
	 * 
	 */
	private class SynchTransactionalUsersTask extends AsyncTask<Boolean, Void, Void> {
		boolean picturesBulkInsert;
		
		public SynchTransactionalUsersTask(boolean bulkInsertRequired) {
			this.picturesBulkInsert=bulkInsertRequired;			
		}

		@Override
		protected Void doInBackground(Boolean... params) {

			// get the flagged users
			Uri queryUri = Uri.parse("content://"+TwitterUsers.TWITTERUSERS_AUTHORITY+"/"+TwitterUsers.TWITTERUSERS);
			Cursor c = null;

			c = getContentResolver().query(queryUri, null, TwitterUsers.COL_FLAGS+"!=0", null, null);			

			if(c.getCount() >= 0){
				c.moveToFirst();				
				
				if (picturesBulkInsert) {				
					long[] rowIds = getRowIdsFromCursor(c);					
					Intent picturesIntent = new Intent(TwitterService.this, PicturesIntentService.class);
					picturesIntent.putExtra(PicturesIntentService.USERS_IDS, rowIds);					
					startService(picturesIntent);
					
				} else {
					
					while(!c.isAfterLast()){
						synchUser(c,params[0]);					
						c.moveToNext();					
					}
				}					
				
			}
			c.close();		
			
			return null;
		}
	}
	
	private long[] getRowIdsFromCursor(Cursor c) {
		long[] ids = new long[c.getCount()];
		
		for (int i=0; i<c.getCount(); i++) {			
			ids[i]= c.getLong(c.getColumnIndex("_id"));
			c.moveToNext();
		}
		return ids ;
	}	
	
	/**
	 * Checks the transactional flags of the tweet with the given _id and performs the corresponding actions
	 */
	private void synchTweet(Cursor c, long notify) {
		Log.i(TAG, "SYNCH_TWEET");
		if (c != null) {
			
			int flags = c.getInt(c.getColumnIndex(Tweets.COL_FLAGS));
			long rowId = c.getInt(c.getColumnIndex("_id"));
			
			if((flags & Tweets.FLAG_TO_DELETE)>0) {
				// Delete a tweet from twitter
				Long[] params = {rowId, 3L, notify}; // three attempts
				(new DestroyStatusTask()).execute(params);
				
			} else if((flags & Tweets.FLAG_TO_INSERT)>0) {
				// post the tweet to twitter
				Log.i(TAG,"uploading tweet");
				Long[] params = {rowId, 3L, notify}; // three attempts
				(new UpdateStatusTask()).execute(params);
			} 		
			if((flags & Tweets.FLAG_TO_FAVORITE)>0) {
				// post favorite to twitter
				Long[] params = {rowId, 3L, notify}; // three attempts
				(new FavoriteStatusTask()).execute(params);
				
			} else if((flags & Tweets.FLAG_TO_UNFAVORITE)>0) {
				// remove favorite from twitter
				Long[] params = {rowId, 3L, notify}; // three attempts
				(new UnfavoriteStatusTask()).execute(params);
			} 
			if((flags & Tweets.FLAG_TO_RETWEET)>0) {
				// retweet
				Long[] params = {rowId, 3L, notify}; // three attempts
				(new RetweetStatusTask()).execute(params);
			} 
		}
		
		
	}

	/**
	 * Checks the transactional flags of the user with the given _id and performs the corresponding actions
	 */
	private void synchUser(Cursor c, boolean force) {		
		// get the flags
		
		int flags = c.getInt(c.getColumnIndex(TwitterUsers.COL_FLAGS));
		long rowId = c.getInt(c.getColumnIndex("_id"));
		
		if((flags & TwitterUsers.FLAG_TO_UPDATE)>0) {			
			// Update a user if it's time to do so
			if(force || ( c.isNull(c.getColumnIndex(TwitterUsers.COL_LASTUPDATE)) ||
					(System.currentTimeMillis() - c.getInt(c.getColumnIndex(TwitterUsers.COL_LASTUPDATE)) >Constants.USERS_MIN_SYNCH) )){
				
				Long[] params = {rowId, 2L}; // three attempts
				(new UpdateUserTask()).execute(params);				
			} 
			
		} else if((flags & TwitterUsers.FLAG_TO_FOLLOW)>0) {
			// Follow a user
			Long[] params = {rowId, 2L}; // three attempts
			(new FollowUserTask()).execute(params);
			
		} else if((flags & TwitterUsers.FLAG_TO_UNFOLLOW)>0) {
			// Unfollow a user
			Long[] params = {rowId, 2L}; // three attempts
			(new UnfollowUserTask()).execute(params);
			
		} else if((flags & TwitterUsers.FLAG_TO_UPDATEIMAGE)>0){
		
			// load the profile image			
			if( c.isNull(c.getColumnIndex(TwitterUsers.COL_LAST_PICTURE_UPDATE)) ||
					(System.currentTimeMillis() - c.getInt(c.getColumnIndex(TwitterUsers.COL_LAST_PICTURE_UPDATE)) >Constants.USERS_MIN_SYNCH)){
				long[] rowIds = {rowId};
				Intent picturesIntent = new Intent(TwitterService.this, PicturesIntentService.class);
				picturesIntent.putExtra(PicturesIntentService.USERS_IDS, rowIds);
				startService(picturesIntent);
			}
		}
		
	}

	/**
	 * Starts a thread to load the timeline. But only if the last timeline request is old enough.
	 */
	private void synchTimeline(Intent intent) {
		
		boolean force = intent.getBooleanExtra(FORCE_FLAG, false);
		
		if(force || (System.currentTimeMillis() - getLastTimelineUpdate(getBaseContext()) > Constants.TIMELINE_MIN_SYNCH)){
			(new UpdateTimelineTask()).execute(Constants.TIMELINE_ATTEMPTS, intent.getIntExtra(OVERSCROLL_TYPE, OVERSCROLL_TOP));
		} 
	}

	/**
	 * Starts a thread to load the favorites. But only if the last favorites request is old enough.
	 */
	private void synchFavorites(boolean force) {
		Log.d(TAG, "SYNCH_FAVORITES");
		if(force || (System.currentTimeMillis() - getLastFavoritesUpdate(getBaseContext()) > Constants.FAVORITES_MIN_SYNCH)){
			(new UpdateFavoritesTask()).execute();
		} 

	}

	/**
	 * Starts a thread to load the mentions. But only if the last mentions request is old enough.
	 */
	private void synchMentions(boolean force) {
		Log.d(TAG, "SYNCH_MENTIONS");
		if(force || (System.currentTimeMillis() - getLastMentionsUpdate(getBaseContext()) > Constants.MENTIONS_MIN_SYNCH)){
			(new UpdateMentionsTask()).execute();
		} 

	}

	/**
	 * Loads DMs from Twitter
	 */
	private void synchMessages() {
		Log.d(TAG, "SYNCH_MESSAGES");
		if(System.currentTimeMillis() - getLastDMsInUpdate(getBaseContext()) > Constants.DMS_MIN_SYNCH){
			(new UpdateDMsInTask()).execute(3); // maximum three attempts before we give up
		} else {
			Log.i(TAG, "Last DM IN synch too recent.");
		}

		if(System.currentTimeMillis() - getLastDMsOutUpdate(getBaseContext()) > Constants.DMS_MIN_SYNCH){
			(new UpdateDMsOutTask()).execute(3); // maximum three attempts before we give up
		} else {
			Log.i(TAG, "Last DM Out synch too recent.");
		}


	}

	/**
	 * Checks the transactional flags of the direct message with the given _id and performs the corresponding actions
	 */
	private void synchMessage(long rowId, long notify) {
		if (D) Log.i(TAG, "SYNCH_DM");
		// get the flags
		Uri queryUri = Uri.parse("content://"+DirectMessages.DM_AUTHORITY+"/"+DirectMessages.DMS+"/"+rowId);
		Cursor c = null;		


		c = getContentResolver().query(queryUri, null, null, null, null);
		if(c == null || c.getCount() == 0 ){
			if (D) Log.w(TAG, "Synch Message: Message not found " + rowId);
			return;
		}
		c.moveToFirst();
		try{	
			int flags = c.getInt(c.getColumnIndexOrThrow(DirectMessages.COL_FLAGS));

			if((flags & DirectMessages.FLAG_TO_DELETE)>0) {
				// Delete the DM from twitter				
				new DestroyMessageTask().execute(rowId,3L, notify);

			} else if((flags & DirectMessages.FLAG_TO_INSERT)>0) {
				// post the DM to twitter				
				(new SendMessageTask()).execute(rowId, notify);
			} 

		} catch(IllegalArgumentException ex){
			if (D) Log.e(TAG, "Exception: " + ex);
		} finally {
			c.close();	
		}
	}

	/**
	 * Starts a thread to load the tweets of a user
	 */
	private void synchUserTweets(String screenname) {

		Log.d(TAG, "SYNCH_USERTWEETS");
		(new UpdateUserTweetsTask()).execute(screenname);

	}
	
	/**
	 * Starts a thread to search Twitter
	 */
	private void synchSearchTweets(String query) {

		Log.d(TAG, "SYNCH_SEARCH");
		(new SearchTweetsTask()).execute(query);

	}

	/**
	 * Reads the ID of the last tweet from shared preferences.
	 * @return
	 */
	public static BigInteger getTimelineSinceId(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String sinceIdString = prefs.getString("timelineSinceId", null); 
		if(sinceIdString==null)
			return null;
		else
			return new BigInteger(sinceIdString);
	}
	
	/**
	 * Reads the ID of the last tweet from shared preferences.
	 * @return
	 */
	public static BigInteger getTimelineUntilId(Context context) {
		Cursor c = context.getContentResolver().query(Uri.parse("content://" + Tweets.TWEET_AUTHORITY + "/" + Tweets.TWEETS + "/" 
				+ Tweets.TWEETS_TABLE_TIMELINE + "/" + Tweets.TWEETS_SOURCE_NORMAL), null, null, null, null);
		if (c.getCount()>0) {
			c.moveToFirst();
		} else
			return null;
		
			
		if (!c.isNull(c.getColumnIndex(Tweets.COL_TID))) {			
			return new BigInteger(Long.toString(c.getLong(c.getColumnIndex(Tweets.COL_TID))));
		}
		else
			return null;
		
	}
	

	/**
	 * Stores the given ID as the since ID
	 */
	public static void setTimelineSinceId(BigInteger sinceId, Context context) {
		
		Log.i(TAG,"inside setTimelineSinceId");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString("timelineSinceId",sinceId==null?null:sinceId.toString());
		prefEditor.commit();
	}	
	
	/**
	 * Reads the timestamp of the last timeline update from shared preferences.
	 * @return
	 */
	public static long getLastTimelineUpdate(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getLong("timelineLastUpdate", 0);
	}

	/**
	 * Stores the current timestamp as the time of last timeline update
	 */
	public static void setLastTimelineUpdate(long timestamp, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();		
		prefEditor.putLong("timelineLastUpdate", timestamp);
		
		prefEditor.commit();
	}

	/**
	 * Reads the ID of the last favorites tweet from shared preferences.
	 * @return
	 */
	public static BigInteger getFavoritesSinceId(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String sinceIdString = prefs.getString("favoritesSinceId", null); 
		if(sinceIdString==null)
			return null;
		else
			return new BigInteger(sinceIdString);
	}

	/**
	 * Stores the given ID as the since ID
	 */
	public static void setFavoritesSinceId(BigInteger sinceId, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString("favoritesSinceId",sinceId==null?null:sinceId.toString());
		prefEditor.commit();
	}

	/**
	 * Reads the timestamp of the last favorites update from shared preferences.
	 * @return
	 */
	public static long getLastFavoritesUpdate(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getLong("favoritesLastUpdate", 0);
	}

	/**
	 * Stores the current timestamp as the time of last favorites update
	 */
	public static void setLastFavoritesUpdate(Date date, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		if(date!=null)
			prefEditor.putLong("favoritesLastUpdate", date.getTime());
		else
			prefEditor.putLong("favoritesLastUpdate", 0);

		prefEditor.commit();
	}

	/**
	 * Reads the ID of the last mentions tweet from shared preferences.
	 * @return
	 */
	public static BigInteger getMentionsSinceId(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String sinceIdString = prefs.getString("mentionsSinceId", null); 
		if(sinceIdString==null)
			return null;
		else
			return new BigInteger(sinceIdString);
	}

	/**
	 * Stores the given ID as the since ID
	 */
	public static void setMentionsSinceId(BigInteger sinceId, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString("mentionsSinceId",sinceId==null?null:sinceId.toString());
		prefEditor.commit();
	}

	/**
	 * Reads the timestamp of the last mentions update from shared preferences.
	 * @return
	 */
	public static long getLastMentionsUpdate(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getLong("mentionsLastUpdate", 0);
	}

	/**
	 * Stores the current timestamp as the time of last mentions update
	 */
	public static void setLastMentionsUpdate(Date date, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		if(date!=null)
			prefEditor.putLong("mentionsLastUpdate", date.getTime());
		else
			prefEditor.putLong("mentionsLastUpdate", 0);

		prefEditor.commit();
	}

	/**
	 * Reads the timestamp of the last friends update from shared preferences.
	 * @return
	 */
	public static long getLastFriendsUpdate(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getLong("friendsLastUpdate", 0);
	}

	/**
	 * Stores the current timestamp as the time of last friends update
	 */
	public static void setLastFriendsUpdate(Date date, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		if(date!=null)
			prefEditor.putLong("friendsLastUpdate", date.getTime());
		else
			prefEditor.putLong("friendsLastUpdate", 0);

		prefEditor.commit();
	}

	/**
	 * Reads the timestamp of the last follower update from shared preferences.
	 * @return
	 */
	public static long getLastFollowerUpdate(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getLong("followerLastUpdate", 0);
	}

	/**
	 * Stores the current timestamp as the time of last follower update
	 */
	public static void setLastFollowerUpdate(Date date, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		if(date!=null)
			prefEditor.putLong("followerLastUpdate", date.getTime());
		else
			prefEditor.putLong("followerLastUpdate", 0);

		prefEditor.commit();
	}

	/**
	 * Reads the timestamp of the last DM (incoming) update from shared preferences.
	 * @return
	 */
	public static long getLastDMsInUpdate(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getLong("DMsInLastUpdate", 0);
	}

	/**
	 * Stores the current timestamp as the time of last DM update
	 */
	public static void setLastDMsInUpdate(Date date, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		if(date!=null)
			prefEditor.putLong("DMsInLastUpdate", date.getTime());
		else
			prefEditor.putLong("DMsInLastUpdate", 0);

		prefEditor.commit();
	}

	/**
	 * Reads the ID of the last incoming direct message
	 */
	public BigInteger getDMsInSinceId(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String sinceIdString = prefs.getString("DMsInSinceId", null); 
		if(sinceIdString==null)
			return null;
		else
			return new BigInteger(sinceIdString);	}

	/**
	 * Stores the provided ID as the last incoming DM
	 * @param lastId
	 * @param baseContext
	 */
	public static void setDMsInSinceId(BigInteger sinceId, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString("DMsInSinceId",sinceId==null?null:sinceId.toString());
		prefEditor.commit();		
	}

	/**
	 * Reads the timestamp of the last DM (outgoing) update from shared preferences.
	 * @return
	 */
	public static long getLastDMsOutUpdate(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getLong("DMsOutLastUpdate", 0);
	}

	/**
	 * Stores the current timestamp as the time of last DM (outgoing) update
	 */
	public static void setLastDMsOutUpdate(Date date, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		if(date!=null)
			prefEditor.putLong("DMsOutLastUpdate", date.getTime());
		else
			prefEditor.putLong("DMsOutLastUpdate", 0);

		prefEditor.commit();
	}

	/**
	 * Reads the ID of the last outgoing direct message
	 */
	public BigInteger getDMsOutSinceId(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String sinceIdString = prefs.getString("DMsOutSinceId", null); 
		if(sinceIdString==null)
			return null;
		else
			return new BigInteger(sinceIdString);	}

	/**
	 * Stores the provided ID as the last outgoing DM
	 * @param lastId
	 * @param baseContext
	 */
	public static void setDMsOutSinceId(BigInteger sinceId, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString("DMsOutSinceId",sinceId==null?null:sinceId.toString());
		prefEditor.commit();		
	}

	/**
	 * Updates a tweet in the DB (or inserts it if the tweet is new to us).
	 * Returns the row ID on success, 0 on failure.
	 * @param ret_screenName 
	 */
	private int updateTweets(ContentValues[] cv){		
		
		Uri insertUri = Uri.parse("content://"+Tweets.TWEET_AUTHORITY+"/"+Tweets.TWEETS+"/"+Tweets.TWEETS_TABLE_TIMELINE + "/" + Tweets.TWEETS_SOURCE_NORMAL);
		int result = getContentResolver().bulkInsert(insertUri, cv);		
		return result;		
		
	}


	/**
	 * Updates a direct message in the DB (or inserts it if the message is new to us)
	 * Returns the row ID on success, 0 on failure.
	 */
	private int updateMessage(winterwell.jtwitter.Message dm, int buffer){
		
		if(dm==null) return 0;
		
		try{
			Uri insertUri = Uri.parse("content://"+DirectMessages.DM_AUTHORITY +"/"+ DirectMessages.DMS + "/" + DirectMessages.DMS_LIST + "/" + DirectMessages.DMS_SOURCE_NORMAL);
			Uri resultUri = getContentResolver().insert(insertUri, getMessageContentValues(dm, buffer));
			return Integer.valueOf(resultUri.getLastPathSegment());
		} catch (Exception ex) {
			Log.e(TAG, "Exception while updating message");
			return 0;
		}
	}


	/**
	 * Updates the user profile in the DB.
	 * @param user
	 */
	private long updateUser(User user, boolean insertAsFriend) {

		if(user==null) return 0;
		
		ContentValues cv = getUserContentValues(user,insertAsFriend);
		if (cv != null) {
			Uri insertUri = Uri.parse("content://" + TwitterUsers.TWITTERUSERS_AUTHORITY + "/" + TwitterUsers.TWITTERUSERS);
			Uri resultUri = getContentResolver().insert(insertUri, cv);
		
			return Long.valueOf(resultUri.getLastPathSegment());
		}
		else return 0;		

	}
	
	/**
	 * Updates the user profile in the DB.
	 * @param contentValues 
	 * @param user
	 */
	private long updateUsers(ContentValues[] users) {
		if(users==null) return 0;
		
		Uri insertUri = Uri.parse("content://" + TwitterUsers.TWITTERUSERS_AUTHORITY + "/" + TwitterUsers.TWITTERUSERS);
		int result = getContentResolver().bulkInsert(insertUri, users);

		return result;	

	}


	private class CacheUrlTask extends AsyncTask<Void, Void, Void>{
		
		String tweet;
		long id;
		
		public CacheUrlTask(String tweet, long id){
			this.tweet = tweet;
			this.id = id;
		}
		
		@Override
		protected Void doInBackground(Void... params) {

			HtmlPagesDbHelper htmlDbHelper = new HtmlPagesDbHelper(getApplicationContext());
			htmlDbHelper.open();
			htmlDbHelper.insertLinksIntoDb(tweet, id, HtmlPagesDbHelper.DOWNLOAD_NORMAL);
			return null;
		}

		
	}

	/**
	 * Creates content values for a tweet from Twitter
	 * @param tweet
	 * @param ret_screenName 
	 * @return
	 */
	private ContentValues getTweetContentValues(Status tweet, String scrName, int buffer) {
		ContentValues cv = new ContentValues();
		
		if(tweet==null || tweet.getId()==null || tweet.getText()== null) 
			return null;
		
		if (scrName != null) {			
			cv.put(Tweets.COL_RETWEETED_BY,scrName);
		}
		
		
		String tweetSpanText = createSpans(tweet).getText();
		cv.put(Tweets.COL_TEXT, tweetSpanText);
		cv.put(Tweets.COL_TEXT_PLAIN, Html.fromHtml(tweetSpanText).toString()  );
		
		String tweetText = Html.fromHtml(tweetSpanText).toString();
		//if there are urls to this tweet, change the status of html field to 1
		if(tweetText.indexOf("http://") > 0 || tweetText.indexOf("https://") > 0 ){
			cv.put(Tweets.COL_HTML_PAGES, 1);

			boolean isOfflineActive  = PreferenceManager.getDefaultSharedPreferences(TwitterService.this).getBoolean(
					TwitterService.this.getString(R.string.pref_offline_mode),false);
			if (isOfflineActive ){
				new CacheUrlTask(tweetText,tweet.getId().longValue()).execute();

			}	
		}	
		cv.put(Tweets.COL_CREATED, tweet.getCreatedAt().getTime());
		cv.put(Tweets.COL_SOURCE, tweet.source);

		cv.put(Tweets.COL_TID, tweet.getId().longValue());

		if (tweet.isFavorite())
			buffer = buffer | Tweets.BUFFER_FAVORITES;		
		
		// TODO: How do we know if we have retweeted the tweet?
		cv.put(Tweets.COL_RETWEETED, 0);
		cv.put(Tweets.COL_RETWEETCOUNT, tweet.retweetCount);
		if(tweet.inReplyToStatusId != null){
			cv.put(Tweets.COL_REPLYTO, tweet.inReplyToStatusId.longValue());
		}
		
		cv.put(Tweets.COL_TWITTERUSER, tweet.getUser().getId());	
		cv.put(Tweets.COL_SCREENNAME, tweet.getUser().getScreenName());
		
		//insert the picture url to the database
		//tweet.getEntities-> List<TweetEntity>
		//
		//cv.put(Tweets.COL_MEDIA tweet.getEntities(KEntityType.media))
		//cv.put(Tweets.COL_FLAGS, 0);
		cv.put(Tweets.COL_BUFFER, buffer);
		
		return cv;
	}
	
	

	private class SpanResult {
		private String text;
		private ArrayList<String> urls;
		
		SpanResult(String text, ArrayList<String> urls) {
			this.text=text;
			this.urls=urls;
		}
		
		String getText() {
			return text;
		}
		
		ArrayList<String> getUrls() {
			return urls;
		}
	}

	/**
	 * Creates spans for entities (mentions, urls, hashtags).
	 * @param tweet
	 * @return The tweet text with the spans
	 */	
	@SuppressWarnings("unchecked")
	private SpanResult createSpans(Status tweet){

		if(tweet==null) return null;
		ArrayList<String> urls = null;
		String originalText = (String) tweet.getText();

		// we need one list with all entities, sorted by their start
		List<TweetEntity> allEntities = new ArrayList<TweetEntity>();

		List<TweetEntity> entities = tweet.getTweetEntities(Twitter.KEntityType.hashtags);
		if(entities != null){
			for (TweetEntity entity: entities) {
				allEntities.add(entity);
			}
		}
		entities = tweet.getTweetEntities(Twitter.KEntityType.user_mentions);
		if(entities != null){
			for (TweetEntity entity: entities) {
				allEntities.add(entity);
				try{
					// we add (or update, if we already have them) the user to the local DB.
					String screenname = tweet.getText().substring(entity.start+1, entity.end);				
					ContentValues cv = new ContentValues();
					cv.put(TwitterUsers.COL_NAME, entity.displayVersion());
					cv.put(TwitterUsers.COL_SCREENNAME, screenname);						
					//cv.put(TwitterUsers.COL_FLAGS, TwitterUsers.FLAG_TO_UPDATE);

					Uri insertUri = Uri.parse("content://" + TwitterUsers.TWITTERUSERS_AUTHORITY + "/" + TwitterUsers.TWITTERUSERS);
					getContentResolver().insert(insertUri, cv);
				} catch(Exception ex){
					Log.e(TAG, "Exception while inserting mentioned user");
				}
			}
		}
		entities = tweet.getTweetEntities(Twitter.KEntityType.urls);
		if(entities != null){
			
			for (TweetEntity entity: entities) {				
				allEntities.add(entity);
			}
		} 			
		// do we have entities at all?
		if(allEntities.isEmpty()) return new SpanResult(tweet.getText(),urls);

		
		// sort according to start character
		Collections.sort(allEntities, new Comparator<TweetEntity>(){
			@Override
			public int compare(TweetEntity entity1, TweetEntity entity2) {
				
				return entity1.start - entity2.start;
			}
		});
		// assemble the text
		StringBuilder replacedText = new StringBuilder();
		int lastIndex = 0;
		try {
			
			urls = new ArrayList<String>();
			for (TweetEntity curEntity: allEntities) {
				// append everything before the start of this entity
				replacedText.append("<tweet>"+originalText.substring(lastIndex, curEntity.start));
				// append the entity
				if(curEntity.type == KEntityType.hashtags){
					replacedText.append("<hashtag target='"+curEntity.toString()+"'>"+ originalText.substring(curEntity.start, curEntity.end)+"</hashtag>");
				} else if(curEntity.type == KEntityType.urls){
					replacedText.append("<url target='"+originalText.substring(curEntity.start, curEntity.end)+"'>"+ curEntity.displayVersion()+"</url>");
					urls.add(curEntity.displayVersion());				    
				  
					
				} else if(curEntity.type == KEntityType.user_mentions){
					replacedText.append("<mention target='"+originalText.substring(curEntity.start, curEntity.end)+"' name='"+curEntity.displayVersion()+"'>"+ originalText.substring(curEntity.start, curEntity.end)+"</mention>");
				}
				lastIndex = curEntity.end;
			}
			// append the rest of the original text
			replacedText.append(originalText.substring(lastIndex,originalText.length())+"</tweet>");
			
			
		} catch (StringIndexOutOfBoundsException ex) {
			Log.e(TAG,"create spans error",ex);
			return new SpanResult(tweet.getText(),urls);
		}	
		
		return new SpanResult(replacedText.toString(),urls);
	}


	/**
	 * Creates content values for a DM from Twitter
	 * @param dm
	 * @param buffer
	 * @return
	 */
	private ContentValues getMessageContentValues(winterwell.jtwitter.Message dm, int buffer) {
		ContentValues cv = new ContentValues();
		cv.put(DirectMessages.COL_TEXT, dm.getText());
		cv.put(DirectMessages.COL_CREATED, dm.getCreatedAt().getTime());
		cv.put(DirectMessages.COL_DMID, dm.getId().longValue());

		cv.put(DirectMessages.COL_SENDER, dm.getSender().getId());
		cv.put(DirectMessages.COL_RECEIVER, dm.getRecipient().getId());
		cv.put(DirectMessages.COL_RECEIVER_SCREENNAME, dm.getRecipient().getScreenName());
		cv.put(Tweets.COL_BUFFER, buffer);

		return cv;
	}


	/**
	 * Creates content values for a user from Twitter. Flags the user for updating their profile picture.
	 * @param user
	 * @return
	 */
	private ContentValues getUserContentValues(User user,boolean insertAsFriend) {
		ContentValues userContentValues = new ContentValues();

		if(user!=null){
			if(user.screenName!=null ) {				
				userContentValues.put(TwitterUsers.COL_TWITTERUSER_ID, user.getId());					
				userContentValues.put(TwitterUsers.COL_SCREENNAME, user.getScreenName());				
				userContentValues.put(TwitterUsers.COL_NAME, user.getName());
				if(user.description!=null) userContentValues.put(TwitterUsers.COL_DESCRIPTION, user.getDescription());
				if(user.location!=null) userContentValues.put(TwitterUsers.COL_LOCATION, user.getLocation());
				userContentValues.put(TwitterUsers.COL_FAVORITES, user.favoritesCount);
				userContentValues.put(TwitterUsers.COL_FRIENDS, user.friendsCount);
				userContentValues.put(TwitterUsers.COL_FOLLOWERS, user.followersCount);
				userContentValues.put(TwitterUsers.COL_LISTED, user.listedCount);
				userContentValues.put(TwitterUsers.COL_TIMEZONE, user.timezone);
				userContentValues.put(TwitterUsers.COL_STATUSES, user.statusesCount);
				userContentValues.put(TwitterUsers.COL_VERIFIED, user.verified);
				userContentValues.put(TwitterUsers.COL_PROTECTED, user.protectedUser);
				if(user.getProfileImageUrl()!=null) {
					userContentValues.put(TwitterUsers.COL_IMAGEURL, user.getProfileImageUrl().toString());
					// we flag the user for updating their profile image	
					userContentValues.put(TwitterUsers.COL_FLAGS, TwitterUsers.FLAG_TO_UPDATEIMAGE);	
				}
				if (insertAsFriend) {
					userContentValues.put(TwitterUsers.COL_ISFRIEND, 1);
					
				}
				
				
				return userContentValues;
			} else		
				return null;
					
			
		} else
			return null;
		
	}

	/**
	 * Logs in with Twitter and writes the local user into the DB.
	 * @author thossmann
	 *
	 */
	private class VerifyCredentialsTask extends AsyncTask<Integer, Void, User> {

		int attempts;
		int startTimeline;
		Exception ex;

		@Override
		protected User doInBackground(Integer... params) {
			Log.d(TAG, "AsynchTask: VerifyCredentialsTask");
			attempts = params[0];
			startTimeline = params[1];
			
			User user = null;

			try {
				Twitter_Account twitterAcc = new Twitter_Account(twitter);
				user = twitterAcc.verifyCredentials();

			} catch (Exception ex) {
				// save the exception to handle it in onPostExecute
				this.ex = ex;	
			}

			return user;
		}

		@Override
		protected void onPostExecute(User result) {

			// error handling
			if(ex != null){
				Log.e(TAG, "exception while verifying: " + ex);
				// user not authorized!
				if(ex instanceof TwitterException.E401){
				
					// tell the user that the login was not successful
					if(startTimeline>0){
						Intent timelineIntent = new Intent(LoginActivity.LOGIN_RESULT_ACTION);
						timelineIntent.putExtra(LoginActivity.LOGIN_RESULT, LoginActivity.LOGIN_FAILURE);
						sendBroadcast(timelineIntent);
					}
				} else {
					if(attempts>0){
						(new VerifyCredentialsTask()).execute(--attempts,startTimeline);
					} else {
						if(startTimeline>0){
							// tell the user that the login was not successful
							Intent timelineIntent = new Intent(LoginActivity.LOGIN_RESULT_ACTION);
							timelineIntent.putExtra(LoginActivity.LOGIN_RESULT, LoginActivity.LOGIN_FAILURE);
							sendBroadcast(timelineIntent);
						}
					}
				}
			} else {
				// this should not happen!
				if(result==null) {
					// if we still have more attempts, we start a new thread
					if(attempts>0){
						(new VerifyCredentialsTask()).execute(--attempts,startTimeline);
					} else {
						if(startTimeline>0){
							// tell the user that the login was not successful
							Intent timelineIntent = new Intent(LoginActivity.LOGIN_RESULT_ACTION);
							timelineIntent.putExtra(LoginActivity.LOGIN_RESULT, LoginActivity.LOGIN_FAILURE);
							sendBroadcast(timelineIntent);
						}

					}
					return;
				} else {
					// update user in DB
					updateUser(result,false);
					// store user Id and screenname in shared prefs
					LoginActivity.setTwitterId(Long.toString(result.getId()), getBaseContext());				
					LoginActivity.setTwitterScreenname(result.getScreenName(), getBaseContext());
					//synchTimeline(true);
					if(startTimeline>0){
						Intent timelineIntent = new Intent(LoginActivity.LOGIN_RESULT_ACTION);
						timelineIntent.putExtra(LoginActivity.LOGIN_RESULT, LoginActivity.LOGIN_SUCCESS);
						sendBroadcast(timelineIntent);
					}
				}
			}			
		}
	}

	/**
	 * Loads the mentions from twitter
	 * @author thossmann
	 *
	 */
	private class UpdateMentionsTask extends AsyncTask<Void, Void, List<Status>> {

		Exception ex;

		@Override
		protected List<winterwell.jtwitter.Status> doInBackground(Void... params) {
			Log.d(TAG, "AsynchTask: UpdateMentionsTask");
			ShowTweetListActivity.setLoading(true);

			List<winterwell.jtwitter.Status> mentions = null;

			twitter.setCount(Constants.NR_MENTIONS);			
			twitter.setSinceId(getMentionsSinceId(getBaseContext()));

			try {				  
				mentions = twitter.getMentions();	
				 
			} catch (Exception ex) {					
				this.ex = ex; // save the exception for later handling
			}

			return mentions;
		}

		@Override
		protected void onPostExecute(List<winterwell.jtwitter.Status> result) {
			ShowTweetListActivity.setLoading(false);
			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.RateLimit){					
					Log.e(TAG, "exception while loading mentions: " + ex);
				} else if(ex instanceof TwitterException.Timeout){
					if (ShowTweetListActivity.running)	
						Toast.makeText(getBaseContext(), "Timeout while loading mentions.", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "exception while loading mentions: " + ex);
				} else {
					Log.e(TAG, "exception while loading mentions: " + ex);
				}
				return;
			} else
				new InsertMentionsTask().execute(result);

		}

	}
	
	

	/**
	 * Asynchronously insert tweets into the mentions buffer
	 * @author thossmann
	 *
	 */
	private class InsertMentionsTask extends AsyncTask<List<winterwell.jtwitter.Status>, Void, Void> {

		@Override
		protected Void doInBackground(List<winterwell.jtwitter.Status>... params) {			
			ShowTweetListActivity.setLoading(true);
			List<winterwell.jtwitter.Status> tweetList = params[0];
			
			if(tweetList!=null && !tweetList.isEmpty()){
				BigInteger lastId = null;								
				
				ArrayList<ContentValues> cv = new ArrayList<ContentValues>();	  
			    ArrayList<ContentValues> users = new ArrayList<ContentValues>();	
				
			    boolean isFirstRound =true;
			    
				for (winterwell.jtwitter.Status tweet: tweetList) {
					if(lastId == null)
						lastId = tweet.getId();						
					
					if(tweet.getUser() != null){	
												
							users.add( getUserContentValues(tweet.getUser(),false));	
							cv.add( getTweetContentValues(tweet,null, Tweets.BUFFER_MENTIONS ) );												
						
					} 					
					if (cv.size() == 5) {						
						insertDataAndNotify(cv,users,isFirstRound, Tweets.TABLE_MENTIONS_URI);		
					    cv.clear();
					    users.clear();	
					    isFirstRound = false;
					}
				}
				if (cv.size() > 0) {
					insertDataAndNotify(cv,users,isFirstRound,Tweets.TABLE_MENTIONS_URI);		
				}

				// save the id of the last tweet for future timeline synchs
				setMentionsSinceId(lastId, getBaseContext());
			}

			// save the timestamp of the last update
			setLastMentionsUpdate(new Date(), getBaseContext());

			return null;
		}

		@Override
		protected void onPostExecute(Void params){
			ShowTweetListActivity.setLoading(false);
			getContentResolver().notifyChange(Tweets.TABLE_MENTIONS_URI, null);
			setTaskExecuted(TwitterService.TASK_MENTIONS, TwitterService.this);
		}

	}

	/**
	 * Updates the favorites
	 * @author thossmann
	 *
	 */
	private class UpdateFavoritesTask extends AsyncTask<Void, Void, List<winterwell.jtwitter.Status>> {

		Exception ex;

		@Override
		protected List<winterwell.jtwitter.Status> doInBackground(Void... params) {
			Log.d(TAG, "AsynchTask: UpdateFavoritesTask");
			ShowTweetListActivity.setLoading(true);
			List<winterwell.jtwitter.Status> favorites = null;

			twitter.setCount(Constants.NR_FAVORITES);			
			twitter.setSinceId(getFavoritesSinceId(getBaseContext()));


			try {
				favorites = twitter.getFavorites();
			} catch (Exception ex) {	
				this.ex = ex;
			}

			return favorites;
		}

		@Override
		protected void onPostExecute(List<winterwell.jtwitter.Status> result) {

			ShowTweetListActivity.setLoading(false);
			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.RateLimit){					
					Log.e(TAG, "exception while loading favorites: " + ex);
				} else if(ex instanceof TwitterException.Timeout){
					if (ShowTweetListActivity.running)							
						Toast.makeText(getBaseContext(), "Timeout while loading favorites.", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "exception while loading favorites: " + ex);
				}else {
					if (ShowTweetListActivity.running)	
						Toast.makeText(getBaseContext(), "Something went wrong when loading your favorites. Please try again later!", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "exception while loading favorites: " + ex);
				}
				return;
			} else
				new InsertFavoritesTask().execute(result);
		}

	}

	/**
	 * Asynchronously insert tweets into the favorites
	 * @author thossmann
	 *
	 */
	private class InsertFavoritesTask extends AsyncTask<List<winterwell.jtwitter.Status>, Void, Void> {

		@Override
		protected Void doInBackground(List<winterwell.jtwitter.Status>... params) {
			
			
			ShowTweetListActivity.setLoading(true);
			List<winterwell.jtwitter.Status> tweetList = params[0];
			if(tweetList!=null && !tweetList.isEmpty()){
				BigInteger lastId = null;
					
			
				ArrayList<ContentValues> cv = new ArrayList<ContentValues>();	  
			    ArrayList<ContentValues> users = new ArrayList<ContentValues>();	
				
			    boolean isFirstRound =true;
				for (winterwell.jtwitter.Status tweet: tweetList) {
					if(lastId == null)
						lastId = tweet.getId();

					if(tweet.getUser() != null){													
							users.add( getUserContentValues(tweet.getUser(),false));	
							cv.add( getTweetContentValues(tweet,null, Tweets.BUFFER_FAVORITES) );						
						
					} 					
					if (cv.size()==4) {
						insertDataAndNotify(cv,users,isFirstRound,Tweets.TABLE_FAVORITES_URI);						
					    cv.clear();
					    users.clear();
					    isFirstRound = false;
					}
				}
				if (cv.size() > 0) {
					insertDataAndNotify(cv,users,isFirstRound,Tweets.TABLE_FAVORITES_URI);		
				}

				// save the id of the last tweet for future timeline synchs
				setFavoritesSinceId(lastId, getBaseContext());
			}

			// save the timestamp of the last update
			setLastFavoritesUpdate(new Date(), getBaseContext());

			return null;
		}

		

		@Override
		protected void onPostExecute(Void params){
			ShowTweetListActivity.setLoading(false);
			getContentResolver().notifyChange(Tweets.TABLE_FAVORITES_URI, null);
		}

	}
	
	private void insertDataAndNotify(ArrayList<ContentValues> cv,
			ArrayList<ContentValues> users, boolean isFirstRound, Uri notify) {			
		
		updateTweets( cv.toArray(new ContentValues[0]) );
		if (users.size()>0) {			
			updateUsers( users.toArray(new ContentValues[0]) );
			new SynchTransactionalUsersTask(true).execute(false);
		}
		if (isFirstRound) getContentResolver().notifyChange(notify, null);
		
		
	}

	/**
	 * Loads the timeline from twitter
	 * @author thossmann
	 *
	 */
	private class UpdateTimelineTask extends AsyncTask<Integer, Void, List<winterwell.jtwitter.Status>> {

		Exception ex = null;
		int attempts_left;
		int overscroll= OVERSCROLL_TOP;

		@Override
		protected List<winterwell.jtwitter.Status> doInBackground(Integer... params) {
			Thread.currentThread().setName("UpdateTimelineTask");
						
			ShowTweetListActivity.setLoading(true);
			attempts_left= params[0];
			
			if (params.length>1)
				overscroll = params[1];
			
			List<winterwell.jtwitter.Status> timeline = null;
			twitter.setCount(Constants.NR_TWEETS);
			if (overscroll == OVERSCROLL_BOTTOM) {				
				twitter.setUntilId(getTimelineUntilId(getBaseContext()));
			} else {
				twitter.setSinceId(getTimelineSinceId(getBaseContext()));
				//twitter.setSinceId(null);
			}

			try {				
				timeline = twitter.getHomeTimeline();
				
				if (timeline.size()>0 && overscroll == OVERSCROLL_BOTTOM ) {
					Constants.TIMELINE_BUFFER_SIZE += 50;
					Log.i(TAG, "BUFFER_SIZE =  "+ Constants.TIMELINE_BUFFER_SIZE);
				}				
				Log.i(TAG,"timeline size:" + timeline.size());
			} catch (Exception ex) {
				this.ex = ex;
			}

			return timeline;
		}

		@Override
		protected void onPostExecute(List<winterwell.jtwitter.Status> result) {

			ShowTweetListActivity.setLoading(false);

			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.RateLimit){					
					Log.e(TAG, "exception while loading timeline: " + ex);
				} else if(ex instanceof TwitterException.Timeout){
					if (ShowTweetListActivity.running)	
						Toast.makeText(getBaseContext(), "Timeout while loading timeline.", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "exception while loading timeline: " + ex);
					if (attempts_left > 0)
						new UpdateTimelineTask().execute(--attempts_left);
				}else {
					if (ShowTweetListActivity.running)	
						Toast.makeText(getBaseContext(), "Something went wrong when loading your timeline. Please try again later!", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "exception while loading timeline: " + ex);
					if (attempts_left > 0)
						new UpdateTimelineTask().execute(--attempts_left);
				}				
				return;
			}
			else 
				new InsertTimelineTask(overscroll).execute(result);




		}

	}

	/**
	 * Asynchronously insert tweets into the timeline
	 * @author thossmann
	 *
	 */
	private class InsertTimelineTask extends AsyncTask<List<winterwell.jtwitter.Status>, Void, Void> {
		
		boolean setTimelineSinceId;
		
		public InsertTimelineTask(int overscrollType) {
			if (overscrollType == OVERSCROLL_TOP) 
				setTimelineSinceId = true;
			else
				setTimelineSinceId = false;
		}

		@Override
		protected Void doInBackground(List<winterwell.jtwitter.Status>... params) {
			
			Thread.currentThread().setName("InsertTimelineTask");			
			
			ShowTweetListActivity.setLoading(true);
			
			List<winterwell.jtwitter.Status> tweetList = params[0];
			
			if(tweetList!=null && !tweetList.isEmpty()){
				BigInteger lastId = null; 
				long timestamp = System.currentTimeMillis();
				
			    ArrayList<ContentValues> cv = new ArrayList<ContentValues>();	  
			    ArrayList<ContentValues> users = new ArrayList<ContentValues>();	
			    ArrayList<ContentValues> allUsers = new ArrayList<ContentValues>();	
			    
			    boolean firstRound = true;
				for (winterwell.jtwitter.Status tweet: tweetList) {
					if(lastId == null) {
						lastId = tweet.getId();						
					}
					
					//is the tweet a retweet ?
					String ret_screenName = null;
					if (tweet.getOriginal()!= null) {
						//yes, get the original one
						ret_screenName = tweet.getUser().getScreenName();
						tweet = tweet.getOriginal();						
					}
					if (!contains(allUsers,tweet.getUser())) {
						if (ret_screenName == null)	{	
							ContentValues userCv = getUserContentValues(tweet.getUser(),true);
							users.add( userCv);	
							allUsers.add( userCv);	
						}
						else {	
							ContentValues userCv = getUserContentValues(tweet.getUser(),false);
							users.add( userCv);	
							allUsers.add( userCv);	
						}
					} 													
					cv.add( getTweetContentValues(tweet,ret_screenName, Tweets.BUFFER_TIMELINE) );												

					if (cv.size() == 5) {						
						insertDataAndNotify(cv,users,firstRound, Tweets.TABLE_TIMELINE_URI);		
						cv.clear();
						users.clear();
						firstRound = false;
					}					
				}
				
				if (cv.size() > 0) {
					insertDataAndNotify(cv,users,firstRound, Tweets.TABLE_TIMELINE_URI);		
				}
				
				// save the id of the last tweet for future timeline synchs
				if (setTimelineSinceId)
					setTimelineSinceId(lastId, getBaseContext());	
		    // save the timestamp of the last update
			setLastTimelineUpdate(timestamp,getBaseContext());
			}
			return null;
		}



		@Override
		protected void onPostExecute(Void params){
			ShowTweetListActivity.setLoading(false);	
			getContentResolver().notifyChange(Tweets.TABLE_TIMELINE_URI, null);
			
			StartServiceHelper.startService(TwitterService.this);

			Log.i(TAG,"Insert onPost Execute");
		}

	}

	private boolean contains(ArrayList<ContentValues> usersCv, User incomingUser) {

		boolean result = false;

		for (ContentValues cv: usersCv){			
			if(cv.getAsLong(TwitterUsers.COL_TWITTERUSER_ID).longValue() == incomingUser.id.longValue()){				
				result = true;
				
				break;		
			}
		}
		return result;
		
	}

	/**
	 * Loads the most recent tweets of a user
	 * @author thossmann
	 *
	 */
	private class UpdateUserTweetsTask extends AsyncTask<String, Void, List<winterwell.jtwitter.Status>> {

		Exception ex;

		@Override
		protected List<winterwell.jtwitter.Status> doInBackground(String... params) {
			
			ShowUserTweetListActivity.setLoading(true);
			
			String screenname = params[0];

			List<winterwell.jtwitter.Status> userTweets = null;
			twitter.setCount(null);
			twitter.setSinceId(null);


			try {
				userTweets = twitter.getUserTimeline(screenname);
			} catch (Exception ex) {
				this.ex = ex;
			}

			return userTweets;
		}

		@Override
		protected void onPostExecute(List<winterwell.jtwitter.Status> result) {

			ShowUserTweetListActivity.setLoading(false);
			
			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.RateLimit){					
					Log.e(TAG, "exception while updating user: " + ex);
				} else {
					if (ShowTweetListActivity.running)						
						Toast.makeText(getBaseContext(), "Something went wrong while loading the timeline. Please try again later!", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "exception while updating user: " + ex);
				}
				return;
			} else
				new InsertUserTweetsTask().execute(result);
		}
	}
	
	/**
	 * Asynchronously insert tweets of a user into the respective buffer
	 * @author thossmann
	 *
	 */
	private class InsertUserTweetsTask extends AsyncTask<List<winterwell.jtwitter.Status>, Void, Void> {

		@Override
		protected Void doInBackground(List<winterwell.jtwitter.Status>... params) {

			ShowUserTweetListActivity.setLoading(true);
			
			List<winterwell.jtwitter.Status> tweetList = params[0];
			
			if(tweetList!=null && !tweetList.isEmpty()){
				int i =0;
				
				ArrayList<ContentValues> cv = new ArrayList<ContentValues>();
				
				for (winterwell.jtwitter.Status tweet: tweetList) {
					
					if(tweet.getUser()!=null){
						
						//is the tweet a retweet ?
						String ret_screenName = null;
						if (tweet.getOriginal()!= null) {
							//yes, get the original one
							ret_screenName = tweet.getUser().getScreenName();
							tweet = tweet.getOriginal();						
						}
						cv.add( getTweetContentValues(tweet,ret_screenName, Tweets.BUFFER_USERS) );				
						
					} 
					i++;
					
					if (i == 5) {	
						updateTweets( cv.toArray(new ContentValues[0]) );						
						getContentResolver().notifyChange(Tweets.TABLE_USER_URI, null);						
						cv.clear();
						i=0;
					}
				}
				
				
				if (cv.size() > 0) {
					updateTweets( cv.toArray(new ContentValues[0]));
					getContentResolver().notifyChange(Tweets.TABLE_USER_URI, null);	
				}
			}			
					
			ShowUserTweetListActivity.setLoading(false);
			
			return null;
		}
	}
	
	
	/**
	 * Loads tweet search results from Twitter
	 * @author thossmann
	 *
	 */
	private class SearchTweetsTask extends AsyncTask<String, Void, List<winterwell.jtwitter.Status>> {

		Exception ex;

		@Override
		protected List<winterwell.jtwitter.Status> doInBackground(String... params) {
			Log.i(TAG, "AsynchTask: SearchTweetsTask");

			SearchableActivity.setLoading(true);
			
			String query = params[0];

			List<winterwell.jtwitter.Status> searchTweets = null;
			twitter.setMaxResults(Constants.NR_SEARCH_TWEETS);
			
			try {
				searchTweets = twitter.search(query);	
				Log.i(TAG,"search tweets size = " + searchTweets.size());
			} catch (Exception ex) {
				this.ex = ex;
			}

			return searchTweets;
		}

		@Override
		protected void onPostExecute(List<winterwell.jtwitter.Status> result) {

			SearchableActivity.setLoading(false);
			
			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.RateLimit){					
					Log.e(TAG, "exception while updating user: " + ex);
				} else {
					if (ShowTweetListActivity.running)						
						Toast.makeText(getBaseContext(), "Something went wrong while searching. Please try again later!", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "exception while searching: " + ex);
				}
				return;
			} else
				new InsertSearchTweetsTask().execute(result);
		}
	}
	

	/**
	 * Loads user search results from Twitter
	 * @author pcarta
	 *
	 */
	private class SearchUsersTask extends AsyncTask<String, Void, List<User>> {

		Exception ex;

		@Override
		protected List<winterwell.jtwitter.User> doInBackground(String... params) {
			Log.v(TAG, "AsynchTask: SearchTweetsTask");

			SearchableActivity.setLoading(true);
			
			String query = params[0];

			List<winterwell.jtwitter.User> searchTweets = null;			
			
			try {
				searchTweets = twitter.users().searchUsers(query);
			} catch (Exception ex) {
				this.ex = ex;
			}

			return searchTweets;
		}

		@Override
		protected void onPostExecute(List<winterwell.jtwitter.User> result) {

			SearchableActivity.setLoading(false);
			
			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.RateLimit){				
					Log.e(TAG, "exception while updating user: " + ex);
				} else {
					if (ShowTweetListActivity.running)						
						Toast.makeText(getBaseContext(), "Something went wrong while searching. Please try again later!", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "exception while searching: " + ex);
				}
				return;
			} else
				 new InsertSearchUsersTask().execute(result);
		}

		
	}
	
	
	/**
	 * Asynchronously insert search result tweets into the respective buffer
	 * @author thossmann
	 *
	 */
	private class InsertSearchTweetsTask extends AsyncTask<List<winterwell.jtwitter.Status>, Void, Void> {

		@Override
		protected Void doInBackground(List<winterwell.jtwitter.Status>... params) {

			SearchableActivity.setLoading(true);			
			List<winterwell.jtwitter.Status> tweetList = params[0];
			if (D) Log.i(TAG,"search results: " + tweetList.size());
			
			if(tweetList!=null && !tweetList.isEmpty()){
				double i = 0;
				ArrayList<ContentValues> cv = new ArrayList<ContentValues>();	  
			    ArrayList<ContentValues> users = new ArrayList<ContentValues>();				
			   
			    boolean isFirstRound =true; 
				for (winterwell.jtwitter.Status tweet: tweetList) {
					
					
					if (tweet.getOriginal()!= null) {						
						tweet = tweet.getOriginal();						
					}
					
					if(tweet.getUser() != null){
						if (tweet.getUser().getScreenName() != null ) {
							
							users.add( getUserContentValues(tweet.getUser(),false));	
							cv.add( getTweetContentValues(tweet,null, Tweets.BUFFER_SEARCH) );								
						} 
					} 
					i++;
					if (i == 5) {						
						insertDataAndNotify(cv,users,isFirstRound,Tweets.TABLE_SEARCH_URI);		
					    cv.clear();
					    users.clear();
						i=0;
						isFirstRound = false;
					}
				}
				insertDataAndNotify(cv,users,isFirstRound,Tweets.TABLE_SEARCH_URI);	
				 
			}					
			SearchableActivity.setLoading(false);
			
			return null;
		}
	}
	
	/**
	 * Asynchronously inserts list of users obtained by the search op into the DB
	 * @author pcarta
	 *
	 */
	private class InsertSearchUsersTask extends AsyncTask<List<User>, Void, Void>{

		@Override
		protected Void doInBackground(List<User>... params) {

			ShowUserListActivity.setLoading(true);
			
			List<User> result = params[0];

			if(result==null || result.isEmpty()){
				return null;
			}
			
			ArrayList<ContentValues> users = new ArrayList<ContentValues>();	

			for (User user: result) {

				ContentValues cv= getUserContentValues(user,false);
				cv.put(TwitterUsers.COL_LASTUPDATE, System.currentTimeMillis());				
				cv.put(TwitterUsers.COL_IS_SEARCH_RESULT,1);

				users.add(cv );					

				if (users.size()==5) {
					updateUsers( users.toArray(new ContentValues[0]) );
					getContentResolver().notifyChange(TwitterUsers.USERS_SEARCH_URI, null);
					new SynchTransactionalUsersTask(true).execute(false);
					users.clear();					
				}			

			}
			if (users.size()>0) {
				updateUsers( users.toArray(new ContentValues[0]) );
				getContentResolver().notifyChange(TwitterUsers.USERS_SEARCH_URI, null);
				new SynchTransactionalUsersTask(true).execute(false);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void params){

			ShowUserListActivity.setLoading(false);
			
			// trigger the user synch (for updating the profile images)
			new SynchTransactionalUsersTask(false).execute(false);

			getContentResolver().notifyChange(TwitterUsers.CONTENT_URI, null);

		}

	}
	
	

	/**
	 * Updates the list of friends
	 * @author thossmann
	 *
	 */
	private class UpdateFriendsTask extends AsyncTask<Long, Void, List<Number>> {

		Exception ex;
		long notify;

		@Override
		protected List<Number> doInBackground(Long... params) {
			Log.d(TAG, "AsynchTask: UpdateFriendsTask");
			ShowUserListActivity.setLoading(true);
			this.notify= params[0];
			
			List<Number> friendsList = null;

			try {
				friendsList = twitter.users().getFriendIDs();

			} catch (Exception ex) {
				this.ex = ex;
			}

			return friendsList;
		}

		@Override
		protected void onPostExecute(List<Number> result) {
			ShowTweetListActivity.setLoading(false);

			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.RateLimit){					
					Log.e(TAG, "exception while loading friends: " + ex);
				} else if(ex instanceof TwitterException.Timeout){
					if (ShowTweetListActivity.running && notify == TRUE)						
						Toast.makeText(getBaseContext(), "Timeout while loading friends.", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "exception while loading friends: " + ex);
				}else {
					if (ShowTweetListActivity.running && notify == TRUE)
						Toast.makeText(getBaseContext(), "Something went wrong when loading your friends. Please try again later!", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "exception while loading followers: " + ex);
				}
				return;
			}

			new LoadFriendsTask().execute(result);
		}

	}

	/**
	 * Asynchronously bulk load list of friends
	 * @author thossmann
	 *
	 */
	private class LoadFriendsTask extends AsyncTask<List<Number>, Void, List<User>>{

	
		
		@Override
		protected List<User> doInBackground(List<Number>... params) {

			ShowUserListActivity.setLoading(true);
			
			List<Number> result = params[0];
			// no friends to insert
			if(result==null) return null;


			// this is the list of user IDs we will request updates for
			List<Long> toLookup = new ArrayList<Long>();
			List<User> userList = null;

			if(!result.isEmpty()){
				for (Number userId: result) {
					// we insert/update at most 100 users 
					if(toLookup.size()<100){
						toLookup.add((Long) userId);
					}
				}
				try {
					userList = twitter.users().showById(toLookup);
					//userList = twitter.bulkShowById(toLookup);

				} catch (Exception ex) {
					
				} 
			}
			
			return userList;
		}

		@Override
		protected void onPostExecute(List<User> toInsert){

			ShowUserListActivity.setLoading(false);
			
			if(toInsert == null) return;
			
			// save the timestamp of the last update
			setLastFriendsUpdate(new Date(), getBaseContext());

			// if we have users to lookup, we do it now
			if(!toInsert.isEmpty()){
				new InsertFriendsListTask().execute(toInsert);
			}
			return;
		}
	}
	
	/**
	 * Asynchronously inserts a list of friends users into the DB
	 * @author theus
	 *
	 */
	private class InsertFriendsListTask extends AsyncTask<List<User>, Void, Void>{
	
		@Override
		protected Void doInBackground(List<User>... params) {
			Log.i(TAG,"insert friends task");
			ShowUserListActivity.setLoading(true);
			
			List<User> result = params[0];

			if(result==null || result.isEmpty()){
				return null;
			}			
			ArrayList<ContentValues> users = new ArrayList<ContentValues>();	
			
			for (User user: result) {
				
				ContentValues cv = getUserContentValues(user,true);
				if (cv !=null) 
					cv.put(TwitterUsers.COL_LASTUPDATE, System.currentTimeMillis());			
				users.add(cv );					
				
				if (users.size()==5) {
					updateUsers( users.toArray(new ContentValues[0]) );
					getContentResolver().notifyChange(TwitterUsers.USERS_FRIENDS_URI, null);
					new SynchTransactionalUsersTask(true).execute(false);
					users.clear();					
				}

			}
			if (users.size()>0) {
				updateUsers( users.toArray(new ContentValues[0]) );
				getContentResolver().notifyChange(TwitterUsers.USERS_FRIENDS_URI, null);
				new SynchTransactionalUsersTask(true).execute(false);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void params){
			ShowUserListActivity.setLoading(false);		

		}

	}

	/**
	 * Updates the list of followers
	 * @author thossmann
	 *
	 */
	private class UpdateFollowersTask extends AsyncTask<Long, Void, List<Number>> {

		Exception ex;
		long notify;
		
		@Override
		protected List<Number> doInBackground(Long... params) {
			Log.d(TAG, "AsynchTask: UpdateFollowersTask");
			ShowUserListActivity.setLoading(true);
			this.notify= params[0];
			
			List<Number> followersList = null;

			try {
				followersList = twitter.users().getFollowerIDs();

			} catch (Exception ex) {
				this.ex = ex;
			}

			return followersList;
		}

		@Override
		protected void onPostExecute(List<Number> result) {
			ShowTweetListActivity.setLoading(false);

			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.RateLimit){				
					Log.e(TAG, "exception while loading followers: " + ex);
				} else if(ex instanceof TwitterException.Timeout){
					if (ShowTweetListActivity.running && notify == TRUE)						
						Toast.makeText(getBaseContext(), "Timeout while loading followers.", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "exception while loading followers: " + ex);
				}else {
					if (ShowTweetListActivity.running && notify == TRUE)
						Toast.makeText(getBaseContext(), "Something went wrong when loading your followers. Please try again later!", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "exception while loading followers: " + ex);
				}
				return;
			}

			new LoadFollowersTask().execute(result);
		}

	}

	/**
	 * Asynchronously bulk load list of followers
	 * @author thossmann
	 *
	 */
	private class LoadFollowersTask extends AsyncTask<List<Number>, Void, List<User>>{

		Exception ex;
		
		@Override
		protected List<User> doInBackground(List<Number>... params) {

			ShowUserListActivity.setLoading(true);
			
			List<Number> result = params[0];
			// no friends to insert
			if(result==null) return null;


			// this is the list of user IDs we will request updates for
			List<Long> toLookup = new ArrayList<Long>();
			List<User> userList = null;

			if(!result.isEmpty()){
				for (Number userId: result) {
					// we insert/update at most 100 users 
					if(toLookup.size()<100){
						toLookup.add((Long) userId);
					}
				}

				try {
					userList = twitter.users().showById(toLookup);

				} catch (Exception ex) {
					this.ex = ex;
				} 

			}
			
			return userList;


		}

		@Override
		protected void onPostExecute(List<User> toInsert){

			ShowUserListActivity.setLoading(false);
			
			if (toInsert != null) {
				// save the timestamp of the last update
				setLastFollowerUpdate(new Date(), getBaseContext());

				// if we have users to lookup, we do it now
				if(!toInsert.isEmpty()){
					new InsertFollowersListTask().execute(toInsert);
				}
			}
			
			return;
		}
	}
	
	/**
	 * Asynchronously inserts a list of followers users into the DB
	 * @author theus
	 *
	 */
	private class InsertFollowersListTask extends AsyncTask<List<User>, Void, Void>{

		@Override
		protected Void doInBackground(List<User>... params) {

			ShowUserListActivity.setLoading(true);
			
			List<User> result = params[0];

			if(result==null || result.isEmpty()){
				return null;
			}			
			
			ContentValues cv = new ContentValues();
			ArrayList<ContentValues> users = new ArrayList<ContentValues>();				
			
			for (User user: result) {

				cv= getUserContentValues(user,false);
				cv.put(TwitterUsers.COL_LASTUPDATE, System.currentTimeMillis());
				cv.put(TwitterUsers.COL_ISFOLLOWER, 1);
				users.add(cv);				
				
				if (users.size()==5){
					updateUsers( users.toArray(new ContentValues[0]) );
					getContentResolver().notifyChange(TwitterUsers.USERS_FOLLOWERS_URI, null);
					new SynchTransactionalUsersTask(true).execute(false);
					users.clear();					
				}
			}
			
			if (users.size()>0) {
				
				updateUsers( users.toArray(new ContentValues[0]) );
				getContentResolver().notifyChange(TwitterUsers.USERS_FOLLOWERS_URI, null);
				new SynchTransactionalUsersTask(true).execute(false);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void params){
			ShowUserListActivity.setLoading(false);		

		}

	}


		
	/**
	 * Post a tweet to twitter
	 * @author thossmann
	 */
	private class UpdateStatusTask extends AsyncTask<Long, Void, winterwell.jtwitter.Status> {

		long attempts,notify;
		long rowId;
		int flags;
		int buffer;
		String mediaName = null;
		Exception ex;

		@Override
		protected winterwell.jtwitter.Status doInBackground(Long... rowId) {
			
			ShowTweetListActivity.setLoading(true);
			this.rowId = rowId[0];
			this.attempts = rowId[1];
			this.notify= rowId[2];
			
			winterwell.jtwitter.Status tweet = null;
			Cursor c = null;

			try {

				Uri queryUri = Uri.parse("content://"+Tweets.TWEET_AUTHORITY+"/"+Tweets.TWEETS+"/"+this.rowId);
				c = getContentResolver().query(queryUri, null, null, null, null);

				if(c.getCount() == 0){
					return null;
				}
				c.moveToFirst();
				flags = c.getInt(c.getColumnIndex(Tweets.COL_FLAGS));
				buffer = c.getInt(c.getColumnIndex(Tweets.COL_BUFFER));

				String text = c.getString(c.getColumnIndex(Tweets.COL_TEXT_PLAIN));
				mediaName = c.getString(c.getColumnIndex(Tweets.COL_MEDIA));
				String mediaUrl =  null;
				
				if (mediaName != null)
					mediaUrl = Environment.getExternalStoragePublicDirectory(Tweets.PHOTO_PATH +
												"/" + LoginActivity.getTwitterId(TwitterService.this) + "/" + mediaName).getAbsolutePath();
				
				boolean hasMedia;
				if(mediaUrl != null)
					hasMedia = true;
				else	
					hasMedia = false;				

				if(!(c.getDouble(c.getColumnIndex(Tweets.COL_LAT))==0 && c.getDouble(c.getColumnIndex(Tweets.COL_LNG))==0)){
					double[] location = {c.getDouble(c.getColumnIndex(Tweets.COL_LAT)),c.getDouble(c.getColumnIndex(Tweets.COL_LNG))}; 
					twitter.setMyLocation(location);
				} else {
					twitter.setMyLocation(null);
				}
				
				if(c.getColumnIndex(Tweets.COL_REPLYTO)>=0){
					if(hasMedia){
						Log.d("upload", "upload media with reply");
						BigInteger replyToId = BigInteger.valueOf(c.getLong(c.getColumnIndex(Tweets.COL_REPLYTO)));
						tweet = twitter.updateStatusWithMedia(text, replyToId, new File(mediaUrl));
					}
					else{
						tweet = twitter.updateStatus(text, c.getLong(c.getColumnIndex(Tweets.COL_REPLYTO)));
					}
				} else {
					if(hasMedia){
						Log.d("upload", "upload media without reply");
						tweet = twitter.updateStatusWithMedia(text, null, new File(mediaUrl));
					}
					else{
						tweet = twitter.updateStatus(text);
					}
					
				}

			} catch(Exception ex) { 
				this.ex = ex;
			} finally {
				if(c!=null) c.close();
			}
			return tweet;
		}

		/**
		 * Clear to insert flag and update the tweet with the information from twitter
		 */
		@Override
		protected void onPostExecute(winterwell.jtwitter.Status result) {
			ShowTweetListActivity.setLoading(false);

			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.Repetition){					
					Log.w(TAG, "exception while posting tweet, Tweet already posted: " + ex);
					// we stil clear the flag
				} else if(ex instanceof TwitterException.Unexplained){
					// we get unexplained exceptions if what twitter returns does not match what we have sent.
					// this does not have to be an error, it happens if we post a url, for example.
					Log.w(TAG, "unexplained exception while posting tweet (maybe it contained a url): " + ex);
					
				} else if(ex instanceof TwitterException.E401){
					Log.w(TAG, "exception while posting tweet: " + ex);
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts,notify};
						(new UpdateStatusTask()).execute(params);
						return;
					} else {
						if (ShowTweetListActivity.running && notify == TRUE)						
						Toast.makeText(getBaseContext(), "Something went wrong while posting your tweet.", Toast.LENGTH_SHORT).show();
						return;
					}
				} else if(ex instanceof TwitterException.Timeout){
					Log.w(TAG, "exception while posting tweet: " + ex);
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts,notify};
						(new UpdateStatusTask()).execute(params);
						return;
					} else {
						if (ShowTweetListActivity.running && notify == TRUE)						
						Toast.makeText(getBaseContext(), "Timeout while posting tweet your tweet.", Toast.LENGTH_SHORT).show();
						return;
					}
				} else {
					if (ShowTweetListActivity.running && notify == TRUE)						
						Toast.makeText(getBaseContext(), "Something went wrong while posting your tweet. Please try again later!", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "exception while posting tweet: " + ex);
					return;
				}
			}			
			
			Uri queryUri = Uri.parse("content://"+Tweets.TWEET_AUTHORITY+"/"+Tweets.TWEETS+"/"+this.rowId);

			ContentValues cv = null;
			// if we had a result, we get the new values. Otherwise we simply clear the flags.
			if(result != null){
				cv = getTweetContentValues(result,null, 0);
			} else {
				cv = new ContentValues();
			}
			cv.put(Tweets.COL_FLAGS, flags & ~(Tweets.FLAG_TO_INSERT));
			cv.put(Tweets.COL_BUFFER, buffer);

			// TODO: Move this in async task
			
			try{
				getContentResolver().update(queryUri, cv, null, null);
			} catch (Exception ex){
				Log.e(TAG, "Exception while updating tweet in DB");
			}
			
			if (ShowTweetListActivity.running)
						if (ShowTweetListActivity.running)
						Toast.makeText(getBaseContext(), "Tweet posted", Toast.LENGTH_SHORT).show();

		}

	}

	/**
	 * Delete a tweet from twitter and from the content provider
	 * @author thossmann
	 */
	private class DestroyStatusTask extends AsyncTask<Long, Void, Integer> {

		long attempts;
		long rowId;
		
		long notify;

		Exception ex;

		@Override
		protected Integer doInBackground(Long... rowId) {
			Log.d(TAG, "AsynchTask: DestroyStatusTask");
			ShowTweetListActivity.setLoading(true);
			this.rowId = rowId[0];
			this.attempts = rowId[1];
			this.notify= rowId[2];
			
			Integer result = null;

			Cursor c = null;

			try {
				
				Uri queryUri = Uri.parse("content://"+Tweets.TWEET_AUTHORITY+"/"+Tweets.TWEETS+"/"+this.rowId);
				c = getContentResolver().query(queryUri, null, null, null, null);

				// making sure the tweet was found in the content provider
				if(c.getCount() == 0){
					Log.w(TAG, "DestroyStatusTask: Tweet not found " + this.rowId);
					ex = new Exception();
					c.close();
					return null;
				}
				c.moveToFirst();			
				
				twitter.destroyStatus(c.getLong(c.getColumnIndex(Tweets.COL_TID)));
				result = 1;
			} catch (TwitterException ex) {
				this.ex = ex;
			} finally {
				if(c!=null) c.close();
			}

			return result;
		}

		/**
		 * If successful, we delete the tweet also locally. If not successful, we keep the tweet and clear the todelete flag
		 */
		@Override
		protected void onPostExecute(Integer result) {
			ShowTweetListActivity.setLoading(false);

			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.Repetition){					
					Uri deleteUri = Uri.parse("content://"+Tweets.TWEET_AUTHORITY+"/"+Tweets.TWEETS+"/"+this.rowId);				
					getContentResolver().delete(deleteUri, null, null);
					
				} else if(ex instanceof TwitterException.E401){				
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts, notify};
						(new DestroyStatusTask()).execute(params);
						return;
					} else {						
						if (ShowTweetListActivity.running && notify == TRUE)
							Toast.makeText(getBaseContext(), "Something went wrong while deleting your tweet. We will try again later!", Toast.LENGTH_SHORT).show();
						return;
					}
				} else if(ex instanceof TwitterException.Timeout){
					
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts, notify};
						(new DestroyStatusTask()).execute(params);
						return;
					} else {						
						if (ShowTweetListActivity.running && notify == TRUE)
							Toast.makeText(getBaseContext(), "Timeout while deleting tweet. We will try again later!", Toast.LENGTH_SHORT).show();
						return;
					}
				}else if (ex instanceof TwitterException.E404) {
					
					Uri deleteUri = Uri.parse("content://"+Tweets.TWEET_AUTHORITY+"/"+Tweets.TWEETS+"/"+this.rowId);				
					getContentResolver().delete(deleteUri, null, null);
					
				}else {
					// an exception happended, we notify the user					
					if (ShowTweetListActivity.running && notify == TRUE)					
						Toast.makeText(getBaseContext(), "Something went wrong while deleting. We will try again later!", Toast.LENGTH_LONG).show();
					
					return;
				}
			}
		    // TODO: Move this to async task
		    else {
				Uri deleteUri = Uri.parse("content://"+Tweets.TWEET_AUTHORITY+"/"+Tweets.TWEETS+"/"+this.rowId);				
				getContentResolver().delete(deleteUri, null, null);
				if (ShowTweetListActivity.running)					
						Toast.makeText(getBaseContext(), "Delete successful.", Toast.LENGTH_SHORT).show();
				
			}
		}

	}
	
	
	/**
	 * Delete a tweet from twitter and from the content provider
	 * @author thossmann
	 */
	private class DestroyMessageTask extends AsyncTask<Long, Void, Integer> {

		long attempts;
		long rowId;
	
		long notify;

		Exception ex;

		@Override
		protected Integer doInBackground(Long... params) {
			Log.d(TAG, "AsynchTask: DestroyStatusTask");
			ShowTweetListActivity.setLoading(true);
			this.rowId = params[0];
			this.attempts = params[1];
			this.notify= params[2];
			
			Integer result = null;

			Cursor c = null;

			try {
				
				Uri queryUri = Uri.parse("content://"+DirectMessages.DM_AUTHORITY+"/"+DirectMessages.DMS+"/"+ rowId);
				c = getContentResolver().query(queryUri, null, null, null, null);

				// making sure the tweet was found in the content provider
				if(c.getCount() == 0){
					Log.w(TAG, "DestroyStatusTask: Msg not found " + this.rowId);
					ex = new Exception();
					c.close();
					return null;
				}				
				c.moveToFirst();				
			
				twitter.destroyMessage(c.getLong(c.getColumnIndex(DirectMessages.COL_DMID)));
				Log.i(TAG,"destroy executed");
				result = 1;
			} catch (TwitterException ex) {
				this.ex = ex;
			} finally {
				if(c!=null) c.close();
				Log.i(TAG,"cursor closed");
			}

			return result;
		}

		/**
		 * If successful, we delete the tweet also locally. If not successful, we keep the tweet and clear the todelete flag
		 */
		@Override
		protected void onPostExecute(Integer result) {
			
			ShowTweetListActivity.setLoading(false);
			Uri deleteUri = Uri.parse("content://"+DirectMessages.DM_AUTHORITY+"/"+DirectMessages.DMS+"/"+ rowId);
			Log.i(TAG,"on post ex");
			
			// error handling
			if(ex != null){
				Log.i(TAG,"ex != null");
				if(ex instanceof TwitterException.Repetition){	
					Log.i(TAG,"repetition");
					getContentResolver().delete(deleteUri, null, null);
					
				} else if(ex instanceof TwitterException.E401){
					Log.i(TAG,"TwitterException.E401");
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts,notify};
						(new DestroyMessageTask()).execute(params);
						return;
					} else {						
						if (ShowTweetListActivity.running && notify == TRUE)
							Toast.makeText(getBaseContext(), "Something went wrong while deleting your message. We will try again later!", Toast.LENGTH_SHORT).show();
						return;
					}
				} else if(ex instanceof TwitterException.Timeout){
					Log.i(TAG,"Timeout");
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts,notify};
						(new DestroyMessageTask()).execute(params);
						return;
					} else {						
						if (ShowTweetListActivity.running && notify == TRUE)
							Toast.makeText(getBaseContext(), "Timeout while deleting message. We will try again later!", Toast.LENGTH_SHORT).show();
						return;
					}
				}else if (ex instanceof TwitterException.E404) {
					Log.i(TAG,"E404");
					getContentResolver().delete(deleteUri, null, null);
					
				}else {
					Log.i(TAG,"error");
					// an exception happended, we notify the user					
					if (ShowTweetListActivity.running && notify == TRUE)					
						Toast.makeText(getBaseContext(), "Something went wrong while deleting. We will try again later!", Toast.LENGTH_LONG).show();
					
					return;
				}
			}
		    // TODO: Move this to async task
		    else {
		    	Log.i(TAG,"else");			
				getContentResolver().delete(deleteUri, null, null);
				if (ShowTweetListActivity.running && notify == TRUE)					
						Toast.makeText(getBaseContext(), "Delete successful.", Toast.LENGTH_SHORT).show();
				
			}
		}

	}

	/**
	 * Favorite a tweet on twitter and clear the respective flag locally
	 * @author thossmann
	 */
	private class FavoriteStatusTask extends AsyncTask<Long, Void, Integer> {

		long attempts,notify;
		long rowId;
		int flags;
		int buffer;

		Exception ex;

		@SuppressWarnings("deprecation")
		@Override
		protected Integer doInBackground(Long... rowId) {
			Log.d(TAG, "AsynchTask: FavoriteStatusTask");
			ShowTweetListActivity.setLoading(true);
			this.rowId = rowId[0];
			this.attempts = rowId[1];
			this.notify= rowId[2];
			
			Integer result = null;
			Cursor c = null;

			try {
				
				Uri queryUri = Uri.parse("content://"+Tweets.TWEET_AUTHORITY+"/"+Tweets.TWEETS+"/"+this.rowId);
				c = getContentResolver().query(queryUri, null, null, null, null);

				// making sure the Tweet was found in the content provider
				if(c.getCount() == 0){
					Log.w(TAG, "FavoriteStatusTask: Tweet not found " + this.rowId);
					return null;
				}
				c.moveToFirst();

				// making sure we have an official Tweet ID from Twitter
				if(c.getColumnIndex(Tweets.COL_TID)<0 || c.isNull(c.getColumnIndex(Tweets.COL_TID))){
					Log.w(TAG, "FavoriteStatusTask: Tweet has no ID! " + this.rowId);
					c.close();
					return null;
				}

				// save the flags for clearing the to favorite flag later
				flags = c.getInt(c.getColumnIndex(Tweets.COL_FLAGS));
				buffer = c.getInt(c.getColumnIndex(Tweets.COL_BUFFER));

				twitter.setFavorite(new winterwell.jtwitter.Status(null, null, c.getLong(c.getColumnIndex(Tweets.COL_TID)), null), true);
				result = 1;
			} catch (Exception ex){
				this.ex = ex;
			} finally {
				if(c!=null) c.close();
			}

			return result;
		}

		/**
		 * After favoriting Twitter, we clear the to favorite flag locally
		 */
		@Override
		protected void onPostExecute(Integer result) {

			ShowTweetListActivity.setLoading(false);

			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.Repetition){					
					Log.e(TAG, "exception while favoriting: " + ex);
				} else if(ex instanceof TwitterException.E401){
					Log.w(TAG, "exception while favoriting tweet: " + ex);
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts,notify};
						(new FavoriteStatusTask()).execute(params);
						return;
					} else {						
						if (ShowTweetListActivity.running && notify == TRUE)
						Toast.makeText(getBaseContext(), "Something went wrong while favoriting.", Toast.LENGTH_SHORT).show();
						return;
					}
				} else if(ex instanceof TwitterException.Timeout){
					Log.w(TAG, "exception while favoriting tweet: " + ex);
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts,notify};
						(new FavoriteStatusTask()).execute(params);
						return;
					} else {						
						if (ShowTweetListActivity.running && notify == TRUE)
						Toast.makeText(getBaseContext(), "Timeout while favoriting.", Toast.LENGTH_SHORT).show();
						return;
					}
				}else {
					// an exception happended, we notify the user					
					if (ShowTweetListActivity.running && notify == TRUE)
						Toast.makeText(getBaseContext(), "Something went wrong while favoriting. We will try again later!", Toast.LENGTH_LONG).show();
					Log.e(TAG, "exception while favoriting: " + ex);
					return;
				}
			}

			ContentValues cv = new ContentValues();
			cv.put(Tweets.COL_FLAGS, flags & ~Tweets.FLAG_TO_FAVORITE);
			cv.put(Tweets.COL_BUFFER, buffer | Tweets.BUFFER_FAVORITES);
			

			Uri updateUri = Uri.parse("content://"+Tweets.TWEET_AUTHORITY+"/"+Tweets.TWEETS+"/"+this.rowId);
			try{
				getContentResolver().update(updateUri, cv, null, null);
				getContentResolver().notifyChange(Tweets.TABLE_FAVORITES_URI, null);
				if (ShowTweetListActivity.running)
						Toast.makeText(getBaseContext(), "Favorite successful.", Toast.LENGTH_SHORT).show();
			} catch(Exception ex){
				Log.e(TAG, "Exception while updating tweet in DB");
			}
		}

	}

	/**
	 * Unfavorite a tweet on twitter and clear the respective flag locally
	 * @author thossmann
	 */
	private class UnfavoriteStatusTask extends AsyncTask<Long, Void, Integer> {

		long attempts,notify;
		long rowId;
		int flags;
		int buffer;

		Exception ex;

		@SuppressWarnings("deprecation")
		@Override
		protected Integer doInBackground(Long... rowId) {
			if (D) Log.d(TAG, "AsynchTask: UnfavoriteStatusTask");
			ShowTweetListActivity.setLoading(true);
			this.rowId = rowId[0];
			this.attempts = rowId[1];
			this.notify= rowId[2];
			
			Integer result = null;
			Cursor c = null;
			
			Uri queryUri = Uri.parse("content://"+Tweets.TWEET_AUTHORITY+"/"+Tweets.TWEETS+"/"+this.rowId);
			c = getContentResolver().query(queryUri, null, null, null, null);

			if (c!= null) {
				// making sure the Tweet was found in the content provider
				if(c.getCount() == 0){
					if (D) Log.w(TAG, "UnfavoriteStatusTask: Tweet not found " + this.rowId);
					c.close();
					ex = new Exception();
					return null;
				}
				c.moveToFirst();

				// making sure we have an official Tweet ID from Twitter
				if(c.getColumnIndex(Tweets.COL_TID)<0 || c.isNull(c.getColumnIndex(Tweets.COL_TID))){
					if (D) Log.w(TAG, "UnavoriteStatusTask: Tweet has no ID! " + this.rowId);
					c.close();
					ex = new Exception();
					return null;
				}

				// save the flags for clearing the to favorite flag later
				flags = c.getInt(c.getColumnIndex(Tweets.COL_FLAGS));
				buffer = c.getInt(c.getColumnIndex(Tweets.COL_BUFFER));
				
				try {
					twitter.setFavorite(new winterwell.jtwitter.Status(null, null, c.getLong(c.getColumnIndex(Tweets.COL_TID)), null), false);
					result = 1;
				} catch(Exception ex) {			
				}				
				c.close();
			}
			return result;
		}

		/**
		 * After unfavoriting from Twitter, we clear the to unfavorite flag locally
		 */
		@Override
		protected void onPostExecute(Integer result) {
			ShowTweetListActivity.setLoading(false);

			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.Repetition){					
					if (D) Log.w(TAG, "exception while favoriting: ", ex);
				} else if(ex instanceof TwitterException.E401){
					if (D) Log.w(TAG, "exception while unfavoriting: ", ex);
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts,notify};
						(new UnfavoriteStatusTask()).execute(params);
						return;
					} else {
						if (ShowTweetListActivity.running && notify == TRUE)
						Toast.makeText(getBaseContext(), "Something went wrong while unfavoriting.", Toast.LENGTH_SHORT).show();
						return;
					}
				} else if(ex instanceof TwitterException.Timeout){
					if (D) Log.w(TAG, "exception while unfavoriting: " + ex);
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts,notify};
						(new UnfavoriteStatusTask()).execute(params);
						return;
					} else {
						if (ShowTweetListActivity.running && notify == TRUE)
						Toast.makeText(getBaseContext(), "Timeout while unfavoriting.", Toast.LENGTH_SHORT).show();
						return;
					}
				}else {
					// an exception happended, we notify the user					
					if (ShowTweetListActivity.running && notify == TRUE)
						Toast.makeText(getBaseContext(), "Something went wrong while unfavoriting. We will try again later!", Toast.LENGTH_LONG).show();
					if (D) Log.e(TAG, "exception while favoriting: " + ex);
					return;
				}
			}

			ContentValues cv = new ContentValues();
			cv.put(Tweets.COL_FLAGS, flags & ~Tweets.FLAG_TO_UNFAVORITE);
			cv.put(Tweets.COL_BUFFER, buffer & ~Tweets.BUFFER_FAVORITES);		

			Uri updateUri = Uri.parse("content://"+Tweets.TWEET_AUTHORITY+"/"+Tweets.TWEETS+"/"+this.rowId);
			try{
				getContentResolver().update(updateUri, cv, null, null);
				getContentResolver().notifyChange(Tweets.TABLE_FAVORITES_URI, null);
				if (ShowTweetListActivity.running)
						Toast.makeText(getBaseContext(), "Unfavorite successful.", Toast.LENGTH_SHORT).show();
			} catch(Exception ex){
				if (D) Log.e(TAG, "Exception while updating tweet in DB");
			}
		}

	}

	/**
	 * Retweet a tweet on twitter and clear the respective flag locally
	 * @author thossmann
	 */
	private class RetweetStatusTask extends AsyncTask<Long, Void, Integer> {

		long attempts,notify;
		long rowId;
		int flags;
		int buffer;

		Exception ex;

		@SuppressWarnings("deprecation")
		@Override
		protected Integer doInBackground(Long... rowId) {
			if (D) Log.d(TAG, "AsynchTask: RetweetStatusTask");

			ShowTweetListActivity.setLoading(true);
			this.rowId = rowId[0];
			this.attempts = rowId[1];
			this.notify= rowId[2];

			Cursor c = null;

			Uri queryUri = Uri.parse("content://"+Tweets.TWEET_AUTHORITY+"/"+Tweets.TWEETS+"/"+this.rowId);
			c = getContentResolver().query(queryUri, null, null, null, null);

			if (c!= null) {
				// making sure the Tweet was found in the content provider
				if(c.getCount() == 0){
					if (D) Log.w(TAG, "RetweetStatusTask: Tweet not found " + this.rowId);
					c.close();
					ex = new Exception();
					return null;
				}
				c.moveToFirst();

				// making sure we have an official Tweet ID from Twitter
				if(c.getColumnIndex(Tweets.COL_TID)<0 || c.isNull(c.getColumnIndex(Tweets.COL_TID))){
					if (D) Log.w(TAG, "RetweetStatusTask: Tweet has no ID! " + this.rowId);
					c.close();
					ex = new Exception();
					return null;
				}

				// save the flags for clearing the to favorite flag later
				flags = c.getInt(c.getColumnIndex(Tweets.COL_FLAGS));
				buffer = c.getInt(c.getColumnIndex(Tweets.COL_BUFFER));

				try {
					twitter.retweet(new winterwell.jtwitter.Status(null, null, c.getLong(c.getColumnIndex(Tweets.COL_TID)), null));

				} catch (Exception ex){}

				c.close();
				return 1;
			} else
				return null;

		}

		/**
		 * After retweeting, we clear the to to retweet flag locally
		 */
		@Override
		protected void onPostExecute(Integer result) {
			ShowTweetListActivity.setLoading(false);

			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.Repetition){					
					if (D) Log.w(TAG, "exception while retweeting: ", ex);

				} else if(ex instanceof TwitterException.E401){
					if (D) Log.w(TAG, "exception while retweeting: " + ex);
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts,notify};
						(new RetweetStatusTask()).execute(params);
						return;
					} else {
						if (ShowTweetListActivity.running && notify == TRUE)
							Toast.makeText(getBaseContext(), "Something went wrong retweeting.", Toast.LENGTH_SHORT).show();
						return;
					}
					
				} else if(ex instanceof TwitterException.Timeout){
					if (D) Log.w(TAG, "exception while retweeting: " + ex);
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts,notify};
						(new RetweetStatusTask()).execute(params);
						return;
					} else {
						if (ShowTweetListActivity.running && notify == TRUE)
							Toast.makeText(getBaseContext(), "Timeout while retweeting.", Toast.LENGTH_SHORT).show();
						return;
					}
				} else {
					// an exception happended, we notify the user					
					if (ShowTweetListActivity.running && notify == TRUE)
						Toast.makeText(getBaseContext(), "Something went wrong while retweeting. We will try again later!", Toast.LENGTH_LONG).show();
					if (D) Log.e(TAG, "exception while retweeting: " + ex);
					return;
				}
			} else {
				ContentValues cv = new ContentValues();
				cv.put(Tweets.COL_FLAGS, flags & ~Tweets.FLAG_TO_RETWEET);
				cv.put(Tweets.COL_BUFFER, buffer);

				if(result!=null) {
					cv.put(Tweets.COL_RETWEETED, 1);
				}

				Uri updateUri = Uri.parse("content://"+Tweets.TWEET_AUTHORITY+"/"+Tweets.TWEETS+"/"+this.rowId);
				try{
					getContentResolver().update(updateUri, cv, null, null);
					if (ShowTweetListActivity.running)
						Toast.makeText(getBaseContext(), "Retweet successful.", Toast.LENGTH_LONG).show();
				} catch(NullPointerException ex){
					if (D) Log.e(TAG, "Exception while updating tweet in DB");
				}
			}
			
		}

	}
	/**
	 * Send a follow request to Twitter
	 * @author thossmann
	 */
	private class FollowUserTask extends AsyncTask<Long, Void, User> {

		long attempts;
		long rowId;
		int flags;

		Exception ex;

		@Override
		protected User doInBackground(Long... rowId) {
			if (D) Log.d(TAG, "AsynchTask: FollowUserTask");

			ShowUserActivity.setLoading(true);
			this.rowId = rowId[0];
			this.attempts = rowId[1];

			Cursor c = null;
			User user = null;


			Uri queryUri = Uri.parse("content://"+TwitterUsers.TWITTERUSERS_AUTHORITY+"/"+TwitterUsers.TWITTERUSERS+"/"+this.rowId);
			c = getContentResolver().query(queryUri, null, null, null, null);

			if (c != null) {
				if(c.getCount() == 0){					
					c.close();
					return null;
				}
				c.moveToFirst();
				flags = c.getInt(c.getColumnIndex(TwitterUsers.COL_FLAGS));

				try {	
					user = twitter.users().follow(c.getString(c.getColumnIndex(TwitterUsers.COL_SCREENNAME)));
				} catch (Exception ex) {
					this.ex = ex;
				} finally {
					c.close();
				}
			}

			return user;
		}

		/**
		 * Clear to follow flag and update the user with the information from twitter
		 */
		@Override
		protected void onPostExecute(User result) {
			ShowUserActivity.setLoading(false);
			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.E401){
					if (D) Log.w(TAG, "exception while sending follow request: ", ex);
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts};
						(new FollowUserTask()).execute(params);
						return;
					} else {
						if (ShowTweetListActivity.running)
						Toast.makeText(getBaseContext(), "Something went wrong while sending follow request.", Toast.LENGTH_SHORT).show();
						return;
					}
				} else if(ex instanceof TwitterException.Timeout){
					if (D) Log.w(TAG, "exception while sending follow request: " + ex);
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts};
						(new FollowUserTask()).execute(params);
						return;
					} else {
						if (ShowTweetListActivity.running)
						Toast.makeText(getBaseContext(), "Timeout while sending follow request.", Toast.LENGTH_SHORT).show();
						return;
					}
				} else {
					// an exception happended, we notify the user					
					if (ShowTweetListActivity.running)
						Toast.makeText(getBaseContext(), "Something went wrong while sending follow request. We will try again later!", Toast.LENGTH_LONG).show();
					if (D) Log.e(TAG, "exception while following: " + ex);
					return;
				}
			} else {
				
				// we get null if: the user does not exist or is protected
				// in any case we clear the to follow flag
				ContentValues cv = getUserContentValues(result,true);
				cv.put(TwitterUsers.COL_FLAGS, (flags & ~TwitterUsers.FLAG_TO_FOLLOW));
				// we get a user if the follow was successful
				// in that case we also mark the user as followed in the DB
				if(result!=null) {
					cv.put(TwitterUsers.COL_ISFRIEND, 1);
				}

				Uri queryUri = Uri.parse("content://"+TwitterUsers.TWITTERUSERS_AUTHORITY+"/"+TwitterUsers.TWITTERUSERS+"/"+this.rowId);

				try{
					getContentResolver().update(queryUri, cv, null, null);
					if (ShowTweetListActivity.running)
							Toast.makeText(getBaseContext(), "Follow request sent.", Toast.LENGTH_LONG).show();
				} catch(NullPointerException ex){
					if (D) Log.e(TAG, "Exception while updating tweet in DB");
				}
				
				getContentResolver().notifyChange(TwitterUsers.CONTENT_URI, null);
			}		

		}
	}


	/**
	 * Unfollow a user on Twitter
	 * @author thossmann
	 */
	private class UnfollowUserTask extends AsyncTask<Long, Void, User> {

		long attempts;
		long rowId;
		int flags;

		Exception ex;

		@Override
		protected User doInBackground(Long... rowId) {

			if (D) Log.d(TAG, "AsynchTask: UnfollowUserTask");

			ShowUserListActivity.setLoading(true);
			this.rowId = rowId[0];
			this.attempts = rowId[1];

			User user = null;
			Cursor c = null;
			
			Uri queryUri = Uri.parse("content://"+TwitterUsers.TWITTERUSERS_AUTHORITY+"/"+TwitterUsers.TWITTERUSERS+"/"+this.rowId);
			c = getContentResolver().query(queryUri, null, null, null, null);
			
			if (c != null) {
				
				if(c.getCount() == 0){					
					ex = new Exception();
					return null;
				}
				c.moveToFirst();
				flags = c.getInt(c.getColumnIndex(TwitterUsers.COL_FLAGS));
				try {
					user = twitter.users().stopFollowing(c.getString(c.getColumnIndex(TwitterUsers.COL_SCREENNAME)));
				} catch (Exception ex) {
					this.ex = ex;
				} finally {
					c.close();
				}
			}

			return user;
		}

		/**
		 * Clear to unfollow flag and update the user with the information from twitter
		 */
		@Override
		protected void onPostExecute(User result) {
			ShowUserListActivity.setLoading(false);
			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.E401){
					if (D) Log.w(TAG, "exception while sending unfollow request: " + ex);
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts};
						(new UnfollowUserTask()).execute(params);
						return;
					} else {
						if (ShowTweetListActivity.running)
						Toast.makeText(getBaseContext(), "Something went wrong while sending unfollow request.", Toast.LENGTH_SHORT).show();
						return;
					}
				} else if(ex instanceof TwitterException.Timeout){
					if (D) Log.w(TAG, "exception while sending unfollow request: " + ex);
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts};
						(new UnfollowUserTask()).execute(params);
						return;
					} else {
						if (ShowTweetListActivity.running)
						Toast.makeText(getBaseContext(), "Timeout while sending unfollow request.", Toast.LENGTH_SHORT).show();
						return;
					}
				} else {
					if (ShowTweetListActivity.running)
						Toast.makeText(getBaseContext(), "Something went wrong while sending the unfollow request. We will try again later!", Toast.LENGTH_LONG).show();
					if (D) Log.e(TAG, "exception while unfollowing: " + ex);
					return;
				}
			}

			// we get null if we did not follow the user
			// in any case we clear the to follow flag
			ContentValues cv = getUserContentValues(result,false);
			cv.put(TwitterUsers.COL_FLAGS, (flags & ~TwitterUsers.FLAG_TO_UNFOLLOW));
			// we get a user if the follow was successful
			// in that case we remove the follow in the DB
			if(result!=null) {
				cv.put(TwitterUsers.COL_ISFRIEND, 0);
			}

			Uri queryUri = Uri.parse("content://"+TwitterUsers.TWITTERUSERS_AUTHORITY+"/"+TwitterUsers.TWITTERUSERS+"/"+this.rowId);

			try{
				getContentResolver().update(queryUri, cv, null, null);
				if (ShowTweetListActivity.running)
						Toast.makeText(getBaseContext(), "Unfollowed user.", Toast.LENGTH_LONG).show();
			} catch(Exception ex){
				if (D) Log.e(TAG, "Exception while updating tweet in DB");
			}
			
			getContentResolver().notifyChange(TwitterUsers.CONTENT_URI, null);

		}

	}
	
	/**
	 * Gets user info from twitter
	 * @author thossmann
	 */
	private class UpdateUserTask extends AsyncTask<Long, Void, User> {

		long attempts;
		long rowId;
		int flags;

		Exception ex;

		@Override
		protected User doInBackground(Long... rowId) {
			Log.i(TAG, "AsynchTask: UpdateUserTask");
			ShowUserActivity.setLoading(true);
			
			this.rowId = rowId[0];
			this.attempts = rowId[1];
			
			Cursor c = null;
			User user = null;
			
			
				Uri queryUri = Uri.parse("content://"+TwitterUsers.TWITTERUSERS_AUTHORITY+"/"+TwitterUsers.TWITTERUSERS+"/"+this.rowId);
				c = getContentResolver().query(queryUri, null, null, null, null);

				if(c.getCount() == 0){
					Log.w(TAG, "UpdateUserTask: User not found " + this.rowId);
					c.close();
					return null;
				}
				c.moveToFirst();
				flags = c.getInt(c.getColumnIndex(TwitterUsers.COL_FLAGS));

			try{				
				// we need a user id or a screenname
				if(!c.isNull(c.getColumnIndex(TwitterUsers.COL_TWITTERUSER_ID))){					
					user = twitter.users().getUser(c.getLong(c.getColumnIndex(TwitterUsers.COL_TWITTERUSER_ID)));
					
				} else if(!c.isNull(c.getColumnIndex(TwitterUsers.COL_SCREENNAME))){
					user = twitter.users().getUser(c.getString(c.getColumnIndex(TwitterUsers.COL_SCREENNAME)));
				}
			} catch (TwitterException ex) {
				this.ex=ex;
				Log.e(TAG,"error",ex);
				
			} finally {			
				if(c!=null) c.close();
			}
			
			return user;
		}

		/**
		 * Clear to update flag and update the user with the information from twitter
		 */
		@Override
		protected void onPostExecute(User result) {
			
			ShowUserActivity.setLoading(false);
			if (ex!= null) {
				
				if(ex instanceof TwitterException.E401){
					Log.w(TAG, "exception while updating user information: " + ex);
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts};
						(new UpdateUserTask()).execute(params);
						return;
					} else {
						return;
					}
				} else if(ex instanceof TwitterException.Timeout){
					Log.w(TAG, "exception while updating user information: " + ex);
					// try again?
					if(attempts>0){
						Long[] params = {rowId, --attempts};
						(new UpdateUserTask()).execute(params);
						return;
					} else {
						return;
					}
				}
			} else {
				
				// we get null if something went wrong				
				if(result!=null) {
					ContentValues cv = getUserContentValues(result,false);
					
					cv.put(TwitterUsers.COL_LASTUPDATE, System.currentTimeMillis());
					
					// we clear the to update flag in any case
					cv.put(TwitterUsers.COL_FLAGS, flags & ~(TwitterUsers.FLAG_TO_UPDATE));
					cv.put("_id", this.rowId);
					
					new InsertUserTask().execute(cv);					
				}			

				ShowUserListActivity.setLoading(false);
			}
		}
	}
	
	/**
	 * Asynchronously insert a user into the DB 
	 * @author thossmann
	 *
	 */
	private class InsertUserTask extends AsyncTask<ContentValues, Void, Void> {

		@Override
		protected Void doInBackground(ContentValues... params) {
			ShowUserActivity.setLoading(true);

			ContentValues cv = params[0];
			Log.i(TAG, "AsynchTask: InsertUserTask");
			try{
				Uri queryUri = Uri.parse("content://"+TwitterUsers.TWITTERUSERS_AUTHORITY+"/"+TwitterUsers.TWITTERUSERS+"/"+cv.getAsInteger("_id"));			
				getContentResolver().update(queryUri, cv, null, null);
			} catch(Exception ex){
				Log.e(TAG, "Exception while inserting user update into DB");
			}

			return null;
		}

		@Override 
		protected void onPostExecute(Void params){
			// here, we have to notify almost everyone
			getContentResolver().notifyChange(TwitterUsers.CONTENT_URI, null);
			getContentResolver().notifyChange(Tweets.ALL_TWEETS_URI, null);
			getContentResolver().notifyChange(DirectMessages.CONTENT_URI, null);
			
			ShowUserActivity.setLoading(false);

		}

	}

	/**
	 * Updates the incoming direct messages
	 * @author thossmann
	 *
	 */
	private class UpdateDMsInTask extends AsyncTask<Integer, Void, List<winterwell.jtwitter.Message>> {

		int attempts;
		Exception ex;

		@Override
		protected List<winterwell.jtwitter.Message> doInBackground(Integer... params) {
			Log.d(TAG, "AsynchTask: UpdateDMsInTask");

			ShowDMUsersListActivity.setLoading(true);
			attempts = params[0];

			List<winterwell.jtwitter.Message> dms = null;

			twitter.setCount(Constants.NR_DMS);
			twitter.setSinceId(getDMsInSinceId(getBaseContext()));


			try {
				dms = twitter.getDirectMessages();
				Log.i(TAG,"dms size: " + dms.size());
			} catch (Exception ex) {					
				// save the expcetion for handling it in on post execute
				this.ex = ex;
			}

			return dms;
		}

		@Override
		protected void onPostExecute(List<winterwell.jtwitter.Message> result) {

			ShowDMUsersListActivity.setLoading(false);

			// error handling
			if(ex != null){
				// an exception happended, we try again or notify the user
				if(ex instanceof TwitterException.RateLimit){					
					Log.e(TAG, "exception while loading incoming DMs: " + ex);
					return;
				} else {
					if(attempts>0) {
						Log.w(TAG, "Exception, attempt " + attempts);
						(new UpdateDMsOutTask()).execute(--attempts);
						return;
					} else {
						if (ShowTweetListActivity.running)
						Toast.makeText(getBaseContext(), "Something went wrong while loading your direct messages. Please try again later!", Toast.LENGTH_LONG).show();
						Log.e(TAG, "exception while loading incoming DMs: " + ex);
						return;
					}
				}
			}

			new InsertDMsInTask().execute(result);

		}

	}

	/**
	 * Asynchronously inserts direct messages (incoming) into DB
	 * @author thossmann
	 *
	 */
	private class InsertDMsInTask extends AsyncTask<List<winterwell.jtwitter.Message>, Void, Void>{

		@Override
		protected Void doInBackground(List<winterwell.jtwitter.Message>... params) {
			
			ShowDMUsersListActivity.setLoading(true);

			List<winterwell.jtwitter.Message> result = params[0];

			if(result==null) return null;

			if(!result.isEmpty()){
				Long lastId = null;

				for (winterwell.jtwitter.Message dm: result) {
					if(lastId == null){
						lastId = dm.getId().longValue();
						// save the id of the last DM (comes first from twitter) for future synchs
						setDMsInSinceId(new BigInteger(lastId.toString()), getBaseContext());
					}

					updateUser(dm.getSender(),false);
					updateMessage(dm, DirectMessages.BUFFER_MESSAGES);

				}

			}
			return null;
		}

		@Override
		protected void onPostExecute(Void params){
			ShowDMUsersListActivity.setLoading(false);

			// trigger the user synch (for updating the profile images)
			new SynchTransactionalUsersTask(false).execute(false);

			// save the timestamp of the last update
			setLastDMsInUpdate(new Date(), getBaseContext());
			setTaskExecuted(TwitterService.TASK_DIRECT_MESSAGES_IN, TwitterService.this);
		}
	}

	/**
	 * Updates the outgoing direct messages
	 * @author thossmann
	 *
	 */
	private class UpdateDMsOutTask extends AsyncTask<Integer, Void, List<winterwell.jtwitter.Message>> {
		int attempts;
		Exception ex;
		@Override
		protected List<winterwell.jtwitter.Message> doInBackground(Integer... params) {

			Log.d(TAG, "AsynchTask: UpdateDMsOutTask");
			ShowDMUsersListActivity.setLoading(true);

			attempts = params[0];

			List<winterwell.jtwitter.Message> dms = null;

			twitter.setCount(Constants.NR_DMS);
			twitter.setSinceId(getDMsOutSinceId(getBaseContext()));

			try {
				dms = twitter.getDirectMessagesSent();
			} catch (Exception ex) {					
				// save the expcetion for handling it in on post execute
				this.ex = ex;
			}

			return dms;
		}

		@Override
		protected void onPostExecute(List<winterwell.jtwitter.Message> result) {

			ShowDMUsersListActivity.setLoading(false);
			
			// error handling
			if(ex != null){
				// an exception happended, we try again or notify the user
				if(ex instanceof TwitterException.RateLimit){					
					Log.e(TAG, "exception while loading outgoing DMs: " + ex);
					return;
				} else {
					if(attempts>0) {
						Log.w(TAG, "Exception, attempt " + attempts);
						(new UpdateDMsOutTask()).execute(--attempts);
						return;
					} else {
						if (ShowTweetListActivity.running)
						Toast.makeText(getBaseContext(), "Something went wrong while loading your direct messages. Please try again later!", Toast.LENGTH_LONG).show();
						Log.e(TAG, "exception while loading outgoing DMs: " + ex);
						return;
					}
				}
			}

			new InsertDMsOutTask().execute(result);
		}
	}

	/**
	 * Asynchronously inserts direct messages (outgoing) into DB
	 * @author thossmann
	 *
	 */
	private class InsertDMsOutTask extends AsyncTask<List<winterwell.jtwitter.Message>, Void, Void>{

		@Override
		protected Void doInBackground(List<winterwell.jtwitter.Message>... params) {

			ShowDMUsersListActivity.setLoading(true);
			
			List<winterwell.jtwitter.Message> result = params[0];

			if(result==null) return null;

			if(!result.isEmpty()){
				Long lastId = null;

				for (winterwell.jtwitter.Message dm: result) {
					if(lastId == null){
						lastId = dm.getId().longValue();
						// save the id of the last DM (comes first from twitter) for future synchs
						setDMsOutSinceId(new BigInteger(lastId.toString()), getBaseContext());
					}

					updateUser(dm.getSender(),false);
					updateMessage(dm, DirectMessages.BUFFER_MESSAGES);

				}

			}
			return null;
		}

		@Override
		protected void onPostExecute(Void params){
			ShowDMUsersListActivity.setLoading(false);

			// trigger the user synch (for updating the profile images)
			new SynchTransactionalUsersTask(false).execute(false);

			// save the timestamp of the last update
			setLastDMsOutUpdate(new Date(), getBaseContext());
			
		}
	}
	/**
	 * Post a DM to twitter
	 * @author thossmann
	 */
	private class SendMessageTask extends AsyncTask<Long, Void, winterwell.jtwitter.Message> {

		long rowId, notify;
		int flags;
		int buffer;
		String text;
		String rec;

		Exception ex;

		@Override
		protected winterwell.jtwitter.Message doInBackground(Long... rowId) {
			Log.d(TAG, "AsynchTask: SendMessageTask");
			this.rowId = rowId[0];
			notify = rowId[1];
			
			Cursor c = null;
			winterwell.jtwitter.Message msg = null;

			try {
				
				Uri queryUri = Uri.parse("content://"+DirectMessages.DM_AUTHORITY+"/"+DirectMessages.DMS+"/"+this.rowId);
				c = getContentResolver().query(queryUri, null, null, null, null);

				if(c.getCount() == 0){
					Log.w(TAG, "SendMessageTask: Message not found " + this.rowId);
					return null;
				}
				c.moveToFirst();
				flags = c.getInt(c.getColumnIndex(DirectMessages.COL_FLAGS));
				buffer = c.getInt(c.getColumnIndex(DirectMessages.COL_BUFFER));


				text = c.getString(c.getColumnIndex(DirectMessages.COL_TEXT));
				rec = c.getString(c.getColumnIndex(DirectMessages.COL_RECEIVER_SCREENNAME));
				Log.d(TAG, "sending: " + text + " to " + rec);
				msg = twitter.sendMessage(rec, text);

			} catch(Exception ex) { 
				this.ex = ex;
			} finally {
				c.close();
			}
			return msg;
		}

		/**
		 * Clear to insert flag and update the message with the information from twitter
		 */
		@Override
		protected void onPostExecute(winterwell.jtwitter.Message result) {

			Uri queryUri = Uri.parse("content://"+DirectMessages.DM_AUTHORITY+"/"+DirectMessages.DMS+"/"+this.rowId);

			// error handling
			if(ex != null){
				if(ex instanceof TwitterException.Repetition){					
					Log.w(TAG, "exception while sending DM: ", ex);
					getContentResolver().delete(queryUri, null, null);
					Log.w(TAG, "Error: "+ex);
					return;
				}  else if (ex instanceof TwitterException.E403) {
					if (ShowUserActivity.running || ShowDMUsersListActivity.running && notify==1)
						Toast.makeText(getBaseContext(), "Could not post message! Maybe the recepient is not following you ?", Toast.LENGTH_LONG).show();
					Log.e(TAG, "exception while sending DM: " + ex);
					Intent i = new Intent(getBaseContext(), NewDMActivity.class);
					i.putExtra("recipient", rec);
					i.putExtra("text", text);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getBaseContext().startActivity(i);
					getContentResolver().delete(queryUri, null, null);
					
					return;
				}
			} else {
				ContentValues cv = new ContentValues();
				cv.put(DirectMessages.COL_FLAGS, flags & ~(Tweets.FLAG_TO_INSERT));
				cv.put(DirectMessages.COL_BUFFER, buffer);
				cv.put(DirectMessages.COL_TEXT, result.getText());
				cv.put(DirectMessages.COL_CREATED, result.getCreatedAt().getTime());
				cv.put(DirectMessages.COL_DMID, result.getId().longValue());
				cv.put(DirectMessages.COL_SENDER, result.getSender().getId());
				cv.put(DirectMessages.COL_RECEIVER, result.getRecipient().getId());
				cv.put(DirectMessages.COL_RECEIVER_SCREENNAME, result.getRecipient().getScreenName());


				getContentResolver().update(queryUri, cv, null, null);
				if (ShowTweetListActivity.running)
							Toast.makeText(getBaseContext(), "Message sent.", Toast.LENGTH_SHORT).show();
			}			

		}

	}

	public static class LinksParser {

		public static ArrayList<String> parseText(String substr){

			ArrayList<String> urls = new ArrayList<String>();

			String[] strarr = substr.split(" ");
			//check the urls of the tweet
			for(String subStrarr : strarr){

				if(subStrarr.indexOf("http://") >= 0 || subStrarr.indexOf("https://") >= 0){
					String subUrl = null;
					if(subStrarr.indexOf("http://") >= 0){
						subUrl = subStrarr.substring(subStrarr.indexOf("http://"));
					}else if(subStrarr.indexOf("https://") >= 0){
						subUrl = subStrarr.substring(subStrarr.indexOf("https://"));
					}
					urls.add(subUrl);

				}
			}
			return urls;

		}
	}

}
