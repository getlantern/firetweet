package ch.ethz.twimight.fragments.adapters;


import java.util.HashMap;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import ch.ethz.twimight.R;
import ch.ethz.twimight.fragments.ListFragment;
import ch.ethz.twimight.fragments.TweetListFragment;
import ch.ethz.twimight.fragments.UserListFragment;

public class ListViewPageAdapter extends FragmentPagerAdapter {
	
	public static final String BUNDLE_TYPE = "bundle_type";
	public static final int BUNDLE_TYPE_TWEETS = 0;
	public static final int BUNDLE_TYPE_USERS = 1;
	public static final int BUNDLE_TYPE_SEARCH_RESULTS = 2;
	
	private static final String TAG = "ListViewPageAdapter";

	Bundle bundle;
	HashMap<Integer,ListFragment> map;
	FragmentManager fragMan;
	
	public ListViewPageAdapter( FragmentManager fm, Bundle bundle) {
		super(fm);
		fragMan = fm;
		this.bundle=bundle;		
		switch (bundle.getInt(BUNDLE_TYPE)) {
			
		case BUNDLE_TYPE_TWEETS:
			map = createTweetListFragments();
			break;
		case BUNDLE_TYPE_USERS:
			map = createUserListFragments();
			break;
		case BUNDLE_TYPE_SEARCH_RESULTS:			
			map = createSearchListFragments();
			
			break;
		}
		
	}
	

	@Override
	public Fragment getItem(int pos) {
		return map.get(pos);

	}

	@Override
	public int getCount() {
		return map.size();

	}
	
	public ListFragment getFragmentByPosition(int pos) {
        String tag = "android:switcher:" + R.id.viewpager + ":" + pos;
      
        return (ListFragment) fragMan.findFragmentByTag(tag);
    }
	

	
	private HashMap<Integer,ListFragment> createSearchListFragments() {

		HashMap<Integer,ListFragment> map = new HashMap<Integer,ListFragment>();	
			
		map.put(0,(getFragmentByPosition(0) == null) ? new TweetListFragment(TweetListFragment.SEARCH_TWEETS) : getFragmentByPosition(0) );
		map.put(1, (getFragmentByPosition(1) == null) ? new UserListFragment(UserListFragment.SEARCH_USERS) : getFragmentByPosition(1) );

		return map;
	}

	private HashMap<Integer,ListFragment> createUserListFragments() {

		HashMap<Integer,ListFragment> map = new HashMap<Integer,ListFragment>();
		map.put(0, (getFragmentByPosition(0) == null) ? new UserListFragment(UserListFragment.FRIENDS_KEY) : getFragmentByPosition(0) );
		map.put(1, (getFragmentByPosition(1) == null) ? new UserListFragment(UserListFragment.FOLLOWERS_KEY) : getFragmentByPosition(1) );
		map.put(2, (getFragmentByPosition(2) == null) ? new UserListFragment(UserListFragment.PEERS_KEY) : getFragmentByPosition(2) );

		return map;
	}

	private HashMap<Integer,ListFragment> createTweetListFragments() {

		HashMap<Integer,ListFragment> map = new HashMap<Integer,ListFragment>();
		
		map.put(0, (getFragmentByPosition(0) == null) ? new TweetListFragment(TweetListFragment.TIMELINE_KEY) : getFragmentByPosition(0));
		map.put(1, (getFragmentByPosition(1) == null) ? new TweetListFragment(TweetListFragment.FAVORITES_KEY) : getFragmentByPosition(1));
		map.put(2, (getFragmentByPosition(2) == null) ? new TweetListFragment(TweetListFragment.MENTIONS_KEY) : getFragmentByPosition(2));

		return map;
	}

}
