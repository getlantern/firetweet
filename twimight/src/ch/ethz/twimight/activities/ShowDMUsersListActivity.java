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

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import ch.ethz.twimight.R;
import ch.ethz.twimight.net.twitter.DMUserAdapter;
import ch.ethz.twimight.net.twitter.DirectMessages;
import ch.ethz.twimight.net.twitter.TwitterUsers;

/**
 * Shows the overview of direct messages. A list view with an item for each user with which
 * we have exchanged direct messages.
 * @author thossmann
 *
 */
public class ShowDMUsersListActivity extends TwimightBaseActivity{

	private static final String TAG = "ShowDMUsersListActivity";
	
	// Views
	private ListView dmUsersListView;
	
	private DMUserAdapter adapter;
	private Cursor c;
	public static boolean running= false;

	// handler
	static Handler handler;

	private int positionIndex;
	private int positionTop;

	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);				
		
		setContentView(R.layout.show_dm_users);
		
		dmUsersListView = (ListView) findViewById(R.id.dmUsersList);
		c = getContentResolver().query(Uri.parse("content://" + DirectMessages.DM_AUTHORITY + "/" + DirectMessages.DMS + "/" + DirectMessages.DMS_USERS), null, null, null, null);
		
		Log.e(TAG, "Users: " +c.getCount());
		
		adapter = new DMUserAdapter(this, c);		
		dmUsersListView.setAdapter(adapter);
		dmUsersListView.setEmptyView(findViewById(R.id.dmListEmpty));
		// Click listener when the user clicks on a user
		dmUsersListView.setClickable(true);
		dmUsersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				Cursor c = (Cursor) dmUsersListView.getItemAtPosition(position);
				Intent i = new Intent(getBaseContext(), ShowDMListActivity.class);
				i.putExtra("rowId", c.getInt(c.getColumnIndex("_id")));
				i.putExtra("screenname", c.getString(c.getColumnIndex(TwitterUsers.COL_SCREENNAME)));
				startActivity(i);
			}
		});
		
		
		

	}
	
	/**
	 * On resume
	 */
	@Override
	public void onResume(){
		super.onResume();
		running = true;
		
		
		if(positionIndex != 0 | positionTop !=0){
			dmUsersListView.setSelectionFromTop(positionIndex, positionTop);
		}
	}
	


	@Override
	protected void onStop() {
		running=false;
		super.onStop();
	}

	/**
	 * Called at the end of the Activity lifecycle
	 */
	@Override
	public void onDestroy(){
		super.onDestroy();	
		
		dmUsersListView.setOnItemClickListener(null);
		dmUsersListView.setAdapter(null);

		if(c!=null) c.close();
				
		unbindDrawables(findViewById(R.id.showDMUsersListRoot));
	}
	
	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if(item.getItemId() == R.id.menu_write_tweet){		
			startActivity(new Intent(getBaseContext(), NewDMActivity.class));	
			
		} else
			super.onOptionsItemSelected(item);
		
		return true;
		
	}

	/**
	 * Saves the current selection
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {

	  positionIndex = dmUsersListView.getFirstVisiblePosition();
	  View v = dmUsersListView.getChildAt(0);
	  positionTop = (v == null) ? 0 : v.getTop();
	  savedInstanceState.putInt("positionIndex", positionIndex);
	  savedInstanceState.putInt("positionTop", positionTop);
	  
	  Log.i(TAG, "saving" + positionIndex + " " + positionTop);
	  
	  super.onSaveInstanceState(savedInstanceState);
	}
	
	/**
	 * Loads the current user selection
	 */
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);
	  
	  positionIndex = savedInstanceState.getInt("positionIndex");
	  positionTop = savedInstanceState.getInt("positionTop");
	  
	  Log.i(TAG, "restoring " + positionIndex + " " + positionTop);
	}
	
}
