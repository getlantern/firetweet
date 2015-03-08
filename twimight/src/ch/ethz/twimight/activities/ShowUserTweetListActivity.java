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


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import ch.ethz.twimight.R;
import ch.ethz.twimight.fragments.TweetListFragment;

/**
 * Shows the most recent tweets of a user
 * @author thossmann
 *
 */
public class ShowUserTweetListActivity extends TwimightBaseActivity{

	private static final String TAG = "ShowUserTweetListActivity";	
	
	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(!getIntent().hasExtra("userId")) finish();
		
		setContentView(R.layout.main);		
				
		long userId = getIntent().getLongExtra("userId", 0);
		FragmentManager fragmentManager = getFragmentManager();
	    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        TweetListFragment tlf = new TweetListFragment(TweetListFragment.USER_TWEETS);
        
        Bundle bundle =  new Bundle();
        bundle.putLong(TweetListFragment.USER_ID, userId);
        tlf.setArguments(bundle);
        
        fragmentTransaction.add(R.id.rootRelativeLayout,tlf);
        fragmentTransaction.commit();
		
	}
	
	

	
	
	
	
}
