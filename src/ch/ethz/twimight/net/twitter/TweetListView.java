package ch.ethz.twimight.net.twitter;

import ch.ethz.twimight.util.Constants;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ListView;

public class TweetListView extends ListView {
	private final String TAG = "TweetListView";
	private int maxOverscroll = 150;
	private int curOverscroll = 0;
	private Intent overscrollIntent;
	private Context context;

	public TweetListView(Context context) {
		super(context);
		
		setOverScrollMode(this.OVER_SCROLL_NEVER);
		this.context = context;
	}

	public TweetListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOverScrollMode(OVER_SCROLL_ALWAYS);
		this.context = context;
	}
	
	public void setOverscrollIntent(Intent i){			
		overscrollIntent = i;
	}

	private void sendOverscrollIntent(boolean topOverscroll){	
		
		if(overscrollIntent!=null ) {
			Log.i(TAG, "calling twitter service");
			if (topOverscroll) {
				overscrollIntent.putExtra(TwitterService.OVERSCROLL_TYPE, TwitterService.OVERSCROLL_TOP);
				if (Constants.TIMELINE_BUFFER_SIZE >= 150)
					Constants.TIMELINE_BUFFER_SIZE -= 50;
				Log.i(TAG, "BUFFER_SIZE =  "+ Constants.TIMELINE_BUFFER_SIZE);
			}
			else {
				overscrollIntent.putExtra(TwitterService.OVERSCROLL_TYPE, TwitterService.OVERSCROLL_BOTTOM);				
				
			}
			context.startService(overscrollIntent);
			
		} else
			Log.i(TAG, "intent null");
	}
		/*
	 * 
	 *
	private void initBounceListView()
	{
		//get the density of the screen and do some maths with it on the max overscroll distance
		//variable so that you get similar behaviors no matter what the screen size
		
		final DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        	final float density = metrics.density;
        
		mMaxYOverscrollDistance = (int) (density * MAX_Y_OVERSCROLL_DISTANCE);
	}
	*
	*/

	@Override
	protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
		
		return super.overScrollBy(0, deltaY, 0, scrollY, 0, scrollRangeY, 0, maxOverscroll, isTouchEvent);

	}

	@Override
	protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
		
		// did we reach the max just now?
		if(curOverscroll>(-maxOverscroll) && scrollY==(-maxOverscroll)){			
			sendOverscrollIntent(true);
		} else if (curOverscroll<(maxOverscroll) && scrollY==(maxOverscroll)){ 
			sendOverscrollIntent(false);	
			
		}
		curOverscroll=scrollY;
		//Log.i(TAG, "scrollY:" + scrollY );

		super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);

	}
}