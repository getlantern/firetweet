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

package ch.ethz.twimight.net.twitter;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import ch.ethz.twimight.R;
import ch.ethz.twimight.util.InternalStorageHelper;

/** 
 * Cursor adapter for a cursor containing users.
 */
public class DMUserAdapter extends SimpleCursorAdapter {
	Context context;
	
	private static final String TAG = "DMUserAdapter";
	static final String[] from = {TwitterUsers.COL_SCREENNAME, TwitterUsers.COL_NAME, DirectMessages.COL_TEXT};
	static final int[] to = {R.id.showUserScreenName, R.id.showUserRealName, R.id.showDMText};

	/** Constructor */
	public DMUserAdapter(Context context, Cursor c) {
		super(context, R.layout.dmuserrow, c, from, to); 
		this.context=context;
	}

	/** This is where data is mapped to its view */
	@Override
	public void bindView(View userrow, Context context, Cursor cursor) {
		super.bindView(userrow, context, cursor);
			
		// Profile image
		ImageView picture = (ImageView) userrow.findViewById(R.id.showMDUserProfileImage);
		if(!cursor.isNull(cursor.getColumnIndex(TwitterUsers.COL_SCREENNAME))){
			int userId = cursor.getInt(cursor.getColumnIndex("_id"));
			Uri imageUri = Uri.parse("content://" +TwitterUsers.TWITTERUSERS_AUTHORITY + "/" + TwitterUsers.TWITTERUSERS + "/" + userId);
			InputStream is;
			
			try {
				is = context.getContentResolver().openInputStream(imageUri);
				if (is != null) {						
					Bitmap bm = BitmapFactory.decodeStream(is);
					picture.setImageBitmap(bm);	
				} else
					picture.setImageResource(R.drawable.default_profile);
			} catch (FileNotFoundException e) {
				Log.e(TAG,"error opening input stream",e);
				picture.setImageResource(R.drawable.default_profile);
			}	
		} else {
			picture.setImageResource(R.drawable.default_profile);
		}
		LinearLayout rowLayout = (LinearLayout) userrow.findViewById(R.id.showDMUser);		
		rowLayout.setBackgroundResource(R.drawable.normal_tweet_background);
		
	}
	
}
