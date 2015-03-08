package ch.ethz.twimight.listeners;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;

 public class TabListener implements ActionBar.TabListener {
    ViewPager vPager;

   

    /** Constructor used each time a new tab is created.
      * @param activity  The host Activity, used to instantiate the fragment
      * @param tag  The identifier tag for the fragment
      * @param clz  The fragment's Class, used to instantiate the fragment
      */
    public TabListener(ViewPager pg) {
       this.vPager=pg;
        
    }

    /* The following are each of the ActionBar.TabListener callbacks 

    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        // Check if the fragment is already initialized
        if (mFragment == null) {        	
        	
            // If not, instantiate and add it to the activity
        	if(mClass.getName().equals("ch.ethz.twimight.fragments.TweetListFragment"))
        		mFragment = new TweetListFragment(mActivity, mTag);	 
        	else if(mClass.getName().equals("ch.ethz.twimight.fragments.UserListFragment"))
        		mFragment = new UserListFragment(mActivity, mTag);	
        	
            ft.add(android.R.id.content, mFragment, mTag);
        } else {
            // If it exists, simply attach it in order to show it
            ft.attach(mFragment);
       
        }
    }
    */
    
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
       //if (mFragment != null) {
            // Detach the fragment, because another one is being attached
            //ft.detach(mFragment);
       // }
    }

    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        // User selected the already selected tab. Usually do nothing.
    }

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		// When the tab is selected, switch to the
        // corresponding page in the ViewPager.
		Log.d("TabListener", "inside onTabSelecred, position = " + tab.getPosition());
        vPager.setCurrentItem(tab.getPosition());
		
	}
}
