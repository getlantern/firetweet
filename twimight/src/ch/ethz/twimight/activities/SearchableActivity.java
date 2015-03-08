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
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.ViewPager;
import ch.ethz.twimight.R;
import ch.ethz.twimight.fragments.ListFragment;
import ch.ethz.twimight.fragments.TweetListFragment.OnInitCompletedListener;
import ch.ethz.twimight.fragments.adapters.ListViewPageAdapter;
import ch.ethz.twimight.listeners.TabListener;
import ch.ethz.twimight.util.TwimightSuggestionProvider;

/**
 * Shows the most recent tweets of a user
 * @author thossmann
 * @author pcarta
 */
public class SearchableActivity extends TwimightBaseActivity implements OnInitCompletedListener{

	private static final String TAG = "SearchableActivity";

	
		
	ViewPager viewPager;	
	public static String query;
	ListViewPageAdapter pagAdapter;
	Intent intent;
	ListFragment listFrag;
	
	/** 
	 * Called when the activity is first created. 	
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(null);
		setContentView(R.layout.main);	

		viewPager = (ViewPager)  findViewById(R.id.viewpager); 

		Bundle bundle = new Bundle();		
		bundle.putInt(ListViewPageAdapter.BUNDLE_TYPE, ListViewPageAdapter.BUNDLE_TYPE_SEARCH_RESULTS);
		
		pagAdapter = new ListViewPageAdapter(getFragmentManager(), bundle);      
		viewPager.setAdapter(pagAdapter);	
		viewPager.setCurrentItem(actionBar.getSelectedNavigationIndex());
		
		//action bar
		actionBar = getActionBar();	
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);	  	

		viewPager.setOnPageChangeListener(
				new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						// When swiping between pages, select the
						// corresponding tab.	                	
	                    getActionBar().setSelectedNavigationItem(position);
	                }
	            });

		
		Tab tab = actionBar.newTab()
				.setText("Tweets")
				.setTabListener(new TabListener(viewPager));
		actionBar.addTab(tab);

		tab = actionBar.newTab()
				.setText("Users")
				.setTabListener(new TabListener(viewPager));
		actionBar.addTab(tab);

		// Get the intent and get the query
		intent = getIntent();
		//processIntent(intent);


	}


	private void processIntent(Intent intent) {
		if (intent.hasExtra(SearchManager.QUERY)) {
			//if (!intent.getStringExtra(SearchManager.QUERY).equals(query))
			query = intent.getStringExtra(SearchManager.QUERY);	
			setTitle(getString(R.string.results_for) + query);
			
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
	                TwimightSuggestionProvider.AUTHORITY, TwimightSuggestionProvider.MODE);
	        suggestions.saveRecentQuery(query, null); 	     
		
		} 
	}



	@Override
	protected void onNewIntent(Intent intent) {		
		setIntent(intent);
		processIntent(intent);
		getFragmentByPosition(0).newQueryText();
		getFragmentByPosition(1).newQueryText();
				
	}

	public ListFragment getFragmentByPosition(int pos) {
        String tag = "android:switcher:" + viewPager.getId() + ":" + pos;
        return (ListFragment) getFragmentManager().findFragmentByTag(tag);
    }


	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		processIntent(intent);
	}


	@Override
	public void onInitCompleted() {
		 processIntent(intent);
		// listFrag = getFragmentByPosition(actionBar.getSelectedNavigationIndex());
	    // listFrag.setQueryText(query);		
		
	}


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		pagAdapter = null;
		viewPager = null;
		
	}
	
	
	
	
	/**
	 * Saves the current selection
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {

		if(searchListView != null){
			positionIndex = searchListView.getFirstVisiblePosition();
			View v = searchListView.getChildAt(0);
			positionTop = (v == null) ? 0 : v.getTop();
			savedInstanceState.putInt("positionIndex", positionIndex);
			savedInstanceState.putInt("positionTop", positionTop);

			Log.i(TAG, "saving" + positionIndex + " " + positionTop);
		}

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
