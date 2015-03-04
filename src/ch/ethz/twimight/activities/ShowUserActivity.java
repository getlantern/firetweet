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
package ch.ethz.twimight.activities;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.content.ContentValues;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import ch.ethz.twimight.R;
import ch.ethz.twimight.fragments.UserListFragment;
import ch.ethz.twimight.net.twitter.TwitterService;
import ch.ethz.twimight.net.twitter.TwitterUsers;

/**
 * Display a user
 * @author thossmann
 *
 */
public class ShowUserActivity extends TwimightBaseActivity{

	private static final String TAG = "ShowUserActivity";

	Uri uri;
	Cursor c;
	int flags;
	long rowId;

	// Views
	private ImageView profileImage;
	private TextView screenName;
	private TextView realName;
	private TextView location;
	private TextView description;
	private TextView statsTweets;
	private TextView statsFavorits;
	private TextView statsFriends;
	private TextView statsFollowers;
	private Button followButton;
	private ImageButton mentionButton;
	private ImageButton messageButton;
	private Button showFollowersButton;
	private Button showFriendsButton;
	private Button showDisPeersButton;
	private LinearLayout followInfo;
	private LinearLayout unfollowInfo;
	private Button showUserTweetsButton;
	
	public static boolean running= false;
	private boolean following;
	String userScreenName;
	Handler handler;
	ContentObserver observer = null;

	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.showuser);
		
		profileImage = (ImageView) findViewById(R.id.showUserProfileImage);
		screenName = (TextView) findViewById(R.id.showUserScreenName);
		realName = (TextView) findViewById(R.id.showUserRealName);
		location = (TextView) findViewById(R.id.showUserLocation);
		description = (TextView) findViewById(R.id.showUserDescription);
		statsTweets = (TextView) findViewById(R.id.statsTweets);
		statsFavorits = (TextView) findViewById(R.id.statsFavorits);
		statsFriends = (TextView) findViewById(R.id.statsFriends);
		statsFollowers = (TextView) findViewById(R.id.statsFollowers);
		followButton = (Button) findViewById(R.id.showUserFollow);
		mentionButton = (ImageButton) findViewById(R.id.showUserMention);
		messageButton = (ImageButton) findViewById(R.id.showUserMessage);
		followInfo = (LinearLayout) findViewById(R.id.showUserTofollow);
		unfollowInfo = (LinearLayout) findViewById(R.id.showUserTounfollow);
		showFollowersButton = (Button) findViewById(R.id.showUserFollowers);
		showFriendsButton = (Button) findViewById(R.id.showUserFriends);
		showDisPeersButton = (Button) findViewById(R.id.showUserDisasterPeers);
		showUserTweetsButton = (Button) findViewById(R.id.showUserTweetsButton);		
		
		if(getIntent().hasExtra("rowId")){
			rowId = (long) getIntent().getIntExtra("rowId", 0);

			// get data from local DB
			uri = Uri.parse("content://" + TwitterUsers.TWITTERUSERS_AUTHORITY + "/" + TwitterUsers.TWITTERUSERS + "/" + rowId);
			c = getContentResolver().query(uri, null, null, null, null);			
			
			if(c.getCount() == 0) finish();

			c.moveToFirst();

		} else if(getIntent().hasExtra("screenname")){

			Log.e(TAG, getIntent().getStringExtra("screenname"));
			
			// get data from local DB
			uri = Uri.parse("content://" + TwitterUsers.TWITTERUSERS_AUTHORITY + "/" + TwitterUsers.TWITTERUSERS);
			c = getContentResolver().query(uri, null, TwitterUsers.COL_SCREENNAME+" LIKE '"+getIntent().getStringExtra("screenname")+"'", null, null);

			if(c.getCount() == 0) {
				Log.w(TAG, "USER NOT FOUND " + getIntent().getStringExtra("screenname"));
				finish();
				return;
			}

			c.moveToFirst();
			rowId = c.getLong(c.getColumnIndex("_id"));

		} else {
			// if we don't know which user to show
			Log.w(TAG, "WHICH USER??");
			finish();
			return;
		}

		// mark the user for updating
		uri = Uri.parse("content://" + TwitterUsers.TWITTERUSERS_AUTHORITY + "/" + TwitterUsers.TWITTERUSERS + "/" + rowId);
		ContentValues cv = new ContentValues();
		cv.put(TwitterUsers.COL_FLAGS, TwitterUsers.FLAG_TO_UPDATEIMAGE|TwitterUsers.FLAG_TO_UPDATE|c.getInt(c.getColumnIndex(TwitterUsers.COL_FLAGS)));
		getContentResolver().update(uri, cv, null, null);
		
		// trigger the update
		Intent i = new Intent(this, TwitterService.class);
		i.putExtra("synch_request", TwitterService.SYNCH_USER);
		i.putExtra("rowId", rowId);
		startService(i);
		
		// register content observer to refresh when user was updated
		handler = new Handler();		

		// show the views
		showUserInfo();
		
	}
	
	/**
	 * We listen to updates from the content provider
	 */
	@Override
	public void onResume(){
		super.onResume();
		observer = new UserContentObserver(handler);
		c.registerContentObserver(observer);
		running = true;
	}
	
	/**
	 * We pause listening for updates from the content provider
	 */
	@Override
	public void onPause(){
		super.onPause();
		if(c!=null){
			if(observer != null) 
				try {
					c.unregisterContentObserver(observer);
				} catch (IllegalStateException ex) {
					//Log.e(TAG,"error unregistering observer",ex);
				}
		}
	}

	/**
	 * Called at the end of the Activity lifecycle
	 */
	@Override
	public void onDestroy(){
		super.onDestroy();		
		if(followButton!=null) followButton.setOnClickListener(null);
		if(mentionButton!=null) mentionButton.setOnClickListener(null);
		if(messageButton!=null) messageButton.setOnClickListener(null);
		if(showFollowersButton!=null) showFollowersButton.setOnClickListener(null);
		if(showFriendsButton!=null) showFriendsButton.setOnClickListener(null);
		if(showUserTweetsButton!=null) showUserTweetsButton.setOnClickListener(null);
		
		TwimightBaseActivity.unbindDrawables(findViewById(R.id.showUserRoot));
		
		observer = null;
		handler = null;
		
	}
	
	@Override
	 	protected void onStop() {
	 	// TODO Auto-generated method stub
	 	super.onStop();
	 	running = false;
	 	}

	/**
	 * Compare the argument with the local user ID.
	 * @param userString
	 * @return
	 */
	private boolean isLocalUser(String userString) {
		String localUserString = LoginActivity.getTwitterId(this);
		return userString.equals(localUserString);
	}
	
	/**
	 * Fills the views
	 */
	private void showUserInfo(){
		/*
		 * User info
		 */

		// do we have a profile image?
		if(!c.isNull(c.getColumnIndex(TwitterUsers.COL_SCREENNAME))){
			int userId = c.getInt(c.getColumnIndex("_id"));
			Uri imageUri = Uri.parse("content://" +TwitterUsers.TWITTERUSERS_AUTHORITY + "/" + TwitterUsers.TWITTERUSERS + "/" + userId);
			InputStream is;
			
			try {
				is = getContentResolver().openInputStream(imageUri);
				if (is != null) {						
					Bitmap bm = BitmapFactory.decodeStream(is);
					profileImage.setImageBitmap(bm);	
				} else
					profileImage.setImageResource(R.drawable.default_profile);
			} catch (FileNotFoundException e) {
				Log.e(TAG,"error opening input stream",e);
				profileImage.setImageResource(R.drawable.default_profile);
			}	
		}
		userScreenName = c.getString(c.getColumnIndex(TwitterUsers.COL_SCREENNAME)); 
		screenName.setText("@" + userScreenName);
		realName.setText(c.getString(c.getColumnIndex(TwitterUsers.COL_NAME)));

		if(c.getColumnIndex(TwitterUsers.COL_LOCATION) >=0){
			location.setText(c.getString(c.getColumnIndex(TwitterUsers.COL_LOCATION)));
			location.setVisibility(TextView.VISIBLE);
		} else {
			location.setVisibility(TextView.GONE);
		}

		if(c.getColumnIndex(TwitterUsers.COL_DESCRIPTION) >=0){
			String tmp = c.getString(c.getColumnIndex(TwitterUsers.COL_DESCRIPTION));
			if(tmp != null){
				description.setText(tmp);
				description.setVisibility(TextView.VISIBLE);
			} else {
				description.setVisibility(TextView.GONE);
			}
		} else {
			description.setVisibility(TextView.GONE);
		}

		int tweets = c.getInt(c.getColumnIndex(TwitterUsers.COL_STATUSES));
		int favorites = c.getInt(c.getColumnIndex(TwitterUsers.COL_FAVORITES));
		int follows = c.getInt(c.getColumnIndex(TwitterUsers.COL_FRIENDS));
		int followed = c.getInt(c.getColumnIndex(TwitterUsers.COL_FOLLOWERS));

		statsTweets.setText(String.valueOf(tweets));
		statsFavorits.setText(String.valueOf(favorites));
		statsFriends.setText(String.valueOf(follows));
		statsFollowers.setText(String.valueOf(followed));

		// if the user we show is the local user, disable the follow button
		if(isLocalUser(Long.toString(c.getLong(c.getColumnIndex(TwitterUsers.COL_TWITTERUSER_ID))))){
			showLocalUser();
		} else {
			showRemoteUser();
		}
		
		// if we have a user ID we show the recent tweets
		if(!c.isNull(c.getColumnIndex(TwitterUsers.COL_TWITTERUSER_ID))){
			showUserTweetsButton.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					Intent i = new Intent(getBaseContext(), ShowUserTweetListActivity.class);
					c.moveToFirst();
					int index = c.getColumnIndex(TwitterUsers.COL_TWITTERUSER_ID);
					if (index != -1) {
						i.putExtra("userId",c.getLong(index));
						startActivity(i);
					}

				}

			});
			showUserTweetsButton.setVisibility(Button.VISIBLE);
		} else {
			showUserTweetsButton.setVisibility(Button.GONE);
		}
	}

	/**
	 * Sets the user interface up to show the local user's profile
	 */
	private void showLocalUser(){
		
		// disable the normal user buttons
		LinearLayout remoteUserButtons = (LinearLayout) findViewById(R.id.showUserButtons);
		if (remoteUserButtons != null)
			remoteUserButtons.setVisibility(LinearLayout.GONE);

		// enable the show followers and show followee's buttons
		LinearLayout localUserButtons = (LinearLayout) findViewById(R.id.showLocalUserButtons);
		if (localUserButtons != null)
			localUserButtons.setVisibility(LinearLayout.VISIBLE);
		
		// the followers Button
		showFollowersButton.setOnClickListener(null);
		showFollowersButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {				
				Intent i = new Intent(getBaseContext(), ShowUserListActivity.class);
				i.putExtra(ShowUserListActivity.USER_FILTER_REQUEST,UserListFragment.FOLLOWERS_KEY 	);
				startActivity(i);

			}

		});

		// the followees Button
		showFriendsButton.setOnClickListener(null);
		showFriendsButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {				
				Intent i = new Intent(getBaseContext(), ShowUserListActivity.class);
				i.putExtra(ShowUserListActivity.USER_FILTER_REQUEST,UserListFragment.FRIENDS_KEY );
				startActivity(i);
			}

		});
		
		showDisPeersButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				
				Intent i = new Intent(getBaseContext(), ShowUserListActivity.class);
				i.putExtra(ShowUserListActivity.USER_FILTER_REQUEST,UserListFragment.PEERS_KEY );
				startActivity(i);

			}

		});
	}

	/**
	 * Sets the UI up to show a remote user (any user except for the local one)
	 */
	private void showRemoteUser(){
		flags = c.getInt(c.getColumnIndex(TwitterUsers.COL_FLAGS));
		Log.i(TAG,"showRemoteUser");
		/*
		 * The following cases are possible: 
		 * - the user was marked for following
		 * - the user was marked for unfollowing
		 * - we follow the user
		 * - the request to follow was sent
		 * - none of the above, we can follow the user
		 */
		following = c.getInt(c.getColumnIndex(TwitterUsers.COL_ISFRIEND))>0;
		if(following){
			followButton.setText(R.string.unfollow);
		} else {
			followButton.setText(R.string.follow);
		}
		// listen to clicks		
		followButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				if(following){
					getContentResolver().update(uri, setUnfollowFlag(flags), null, null);
					followButton.setVisibility(Button.GONE);
					unfollowInfo.setVisibility(LinearLayout.VISIBLE);
					following=false;
				} else {
					getContentResolver().update(uri, setFollowFlag(flags), null, null);
					followButton.setVisibility(Button.GONE);
					followInfo.setVisibility(LinearLayout.VISIBLE);
					following = true;
				}
				
				// trigger the update
				Intent i = new Intent(getBaseContext(), TwitterService.class);
				i.putExtra("synch_request", TwitterService.SYNCH_USER);
				i.putExtra("rowId", rowId);
				startService(i);				
			}

		});

		if((flags & TwitterUsers.FLAG_TO_FOLLOW)>0){			
			// disable follow button
			followButton.setVisibility(Button.GONE);
			// show info that the user will be followed upon connectivity
			followInfo.setVisibility(LinearLayout.VISIBLE);
		} else {
			// disable follow button
			followButton.setVisibility(Button.VISIBLE);
			// show info that the user will be followed upon connectivity
			followInfo.setVisibility(LinearLayout.GONE);
		}
		
		if((flags & TwitterUsers.FLAG_TO_UNFOLLOW)>0){
			// disable follow button
			followButton.setVisibility(Button.GONE);
			// show info that the user will be unfollowed upon connectivity
			unfollowInfo.setVisibility(LinearLayout.VISIBLE);
		} else {			
			// disable follow button
			followButton.setVisibility(Button.VISIBLE);
			// show info that the user will be unfollowed upon connectivity
			unfollowInfo.setVisibility(LinearLayout.GONE);
		}
		
		if(c.getInt(c.getColumnIndex(TwitterUsers.COL_FOLLOWREQUEST))>0){			
			// disable follow button
			followButton.setVisibility(Button.GONE);
		}    

		/*
		 * Mention button
		 */
		mentionButton.setOnClickListener(null);
		mentionButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getBaseContext(),NewTweetActivity.class);
				i.putExtra("text", "@"+userScreenName+" ");
				startActivity(i);
			}
		});

		/*
		 * Message button
		 */
		messageButton.setOnClickListener(null);
		messageButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getBaseContext(),NewDMActivity.class);
				i.putExtra("recipient", userScreenName);
				startActivity(i);
			}
		});

	}

	/**
	 * Returns content values with the to follow flag set
	 * @param flags
	 * @return
	 */
	private ContentValues setFollowFlag(int flags) {
		ContentValues cv = new ContentValues();
		flags = flags & (~TwitterUsers.FLAG_TO_UNFOLLOW);
		// set follow flag
		cv.put(TwitterUsers.COL_FLAGS, flags | TwitterUsers.FLAG_TO_FOLLOW);
		return cv;
	}


	/**
	 * Returns content values with the to unfollow flag set
	 * @param flags
	 * @return
	 */
	private ContentValues setUnfollowFlag(int flags) {
		ContentValues cv = new ContentValues();
		flags = flags & (~TwitterUsers.FLAG_TO_FOLLOW);
		// set follow flag
		cv.put(TwitterUsers.COL_FLAGS, flags | TwitterUsers.FLAG_TO_UNFOLLOW);
		return cv;
	}

	/**
	 * Calls showUserInfo if the user data has been updated
	 * @author thossmann
	 *
	 */
	class UserContentObserver extends ContentObserver {
		public UserContentObserver(Handler h) {
			super(h);
		}

		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}

		@Override
		public void onChange(boolean selfChange) {
			
			super.onChange(selfChange);
			
			// and get a new one
			uri = Uri.parse("content://" + TwitterUsers.TWITTERUSERS_AUTHORITY + "/" + TwitterUsers.TWITTERUSERS + "/" + rowId);
			c = getContentResolver().query(uri, null, null, null, null);
			if(c.getCount() == 0) 
				finish();
			else {
				c.moveToFirst();			
				showUserInfo();
			}
			

		}
	}
}
