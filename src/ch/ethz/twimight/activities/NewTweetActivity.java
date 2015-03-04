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


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import ch.ethz.twimight.R;
import ch.ethz.twimight.data.StatisticsDBHelper;
import ch.ethz.twimight.location.LocationHelper;
import ch.ethz.twimight.net.twitter.Tweets;
import ch.ethz.twimight.net.twitter.TwitterService;
import ch.ethz.twimight.util.Constants;
import ch.ethz.twimight.util.SDCardHelper;

/**
 * The activity to write a new tweet.
 * @author thossmann
 * @author pcarta
 */
public class NewTweetActivity extends Activity{

	private static final String TAG = "TweetActivity";
	
	private boolean useLocation;
	private EditText text;
	private TextView characters;
	private Button cancelButton;
	private Button sendButton;
	
	private long isReplyTo;
	
	// the following are all to deal with location
	private ImageButton locationButton;
	
	private boolean locationChecked;
	private TextWatcher textWatcher;
	
	//uploading photos
	private static final int PICK_FROM_CAMERA = 1;
	private static final int PICK_FROM_FILE = 2;
	private String tmpPhotoPath; //path storing photos on SDcard
	private String finalPhotoPath; //path storing photos on SDcard
	private String finalPhotoName; //file name of uploaded photo
	private Uri tmpPhotoUri; //uri storing temp photos
	private Uri photoUri; //uri storing photos
	private ImageView mImageView; //to display the photo to be uploaded

	private boolean hasMedia = false;
	private ImageButton uploadFromGallery;
	private ImageButton uploadFromCamera;
	private ImageButton deletePhoto;
	private ImageButton previewPhoto;
	private ImageButton photoButton;
	private Bitmap photo = null;
	private LinearLayout photoLayout;

	//SDcard helper
	private SDCardHelper sdCardHelper;

	//LOGS
	LocationHelper locHelper ;
	long timestamp;		
	ConnectivityManager cm;		
	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tweet);				

		cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);			
		locHelper = LocationHelper.getInstance(this);		

		//SDCard helper
		sdCardHelper = new SDCardHelper();		
		setupBasicButtons();

		characters = (TextView) findViewById(R.id.tweet_characters);
		characters.setText(Integer.toString(Constants.TWEET_LENGTH));

		text = (EditText) findViewById(R.id.tweetText);		

		// Did we get some extras in the intent?
		Intent i = getIntent();
		if(i.hasExtra("text")){
			text.setText(Html.fromHtml("<i>"+i.getStringExtra("text")+"</i>"));
		}
		if(text.getText().length()==0){
			sendButton.setEnabled(false);
		}

		if(text.getText().length()>Constants.TWEET_LENGTH){
			text.setText(text.getText().subSequence(0, Constants.TWEET_LENGTH));
			text.setSelection(text.getText().length());
			characters.setTextColor(Color.RED);
		}

		characters.setText(Integer.toString(Constants.TWEET_LENGTH-text.getText().length()));

		if(i.hasExtra("isReplyTo")){
			isReplyTo = i.getLongExtra("isReplyTo", 0);
		}

		// This makes sure we do not enter more than 140 characters	
		textWatcher = new TextWatcher(){
			public void afterTextChanged(Editable s){
				int nrCharacters = Constants.TWEET_LENGTH-text.getText().length();

				if(nrCharacters < 0){
					text.setText(text.getText().subSequence(0, Constants.TWEET_LENGTH));
					text.setSelection(text.getText().length());
					nrCharacters = Constants.TWEET_LENGTH-text.getText().length();
				}

				if(nrCharacters <= 0){
					characters.setTextColor(Color.RED);
				} else {
					characters.setTextColor(Color.BLACK);
				}

				if(nrCharacters == Constants.TWEET_LENGTH){
					sendButton.setEnabled(false);
				} else {
					sendButton.setEnabled(true);
				}

				characters.setText(Integer.toString(nrCharacters));

			}
			public void  beforeTextChanged(CharSequence s, int start, int count, int after){}
			public void  onTextChanged (CharSequence s, int start, int before,int count) {} 
		};
		text.addTextChangedListener(textWatcher);
		text.setSelection(text.getText().length());	

		setupImageRelatedButtons();	


	}

	private void setupImageRelatedButtons(){

		//uploading photos
		tmpPhotoPath = Tweets.PHOTO_PATH + "/" + "tmp";
		finalPhotoPath = Tweets.PHOTO_PATH + "/" + LoginActivity.getTwitterId(this);
		mImageView = new ImageView(this);

		photoLayout = (LinearLayout) findViewById(R.id.linearLayout_photo_view);
		photoLayout.setVisibility(View.GONE);

		uploadFromGallery = (ImageButton) findViewById(R.id.upload_from_gallery);
		uploadFromGallery.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				uploadFromGallery();
			}
		});

		uploadFromCamera = (ImageButton) findViewById(R.id.upload_from_camera);
		uploadFromCamera.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				uploadFromCamera();
			}
		});

		previewPhoto = (ImageButton) findViewById(R.id.preview_photo);
		previewPhoto.setOnClickListener(new OnClickListener(){
			public void onClick(View v){

				mImageView = new ImageView(NewTweetActivity.this);
				mImageView.setImageBitmap(photo);
				AlertDialog.Builder photoPreviewDialog = new AlertDialog.Builder(NewTweetActivity.this);
				photoPreviewDialog.setView(mImageView);
				photoPreviewDialog.setNegativeButton("close",null);
				photoPreviewDialog.show();

			}
		});

		deletePhoto = (ImageButton) findViewById(R.id.delete_photo);
		deletePhoto.setOnClickListener(new OnClickListener(){
			public void onClick(View v){

				sdCardHelper.deleteFile(tmpPhotoUri.getPath());
				hasMedia = false;
				setButtonStatus(true,false);
			}		
		});

		String[] filePaths = {tmpPhotoPath, finalPhotoPath};
		if(sdCardHelper.checkSDState(filePaths)){

			sdCardHelper.clearTempDirectory(tmpPhotoPath);
			setButtonStatus(true,false);
		}
		else setButtonStatus(false,false);
	}


	private void setupBasicButtons() {

		cancelButton = (Button) findViewById(R.id.tweet_cancel);
		cancelButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {				
				finish();		
			}

		});

		sendButton = (Button) findViewById(R.id.tweet_send);
		sendButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				new SendTweetTask().execute();				
			}

		});

		photoButton = (ImageButton) findViewById(R.id.tweet_photo);		
		photoButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(photoLayout.getVisibility() == View.GONE){
					photoLayout.setVisibility(View.VISIBLE);
					//photoButton.setImageResource(R.drawable.ic_menu_gallery_on);
				}
				else{
					photoLayout.setVisibility(View.GONE);
					//photoButton.setImageResource(R.drawable.ic_menu_gallery);
				}
			}
		});

		// User settings: do we use location or not?

		useLocation = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("prefUseLocation", Constants.TWEET_DEFAULT_LOCATION);

		locationButton = (ImageButton) findViewById(R.id.tweet_location);
		locationChecked = false;

		if(useLocation){
			locationButton.setImageResource(R.drawable.ic_menu_mylocation_on);
			locationChecked = true;
		}

		locationButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(!locationChecked){

					locHelper.registerLocationListener();
					Toast.makeText(NewTweetActivity.this, getString(R.string.location_on), Toast.LENGTH_SHORT).show();
					locationButton.setImageResource(R.drawable.ic_menu_mylocation_on);
					locationChecked = true;

				} else {

					locHelper.unRegisterLocationListener();
					Toast.makeText(NewTweetActivity.this, getString(R.string.location_off), Toast.LENGTH_SHORT).show();
					locationButton.setImageResource(R.drawable.ic_menu_mylocation);
					locationChecked = false;
				}
			}
		});

	}

	/**
	 * set button status with different operations
	 * 
	 * @param statusUpload
	 * @param statusDelete
	 */
	private void setButtonStatus(boolean statusUpload, boolean statusDelete){
		uploadFromGallery.setEnabled(statusUpload);
		uploadFromCamera.setEnabled(statusUpload);
		deletePhoto.setEnabled(statusDelete);
		previewPhoto.setEnabled(statusDelete);
		if(statusUpload){
			uploadFromGallery.setImageResource(R.drawable.ic_menu_slideshow);
			uploadFromCamera.setImageResource(R.drawable.ic_camera);
		}else{
			uploadFromGallery.setImageResource(R.drawable.ic_menu_slideshow_off);
			uploadFromCamera.setImageResource(R.drawable.ic_camera_off);
		}
		if(statusDelete){
			deletePhoto.setImageResource(R.drawable.ic_menu_delete);
			previewPhoto.setImageResource(R.drawable.ic_menu_zoom);
		}else{
			deletePhoto.setImageResource(R.drawable.ic_menu_delete_off);
			previewPhoto.setImageResource(R.drawable.ic_menu_zoom_off);
		}
	}
	
	/**
	 * onResume
	 */
	@Override
	public void onResume(){
		super.onResume();
		if(useLocation){
			locHelper.registerLocationListener();
		}
	}
	
	/**
	 * onPause
	 */
	@Override
	public void onPause(){
		super.onPause();
		locHelper.unRegisterLocationListener();
	}
	
	/**
	 * On Destroy
	 */
	@Override
	public void onDestroy(){
		super.onDestroy();
		Log.d(TAG, "onDestroy");
		if(hasMedia){
			sdCardHelper.deleteFile(tmpPhotoUri.getPath());
			hasMedia = false;
		}
		if (locHelper!= null) 
			locHelper.unRegisterLocationListener();	
		
		locationButton.setOnClickListener(null);
		locationButton = null;
		
		cancelButton.setOnClickListener(null);
		cancelButton = null;
		
		sendButton.setOnClickListener(null);
		sendButton = null;
		
		text.removeTextChangedListener(textWatcher);		
		textWatcher = null;
		
		TwimightBaseActivity.unbindDrawables(findViewById(R.id.showNewTweetRoot));
	}
	
	/**	
	 * Checks whether we are in disaster mode and inserts the content values into the content provider.
	 *
	 * @author pcarta
	 *
	 */
private class SendTweetTask extends AsyncTask<Void, Void, Boolean>{
		
		Uri insertUri = null;
		StatisticsDBHelper statsDBHelper;	
		
		@Override
		protected Boolean doInBackground(Void... params) {
			boolean result=false;
			
			//Statistics
			statsDBHelper = new StatisticsDBHelper(getApplicationContext());
			statsDBHelper.open();
			timestamp = System.currentTimeMillis();

			
			if(hasMedia){
				try {
					finalPhotoName = "twimight" + String.valueOf(timestamp) + ".jpg";
					photoUri = Uri.fromFile(sdCardHelper.getFileFromSDCard(finalPhotoPath, finalPhotoName));//photoFileParent, photoFilename));
					String fromFile = tmpPhotoUri.getPath();
					String toFile = photoUri.getPath();
					if (TwimightBaseActivity.D) Log.i(TAG, fromFile);
					if (TwimightBaseActivity.D) Log.i(TAG, toFile);
					if(sdCardHelper.copyFile(fromFile, toFile)){

						if (TwimightBaseActivity.D) Log.i(TAG, "file copy successful");

					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					if (TwimightBaseActivity.D) Log.d("photo", "exception!!!!!");
					e.printStackTrace();
				}
			}
			// if no connectivity, notify user that the tweet will be send later		
				
				ContentValues cv = createContentValues(); 
				
				if(PreferenceManager.getDefaultSharedPreferences(NewTweetActivity.this).getBoolean("prefDisasterMode", false) == true){				
					
					
					// our own tweets go into the my disaster tweets buffer
					cv.put(Tweets.COL_BUFFER, Tweets.BUFFER_TIMELINE|Tweets.BUFFER_MYDISASTER);

					insertUri = getContentResolver().insert(Uri.parse("content://" + Tweets.TWEET_AUTHORITY + "/" + Tweets.TWEETS + "/" 
																+ Tweets.TWEETS_TABLE_TIMELINE + "/" + Tweets.TWEETS_SOURCE_DISASTER), cv);
					getContentResolver().notifyChange(Tweets.TABLE_TIMELINE_URI, null);
				} else {				
					
					// our own tweets go into the timeline buffer
					cv.put(Tweets.COL_BUFFER, Tweets.BUFFER_TIMELINE);
					//we publish on twitter directly only normal tweets
					cv.put(Tweets.COL_FLAGS, Tweets.FLAG_TO_INSERT);	
					
					insertUri = getContentResolver().insert(Uri.parse("content://" + Tweets.TWEET_AUTHORITY + "/" + Tweets.TWEETS + "/" + 
																Tweets.TWEETS_TABLE_TIMELINE + "/" + Tweets.TWEETS_SOURCE_NORMAL), cv);
					getContentResolver().notifyChange(Tweets.TABLE_TIMELINE_URI, null);
					//getContentResolver().notifyChange(insertUri, null);
					ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
					if(cm.getActiveNetworkInfo()==null || !cm.getActiveNetworkInfo().isConnected()){
						result=true;
					}
				}
				if (locHelper.getCount() > 0 && cm.getActiveNetworkInfo()!= null) {	

					 Log.i(TAG,"writing log");
					statsDBHelper.insertRow(locHelper.getLocation(), cm.getActiveNetworkInfo().getTypeName(), 
							StatisticsDBHelper.TWEET_WRITTEN, null, timestamp);
					locHelper.unRegisterLocationListener();
					 Log.i(TAG, String.valueOf(hasMedia));
				}

				return result;
			
		}

		@Override
		protected void onPostExecute(Boolean result){
			if (result)
				Toast.makeText(NewTweetActivity.this, getString(R.string.no_connection4), Toast.LENGTH_SHORT).show();
			
			if(insertUri != null){
				// schedule the tweet for uploading to twitter
				Intent i = new Intent(NewTweetActivity.this, TwitterService.class);
				i.putExtra("synch_request", TwitterService.SYNCH_TWEET);
				i.putExtra("rowId", new Long(insertUri.getLastPathSegment()));
				startService(i);
			}
			finish();
		}
	}
	
	
	
	
	
	/**
	 * Prepares the content values of the tweet for insertion into the DB.
	 * @return
	 */
	private ContentValues createContentValues() {
		ContentValues tweetContentValues = new ContentValues();
		
		tweetContentValues.put(Tweets.COL_TEXT, text.getText().toString());

		tweetContentValues.put(Tweets.COL_TEXT_PLAIN, text.getText().toString());
		tweetContentValues.put(Tweets.COL_TWITTERUSER, LoginActivity.getTwitterId(this));
		tweetContentValues.put(Tweets.COL_SCREENNAME, LoginActivity.getTwitterScreenname(this));
		if (isReplyTo > 0) {
			tweetContentValues.put(Tweets.COL_REPLYTO, isReplyTo);
		}	
		// set the current timestamp
		tweetContentValues.put(Tweets.COL_CREATED, System.currentTimeMillis());
		
		
		if(useLocation){
			Location loc = locHelper.getLocation();
			if(loc!=null){
				tweetContentValues.put(Tweets.COL_LAT, loc.getLatitude());
				tweetContentValues.put(Tweets.COL_LNG, loc.getLongitude());
			}
		}
		//if there is a photo, put the path of photo in the cv
		if (hasMedia){
			tweetContentValues.put(Tweets.COL_MEDIA, finalPhotoName);
			Log.i(TAG, Tweets.COL_MEDIA + ":" + finalPhotoName);
		}
		
		return tweetContentValues;
	}
	
	

	
	
	//methods photo uploading
	
	/**
	 * upload photo from camera
	 */
	private void uploadFromCamera() {
		
		if((tmpPhotoUri = sdCardHelper.createTmpPhotoStoragePath(tmpPhotoPath)) != null){
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			
			intent.putExtra(MediaStore.EXTRA_OUTPUT, tmpPhotoUri);
			
			try {
				intent.putExtra("return-data", true);
				startActivityForResult(intent, PICK_FROM_CAMERA);
			} 
			catch (ActivityNotFoundException e) {
				e.printStackTrace();
			}
		}
		else{
			Log.i(TAG, "path for storing photos cannot be created!");
			setButtonStatus(false, false);
		}
		
	}
	
	/**
	 * upload photo by taking a picture
	 */
	private void uploadFromGallery(){
		if((tmpPhotoUri = sdCardHelper.createTmpPhotoStoragePath(tmpPhotoPath)) != null){
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			startActivityForResult(Intent.createChooser(intent, getString(R.string.picker)), PICK_FROM_FILE);
		}
		else{
			Log.i(TAG, "path for storing photos cannot be created!");
			setButtonStatus(false, false);
		}
		
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (resultCode != RESULT_OK) return;
	    setButtonStatus(false,true);
	    hasMedia = true;
	    switch (requestCode) {
		    case PICK_FROM_CAMERA:
		    	
		    	//display the picture		    	
		    	photo = sdCardHelper.decodeBitmapFile(tmpPhotoUri.getPath());
		    	mImageView.setImageBitmap(photo);

		    	break;

		    case PICK_FROM_FILE: 
		    	
		    	//display the photo
		    	Uri mImageGalleryUri = data.getData();
		    	
		    	//get the real path for chosen photo
		    	mImageGalleryUri = Uri.parse(sdCardHelper.getRealPathFromUri( (Activity) NewTweetActivity.this, mImageGalleryUri));
		    	
		    	//copy the photo from gallery to tmp directory

		    	String fromFile = mImageGalleryUri.getPath();
		    	String toFile = tmpPhotoUri.getPath();
				if(sdCardHelper.copyFile(fromFile, toFile)){
			    	photo = sdCardHelper.decodeBitmapFile(toFile);
			    	mImageView.setImageBitmap(photo);
				}
		    	break;    	
	    }
	}
	
}
