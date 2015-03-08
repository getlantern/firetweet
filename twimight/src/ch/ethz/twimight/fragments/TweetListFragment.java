package ch.ethz.twimight.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import ch.ethz.twimight.R;
import ch.ethz.twimight.activities.SearchableActivity;
import ch.ethz.twimight.activities.ShowTweetActivity;
import ch.ethz.twimight.activities.TwimightBaseActivity;
import ch.ethz.twimight.net.twitter.TweetAdapter;
import ch.ethz.twimight.net.twitter.TweetListView;
import ch.ethz.twimight.net.twitter.Tweets;
import ch.ethz.twimight.net.twitter.TwitterService;


@SuppressLint("ValidFragment")
public class TweetListFragment extends ListFragment {	
	
	long userId;
	public static final int TIMELINE_KEY = 10;	
	public static final int FAVORITES_KEY = 11;
	public static final int MENTIONS_KEY = 12;
	public static final int SEARCH_TWEETS = 13;
	public static final int USER_TWEETS = 14;
	
	public static final String USER_ID = "USER_ID";
	
	// Container Activity must implement this interface
    public interface OnInitCompletedListener {
        public void onInitCompleted();
    }
    
    OnInitCompletedListener listener;
    
    @Override
    public void onAttach(Activity activity) {    	
        super.onAttach(activity);
        try {
            listener = (OnInitCompletedListener) activity;
        } catch (ClassCastException e) {      
        }
    }


	
	public TweetListFragment(){
        
    };
   
   
	public TweetListFragment(int type) {           
            this.type=type;           
            Log.i(TAG,"creating instance of tweet list frag");
	}   
	
	

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		listener = null;
		
		super.onDestroy();		
	}



	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);			
		if (type == USER_TWEETS) {
			userId = getArguments().getLong(USER_ID);
			Log.i("TEST","userId: " + userId);
		}
		adapter = getData(type);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		
		TweetListView list = (TweetListView) super.onCreateView(inflater, container, savedInstanceState);
		// Click listener when the user clicks on a tweet
		list.setClickable(true);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
				Cursor c = (Cursor) arg0.getItemAtPosition(position);
				Intent i = new Intent(getActivity(), ShowTweetActivity.class);
				i.putExtra("rowId", c.getInt(c.getColumnIndex("_id")));
				i.putExtra("type", type);			
				startActivity(i);
				//if (type == SEARCH_TWEETS)
					//i.putExtra(ListFragment.SEARCH_QUERY, query);
			}
		});
		
		//if (listener != null)
			//listener.onInitCompleted();
		return list;
	}

	/**
	 * Which tweets do we show? Timeline, favorites, mentions?
	 * @param filter
	 */
	ListAdapter getData(int filter){
		// set all header button colors to transparent
	
		if(c!=null) c.close();			
		overscrollIntent = new Intent(getActivity(), TwitterService.class); 
		
		switch(filter) {
		case TIMELINE_KEY: 
			
			overscrollIntent.putExtra("synch_request", TwitterService.SYNCH_TIMELINE);
			overscrollIntent.putExtra(TwitterService.FORCE_FLAG, true);
			c = resolver.query(Uri.parse("content://" + Tweets.TWEET_AUTHORITY + "/" + Tweets.TWEETS + "/" 
					+ Tweets.TWEETS_TABLE_TIMELINE + "/" + Tweets.TWEETS_SOURCE_ALL), null, null, null, null);


			break;
		case FAVORITES_KEY: 
				
			overscrollIntent.putExtra("synch_request", TwitterService.SYNCH_FAVORITES);
			overscrollIntent.putExtra(TwitterService.FORCE_FLAG, true);
			c = resolver.query(Uri.parse("content://" + Tweets.TWEET_AUTHORITY + "/" + Tweets.TWEETS + "/"
					+ Tweets.TWEETS_TABLE_FAVORITES + "/" + Tweets.TWEETS_SOURCE_ALL), null, null, null, null);


			break;
		case MENTIONS_KEY: 
		
			overscrollIntent.putExtra("synch_request", TwitterService.SYNCH_MENTIONS);
			overscrollIntent.putExtra(TwitterService.FORCE_FLAG, true);
			c = resolver.query(Uri.parse("content://" + Tweets.TWEET_AUTHORITY + "/" + Tweets.TWEETS + "/" 
					+ Tweets.TWEETS_TABLE_MENTIONS + "/" + Tweets.TWEETS_SOURCE_ALL), null, null, null, null);


			break;
		case SEARCH_TWEETS:
			c = resolver.query(Uri.parse("content://" + Tweets.TWEET_AUTHORITY + "/" + Tweets.TWEETS + "/" 
					+ Tweets.SEARCH), null, SearchableActivity.query , null, null);
		
			break;
			
		case USER_TWEETS:
			c = resolver.query(Uri.parse("content://" + Tweets.TWEET_AUTHORITY + "/" + Tweets.TWEETS +
					"/" + Tweets.TWEETS_TABLE_USER + "/" + userId), null, null, null, null);
			Log.i("TEST","QUERY PERFORMED	");

			break;
		

		}			
		return new TweetAdapter(getActivity(), c);	

		
	}
	
	

}
