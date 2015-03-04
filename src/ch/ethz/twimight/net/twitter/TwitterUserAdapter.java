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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import ch.ethz.twimight.R;

/** 
 * Cursor adapter for a cursor containing users.
 */
public class TwitterUserAdapter extends SimpleCursorAdapter {
	
	private static final String TAG = "TwitterUserAdapter";
	
	static final String[] from = {TwitterUsers.COL_SCREENNAME, TwitterUsers.COL_NAME, TwitterUsers.COL_LOCATION};
	static final int[] to = {R.id.showUserScreenName, R.id.showUserRealName, R.id.showUserLocation};

	/** Constructor */
	public TwitterUserAdapter(Context context, Cursor c) {
		super(context, R.layout.userrow, c, from, to);  
	}
	
	private static class ViewHolder {
		ImageView picture ;
		LinearLayout rowLayout ;		
		 
		}

	 


	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		// TODO Auto-generated method stub

		LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.userrow, null);		
		createHolder(view);	
		
		return view;

	}

	private void createHolder(View view) {
		ViewHolder holder = new ViewHolder();
		setHolderFields(view,holder);
		view.setTag(holder);
	}
	
	private void setHolderFields(View userrow, ViewHolder holder) {
		holder.picture = (ImageView) userrow.findViewById(R.id.showUserProfileImage);
		holder.rowLayout = (LinearLayout) userrow.findViewById(R.id.showUserInfo);	
		
		
	}

	/** This is where data is mapped to its view */
	@Override
	public void bindView(View userrow, Context context, Cursor cursor) {
		super.bindView(userrow, context, cursor);
			
		// Profile image
		
		ViewHolder holder = (ViewHolder) userrow.getTag();
		if(!cursor.isNull(cursor.getColumnIndex(TwitterUsers.COL_SCREENNAME))){

			//InternalStorageHelper helper = new InternalStorageHelper(context);
			//byte[] imageByteArray = helper.readImage(cursor.getString(cursor.getColumnIndex(TwitterUsers.COL_SCREENNAME)));
			int userId = cursor.getInt(cursor.getColumnIndex("_id"));
			Uri imageUri = Uri.parse("content://" + TwitterUsers.TWITTERUSERS_AUTHORITY + "/" + TwitterUsers.TWITTERUSERS + "/" + userId);
			InputStream is;
			
			try {
				is = context.getContentResolver().openInputStream(imageUri);
				if (is != null) {						
					Bitmap bm = BitmapFactory.decodeStream(is);
					holder.picture.setImageBitmap(bm);	
				} else
					holder.picture.setImageResource(R.drawable.default_profile);
			} catch (FileNotFoundException e) {
				//Log.e(TAG,"error opening input stream");
				holder.picture.setImageResource(R.drawable.default_profile);
			}	

		} else {
			holder.picture.setImageResource(R.drawable.default_profile);
		}
		
		holder.rowLayout.setBackgroundResource(R.drawable.normal_tweet_background);
	}

}



			