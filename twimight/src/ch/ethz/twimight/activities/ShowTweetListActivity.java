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

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.Toast;
import ch.ethz.twimight.R;
import ch.ethz.twimight.data.StatisticsDBHelper;
import ch.ethz.twimight.fragments.TweetListFragment;
import ch.ethz.twimight.fragments.adapters.ListViewPageAdapter;
import ch.ethz.twimight.listeners.TabListener;
import ch.ethz.twimight.location.LocationHelper;
import ch.ethz.twimight.util.Constants;



/**
 * The main Twimight view showing the Timeline, favorites and mentions
 * @author thossmann
 * 
 */
public class ShowTweetListActivity extends TwimightBaseActivity{

	private static final String TAG = "ShowTweetListActivity";	
	
		
	public static boolean running= false;
	// handler
	static Handler handler;


	
	//LOGS
	LocationHelper locHelper ;
	long timestamp;	
	ConnectivityManager cm;
	StatisticsDBHelper locDBHelper;	
	CheckLocation checkLocation;
	public static final String ON_PAUSE_TIMESTAMP = "onPauseTimestamp";	
	
	ActionBar actionBar;
	public static final String FILTER_REQUEST = "filter_request";

	ViewPager viewPager;
	ListViewPageAdapter pagAdapter;
	
	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(null);				
		setContentView(R.layout.main);
					
		//statistics
		locDBHelper = new StatisticsDBHelper(getApplicationContext());
		locDBHelper.open();
		
		cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		timestamp = System.currentTimeMillis();
		
		locHelper = LocationHelper.getInstance(this);
		locHelper.registerLocationListener();
		
		handler = new Handler();
		checkLocation = new CheckLocation();
		handler.postDelayed(checkLocation, 1*60*1000L);	
		
		Bundle bundle = new Bundle();
		bundle.putInt(ListViewPageAdapter.BUNDLE_TYPE, ListViewPageAdapter.BUNDLE_TYPE_TWEETS);
		pagAdapter = new ListViewPageAdapter(getFragmentManager(), bundle);		
        
		viewPager = (ViewPager)  findViewById(R.id.viewpager);	
		
		viewPager.setAdapter(pagAdapter);
		viewPager.setOffscreenPageLimit(2);
		viewPager.setOnPageChangeListener(
	            new ViewPager.SimpleOnPageChangeListener() {
	                @Override
	                public void onPageSelected(int position) {
	                    // When swiping between pages, select the
	                    // corresponding tab.	                	
	                    getActionBar().setSelectedNavigationItem(position);
	                }
	            });

		//action bar
		actionBar = getActionBar();	
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		Tab tab = actionBar.newTab()
				.setIcon(R.drawable.ic_twimight_speech)
				.setTabListener(new TabListener(viewPager));
		actionBar.addTab(tab);

		tab = actionBar.newTab()
				.setIcon(R.drawable.ic_twimight_favorites)
				.setTabListener(new TabListener(viewPager));
		actionBar.addTab(tab);

		tab = actionBar.newTab()
				.setIcon(R.drawable.ic_twimight_mentions)
				.setTabListener(new TabListener(viewPager ));
		actionBar.addTab(tab);		

	}
		


	private class CheckLocation implements Runnable {

		@Override
		public void run() {

			if (locHelper != null && locHelper.getCount() > 0 && locDBHelper != null && cm.getActiveNetworkInfo() != null) {	
				Log.i(TAG,"writing log");
				locDBHelper.insertRow(locHelper.getLocation(), cm.getActiveNetworkInfo().getTypeName(), StatisticsDBHelper.APP_STARTED, null, timestamp);
				locHelper.unRegisterLocationListener();

			} else {}
			
		}
		
	}
	

	@Override
	protected void onNewIntent(Intent intent) {		
		setIntent(intent);	
	}



	/**
	 * On resume
	 */
	@Override
	public void onResume(){

		super.onResume();
		running = true;

		Intent intent = getIntent();

		if(intent.hasExtra(FILTER_REQUEST)) {
			viewPager.setCurrentItem(intent.getIntExtra(FILTER_REQUEST, TweetListFragment.TIMELINE_KEY));
			intent.removeExtra(FILTER_REQUEST);

		}

		Long pauseTimestamp =  getOnPauseTimestamp(this);
		if (pauseTimestamp != 0 &&  (System.currentTimeMillis()-pauseTimestamp) > 10 * 60 * 1000L ) {
			handler = new Handler();			
			handler.post(new CheckLocation());

		}		


	}
    


	@Override
	protected void onPause() {
		
		super.onPause();
		setOnPauseTimestamp(System.currentTimeMillis(), this);
	}


	/**
	 * 
	 * @param id
	 * @param context
	 */
	private static void setOnPauseTimestamp(long timestamp, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putLong(ON_PAUSE_TIMESTAMP, timestamp);
		prefEditor.commit();
	}
	
	/**
	 * Gets the Twitter ID from shared preferences
	 * @param context
	 * @return
	 */
	public static Long getOnPauseTimestamp(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getLong(ON_PAUSE_TIMESTAMP, 0);
	}


	@Override
	protected void onStop() {
		running=false;
		locHelper.unRegisterLocationListener();
		super.onStop();
	
		
	}
	

	/**
	 * Called at the end of the Activity lifecycle
	 */
	@Override
	public void onDestroy(){
		super.onDestroy();
		running = false;
		
		pagAdapter = null;
		viewPager = null;
		
		Log.i(TAG,"setting dd and dn to null");
		dd.setCallback(null);
		dn.setCallback(null);
		dd = null;
		dn = null;
		actionBar = null;
		
		Log.i(TAG,"destroying main activity");
		if ((System.currentTimeMillis() - timestamp <= 1 * 60 * 1000L)&& locHelper!=null && locDBHelper != null && 
				cm.getActiveNetworkInfo() != null) {
			
			if (locHelper.getCount() > 0 && cm.getActiveNetworkInfo() != null ) {				
				handler.removeCallbacks(checkLocation);				
				locDBHelper.insertRow(locHelper.getLocation(), cm.getActiveNetworkInfo().getTypeName(), StatisticsDBHelper.APP_STARTED , null, timestamp);
			} else {}
		}
		
		if ((locHelper != null && locHelper.getCount() > 0) && locDBHelper != null && cm.getActiveNetworkInfo() != null) {				
			locDBHelper.insertRow(locHelper.getLocation(), cm.getActiveNetworkInfo().getTypeName(), StatisticsDBHelper.APP_CLOSED , null, System.currentTimeMillis());
		} else {}

		TwimightBaseActivity.unbindDrawables(findViewById(R.id.rootRelativeLayout));	
		
		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("prefDisasterMode", Constants.DISASTER_DEFAULT_ON) == true)
			Toast.makeText(this, getString(R.string.disastermode_running), Toast.LENGTH_LONG).show();


	}

	
	
	/**
	 * Saves the current selection
	 
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {

	  savedInstanceState.putInt("currentFilter", currentFilter);
	  positionIndex = timelineListView.getFirstVisiblePosition();
	  View v = timelineListView.getChildAt(0);
	  positionTop = (v == null) ? 0 : v.getTop();
	  savedInstanceState.putInt("positionIndex", positionIndex);
	  savedInstanceState.putInt("positionTop", positionTop);
	  
	  Log.i(TAG, "saving" + positionIndex + " " + positionTop);
	  
	  super.onSaveInstanceState(savedInstanceState);
	}
	
	/**
	 * Loads the current user selection
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);
	  
	
	  positionIndex = savedInstanceState.getInt("positionIndex");
	  positionTop = savedInstanceState.getInt("positionTop");
	  
	  Log.i(TAG, "restoring " + positionIndex + " " + positionTop);
	}
	
	*/
	
	
	
	
	
	
	
}
