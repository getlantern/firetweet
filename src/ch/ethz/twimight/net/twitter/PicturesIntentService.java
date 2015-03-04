package ch.ethz.twimight.net.twitter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import ch.ethz.twimight.activities.ShowUserListActivity;
import ch.ethz.twimight.util.InternalStorageHelper;

public class PicturesIntentService extends IntentService {
	
	public static final String TAG = "PicturesIntentService";
	public static final String USERS_IDS = "users_ids";
	ArrayList<String> screenNames;
	ArrayList<byte[]> pictures;
	long[] rowIds;
	Cursor[] cursorArray ;

	public PicturesIntentService() {
		super("PicturesIntentService");
		
	}

	@Override
	protected void onHandleIntent(Intent intent) {		
		
		ShowUserListActivity.setLoading(true);
		rowIds = intent.getLongArrayExtra(PicturesIntentService.USERS_IDS);
		downloadProfilePictures(rowIds);
		insertPictures();		
		ShowUserListActivity.setLoading(false);

	}	
	

	private void insertPictures() {	
		
		
		// clear the update image flag			
		ContentValues[] cv = new ContentValues[pictures.size()];		
		
		for (int i=0; i<cv.length; i++) {				
															
			insertProfileImageIntoInternalStorage(pictures.get(i),screenNames.get(i));
			//cursorArray[i].getInt(cursorArray[i].getColumnIndex("_id"));
			
			cv[i]= new ContentValues();
			cv[i].put("_id", rowIds[i]);
			if (!cursorArray[i].isClosed()) {
				cv[i].put(TwitterUsers.COL_FLAGS, ~(TwitterUsers.FLAG_TO_UPDATEIMAGE) & 
						cursorArray[i].getInt(cursorArray[i].getColumnIndex(TwitterUsers.COL_FLAGS)));
				cv[i].put(TwitterUsers.COL_PROFILEIMAGE_PATH, new File(getFilesDir(),screenNames.get(i)).getPath() );	
				cv[i].put(TwitterUsers.COL_LAST_PICTURE_UPDATE, System.currentTimeMillis());	
				cursorArray[i].close();
			}				
		}	

		// insert pictures into DB
		InsertProfileImagesParameters(cv);
		// here, we have to notify almost everyone
		//Log.i(TAG,"notifying group of pictures");
		//getContentResolver().notifyChange(TwitterUsers.CONTENT_URI, null);
		getContentResolver().notifyChange(Tweets.TABLE_TIMELINE_URI, null);
		getContentResolver().notifyChange(Tweets.TABLE_MENTIONS_URI, null);
		getContentResolver().notifyChange(Tweets.TABLE_FAVORITES_URI, null);
		

	}

	private void insertProfileImageIntoInternalStorage(byte[] image, String name) {
		InternalStorageHelper helper = new InternalStorageHelper(getBaseContext());
		helper.writeImage(image, name);
		
	}

	private void InsertProfileImagesParameters(ContentValues[] params) {
		if (params.length ==1) {
			ContentValues cv = params[0];				
			try{
				Uri queryUri = Uri.parse("content://"+TwitterUsers.TWITTERUSERS_AUTHORITY+"/"+TwitterUsers.TWITTERUSERS+"/"+cv.getAsLong("_id"));			
				getContentResolver().update(queryUri, cv, null, null);
			} catch(IllegalArgumentException ex){
				Log.e(TAG, "Exception while inserting profile image into DB",ex);
				
			}
		} else 				
			updateUsers(params);	
		
	}
	
	/**
	 * Updates the user profile in the DB.
	 * @param contentValues 
	 * @param user
	 */
	private long updateUsers(ContentValues[] users) {
		
		if(users==null) return 0;		
		Uri insertUri = Uri.parse("content://" + TwitterUsers.TWITTERUSERS_AUTHORITY + "/" + TwitterUsers.TWITTERUSERS);
		int result = getContentResolver().bulkInsert(insertUri, users);

		return result;	

	}

	private void downloadProfilePictures(long[] rowIds) {
		Uri queryUri;				
		
		DefaultHttpClient mHttpClient = new DefaultHttpClient();		
		pictures = new ArrayList<byte[]>();
		screenNames = new ArrayList<String>();
		cursorArray = new Cursor[rowIds.length];
		
		for (int i=0; i<rowIds.length; i++) {

			queryUri = Uri.parse("content://"+TwitterUsers.TWITTERUSERS_AUTHORITY+"/"+TwitterUsers.TWITTERUSERS+"/"+rowIds[i]);					
			cursorArray[i] = getContentResolver().query(queryUri, null, null, null, null);

			if(cursorArray[i].getCount() == 0){						
				cursorArray[i].close();
				continue;
			}
			cursorArray[i].moveToFirst();

			String imageUrl = null;
			// this should not happen
			if(cursorArray[i].isNull(cursorArray[i].getColumnIndex(TwitterUsers.COL_IMAGEURL))){
				cursorArray[i].close();					
				continue;
			} else 
				imageUrl = cursorArray[i].getString(cursorArray[i].getColumnIndex(TwitterUsers.COL_IMAGEURL));
			
			HttpGet mHttpGet = new HttpGet(imageUrl);
			HttpResponse mHttpResponse;
			try {
				mHttpResponse = mHttpClient.execute(mHttpGet);
				if (mHttpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					
					try {
						
						pictures.add(EntityUtils.toByteArray( mHttpResponse.getEntity()));
						screenNames.add(cursorArray[i].getString(cursorArray[i].getColumnIndex(TwitterUsers.COL_SCREENNAME)));
					} catch (IOException ex) {
						
					}
				}
			} catch (ClientProtocolException e) {				
			} catch (IOException e) {				
			}
			
		}
	}
	

	

}
