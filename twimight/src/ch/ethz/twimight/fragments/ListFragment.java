package ch.ethz.twimight.fragments;


import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import ch.ethz.twimight.R;
import ch.ethz.twimight.net.twitter.TweetListView;

public abstract class ListFragment extends Fragment {
	
	Cursor c;
	Intent overscrollIntent ;
	int type ;
	ContentResolver resolver;
	ListAdapter adapter;
	//String query;
	TweetListView list;
	protected static final String TAG = "ListFragment";
	
	public static final String FRAGMENT_TYPE = "fragment_type";	
	public static final String SEARCH_QUERY = "search_query";
	
	

	
	@Override
	public void onCreate(Bundle savedInstanceState) {		
		
		super.onCreate(savedInstanceState);			
		resolver = getActivity().getContentResolver();		
		
	}
		
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		
		Log.i(TAG,"onCreateView");
        // Inflate the layout for this fragment	
	    View view = inflater.inflate(R.layout.fragment_layout, container, false);
		list = (TweetListView) view.findViewById(R.id.tweetListView);
		
		list.setAdapter(adapter);
		list.setOverscrollIntent(overscrollIntent);		
		
        return list;
        
    }	

	public void newQueryText() {
		// Called when the action bar search text has changed.  Update
		// the search filter
		//query = newText;
		
		adapter = getData(type);
		list.setAdapter(adapter);
	}

		
	/**
	 * Which tweets do we show? Timeline, favorites, mentions?
	 * @param filter
	 */
	abstract ListAdapter getData(int filter);

	
	


}
