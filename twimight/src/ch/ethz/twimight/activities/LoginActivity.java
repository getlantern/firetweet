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

import junit.framework.Assert;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import ch.ethz.bluetest.credentials.Obfuscator;
import ch.ethz.twimight.R;
import ch.ethz.twimight.data.DBOpenHelper;
import ch.ethz.twimight.data.RevocationDBHelper;
import ch.ethz.twimight.net.opportunistic.ScanningAlarm;
import ch.ethz.twimight.net.tds.TDSAlarm;
import ch.ethz.twimight.net.tds.TDSService;
import ch.ethz.twimight.net.twitter.TwitterAlarm;
import ch.ethz.twimight.net.twitter.TwitterService;
import ch.ethz.twimight.security.CertificateManager;
import ch.ethz.twimight.security.KeyManager;
import ch.ethz.twimight.util.Constants;
import ch.ethz.twimight.util.TwimightSuggestionProvider;

/**
 * Logging the user in and out.
 * Different things can happen, depending on whether we have (i) tokens and/or (ii) connectivity:
 * Tokens, Connectivity: Start the Timeline. In the background verify the tokens and report only on error.
 * Tokens, no Connectivity: Start the Timeline. Display Toast about lack of connectivity.
 * No tokens: whether or not we have connectivity, we show the login button.
 * TODO: Dump the state in a file upon logout and read it again when logging in.
 * @author thossmann
 *
 */
public class LoginActivity extends Activity implements OnClickListener{

	private static final String TAG = "LoginActivity"; /** For logging */
	
	// shared preferences
		public static final String TWITTER_ID = "twitter_id"; /** Name of Twitter ID in shared preferences */
		private static final String TWITTER_SCREENNAME = "twitter_screenname"; /** Name of Twitter screenname in shared preferences */
		
		private static final String TWITTER_ACCESS_TOKEN = "twitter_access_token"; /** Name of access token in preference */
		private static final String TWITTER_ACCESS_TOKEN_SECRET = "twitter_access_token_secret"; /** Name of secret in preferences */

		private static final String TWITTER_REQUEST_TOKEN = "twitter_request_token"; /** Name of the request token in preferences */
		private static final String TWITTER_REQUEST_TOKEN_SECRET = "twitter_request_token_secret"; /** Name of the request token secret in preferences */
		
		// twitter urls
		private static final String TWITTER_REQUEST_TOKEN_URL = "https://api.twitter.com/oauth/request_token"; 
		private static final String TWITTER_ACCESS_TOKEN_URL = "https://api.twitter.com/oauth/access_token";
		private static final String TWITTER_AUTHORIZE_URL = "https://api.twitter.com/oauth/authorize";
		private static final Uri CALLBACK_URI = Uri.parse("my-app://bluetest");
		
		public static final String LOGIN_RESULT_ACTION = "twitter_login_result_action";
		public static final String LOGIN_RESULT = "twitter_login_result";
		public static final int LOGIN_SUCCESS = 1;
		public static final int LOGIN_FAILURE = 2;
		
		// views
		Button buttonLogin;
		LinearLayout showLoginLayout;

		private ProgressDialog progressDialog;
		private LoginReceiver loginReceiver;
		private static PendingIntent restartIntent;
		private static LoginActivity instance = null; /** The single instance of this class */
		
		
		
		/** 
		 * Called when the activity is first created. 
		 */
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);		
			setContentView(R.layout.login);
			
			
			setRestartIntent(PendingIntent.getActivity(this.getBaseContext(), 0, 
					new Intent(getIntent()), getIntent().getFlags()));
			instance = this;
			
			// which state are we in?
			if(hasAccessToken(this) && hasAccessTokenSecret(this) && getTwitterId(this)!=null){
				// if we have token, secret and ID: launch the timeline activity
				
				// Do we have connectivity?
				ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
				if(cm.getActiveNetworkInfo()==null || !cm.getActiveNetworkInfo().isConnected()){
					Toast.makeText(this,getString(R.string.no_connection), Toast.LENGTH_LONG).show();
				}
				startTimeline(getApplicationContext());
				
			} else if(hasAccessToken(this) && hasAccessTokenSecret(this)) {
				
				// we verify the tokens and retrieve the twitter ID
				Intent i = new Intent(TwitterService.SYNCH_ACTION);
				i.putExtra("synch_request", TwitterService.SYNCH_LOGIN);
				registerLoginReceiver();
				startService(i);
				removeLoginInterface();
				
			} else if(hasRequestToken(this) && hasRequestTokenSecret(this)) {
				
				// We get the URI when we are called back from Twitter
				Uri uri = getIntent().getData();
				if(uri != null){
					
					removeLoginInterface();
					new GetAccessTokensTask().execute(uri);
				} else {
					
					// Delete Request token and secret
					setRequestToken(null, this);
					setRequestTokenSecret(null, this);
					setupLoginButton();				
				}

			} else {
				// if we don't have request token and secret, we show the login button
					
				setupLoginButton();
			}	
			
		}
		
		private void removeLoginInterface(){
			buttonLogin = (Button) findViewById(R.id.buttonLogin);
			showLoginLayout = (LinearLayout) findViewById(R.id.showLoginLogo);
			buttonLogin.setVisibility(Button.GONE);
			showLoginLayout.setVisibility(LinearLayout.GONE);
		}
		
		private void setupLoginButton() {
			buttonLogin = (Button) findViewById(R.id.buttonLogin);
			buttonLogin.setEnabled(true);	
			buttonLogin.setOnClickListener(this);
			
		}

		/**
		 * Method used to register a login Receiver
		 * @author pcarta	 
		 */
		private void registerLoginReceiver() {
			if (loginReceiver == null) loginReceiver = new LoginReceiver();
			IntentFilter intentFilter = new IntentFilter(LoginActivity.LOGIN_RESULT_ACTION);
			registerReceiver(loginReceiver, intentFilter);
		}
		
		
		
		/**
		 * When the login button is pressed
		 */
		@Override
		public void onClick(View view) {
			switch (view.getId()) {		
			case R.id.buttonLogin:
				ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
				if(cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()){
					// disabling button
					buttonLogin.setEnabled(false);
										
					new GetRequestTokenTask().execute();
				} else {
					Toast.makeText(this,getString(R.string.no_connection2), Toast.LENGTH_LONG).show();
				}
				break;						
			}
		}
		
		
		/**
		 * onDestroy
		 */
		@Override
		public void onDestroy(){
			super.onDestroy();	
			
			if (loginReceiver != null) unregisterReceiver(loginReceiver);
			
			// null the onclicklistener of the button
			if(buttonLogin != null){
				buttonLogin.setOnClickListener(null);
			}
			TwimightBaseActivity.unbindDrawables(findViewById(R.id.showLoginRoot));
			
		}
		
		/**
		 * Upon pressing the login button, we first get Request tokens from Twitter.
		 * @param context
		 */
		
		private class GetRequestTokenTask extends AsyncTask<Void,Void,String> {

			@Override
			protected String doInBackground(Void... params) {
				

				OAuthConsumer consumer = new CommonsHttpOAuthConsumer(Obfuscator.getKey(),Obfuscator.getSecret());		
				OAuthProvider provider = new CommonsHttpOAuthProvider (TWITTER_REQUEST_TOKEN_URL,TWITTER_ACCESS_TOKEN_URL,TWITTER_AUTHORIZE_URL);

				provider.setOAuth10a(true);
				
				try {				
					String authUrl = provider.retrieveRequestToken(consumer, CALLBACK_URI.toString());
					setRequestToken(consumer.getToken(), LoginActivity.this);
					setRequestTokenSecret(consumer.getTokenSecret(), LoginActivity.this);
					
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
					intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY); 

					// Show twitter login in Browser.
				    startActivity(intent);
					finish();
					
					// now we have the request token.
				} catch (OAuthMessageSignerException e) {
					e.printStackTrace();						
					return getString(R.string.error_signing);
					
				} catch (OAuthNotAuthorizedException e) {
					e.printStackTrace();		
					return getString(R.string.error_twitter);
							
				} catch (OAuthExpectationFailedException e) {
					e.printStackTrace();							
					return getString(R.string.error_parameters) ;
							
				} catch (OAuthCommunicationException e) {
					e.printStackTrace();						
					return getString(R.string.error_server);
				}
				
				return null;
			}
			
			@Override
			protected void onPostExecute(String result) {
				if (result != null) {
					Toast.makeText(LoginActivity.this, result, Toast.LENGTH_LONG).show();
					buttonLogin.setEnabled(true);
				}
			}
			
		}
		

		
		private class GetAccessTokensTask extends AsyncTask<Uri,Void,String> {
			boolean success = false;		

			@Override
			protected void onPreExecute() {
				
				super.onPreExecute();
				progressDialog=ProgressDialog.show(LoginActivity.this, getString(R.string.in_progress), getString(R.string.verifying));
			}

			@Override
			protected String doInBackground(Uri... params) {
				

				Uri uri = params[0];
				
				String requestToken = getRequestToken(LoginActivity.this);
				String requestSecret = getRequestTokenSecret(LoginActivity.this);

				OAuthConsumer consumer = new CommonsHttpOAuthConsumer(Obfuscator.getKey(),Obfuscator.getSecret());		
				OAuthProvider provider = new CommonsHttpOAuthProvider (TWITTER_REQUEST_TOKEN_URL,TWITTER_ACCESS_TOKEN_URL,TWITTER_AUTHORIZE_URL);

				provider.setOAuth10a(true);			
				
				String accessToken = null;
				String accessSecret = null;
				
				try {
					if(!(requestToken == null || requestSecret == null)) {
						consumer.setTokenWithSecret(requestToken, requestSecret);
					}
					
					String otoken = uri.getQueryParameter(OAuth.OAUTH_TOKEN);
					String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);

					// This is a sanity check which should never fail - hence the assertion
					Assert.assertEquals(otoken, consumer.getToken());

					// This is the moment of truth - we could throw here				
					provider.retrieveAccessToken(consumer, verifier);
					
					// Now we can retrieve the goodies
					accessToken = consumer.getToken();
					accessSecret = consumer.getTokenSecret();
					
					success = true;
					
				} catch (OAuthMessageSignerException e) {
					e.printStackTrace();				
					success = false;
					finish();
					return getString(R.string.error_authentication);
					
				} catch (OAuthNotAuthorizedException e) {
					e.printStackTrace();				
					success = false;
					finish();
					return getString(R.string.error_authentication);
					
				} catch (OAuthExpectationFailedException e) {
					e.printStackTrace();				
					success = false;
					finish();
					return getString(R.string.error_authentication);
					
				} catch (OAuthCommunicationException e) {
					e.printStackTrace();			
					success = false;
					finish();
					return getString(R.string.error_authentication);
					
				} finally {
				
					// save the access token and secret
					setAccessToken(accessToken, LoginActivity.this);
					setAccessTokenSecret(accessSecret, LoginActivity.this);

					// Clear the request token and secret
					setRequestToken(null, LoginActivity.this);
					setRequestTokenSecret(null, LoginActivity.this);
					
				}
				
				return null;
				
			}
			
			@Override
			protected void onPostExecute(String result) {
				if (result != null) {
					Toast.makeText(LoginActivity.this, result, Toast.LENGTH_LONG).show();
					
				}
				// As a last step, we verify the correctness of the credentials and retrieve our Twitter ID
				if(success){
					SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit();
	                edit.putBoolean("isFirstLogin", true);
	                edit.commit();

					// call the twitter service to verify the credentials
					Intent i = new Intent(TwitterService.SYNCH_ACTION);
					i.putExtra("synch_request", TwitterService.SYNCH_LOGIN);
					registerLoginReceiver();
					startService(i);
					
				}
			}
			
		}
		
		

		private void startTimeline(Context context) {		
			Intent i = new Intent(context, ShowTweetListActivity.class);
			
			i.putExtra("login", true);		
			startActivity(i);		
			startAlarms(context);
			finish();
		}

		/**
		 * Start all the enabled alarms and services.
		 */
		public static void startAlarms(Context context) {
			
			// Start the alarm for communication with the TDS
			if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.prefTDSCommunication), 
					Constants.TDS_DEFAULT_ON)==true){
				
				new TDSAlarm(context, Constants.TDS_UPDATE_INTERVAL);
			}		
			
			if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.prefDisasterMode), 
					Constants.DISASTER_DEFAULT_ON)==true){
				
				new ScanningAlarm(context,false);
			}		
			
			//start the twitter update alarm
			if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.prefRunAtBoot), 
					Constants.TWEET_DEFAULT_RUN_AT_BOOT)==true){
				
				new TwitterAlarm(context,true);
			}
							
		}
		
		/**
		 * Stop all the alarms and services
		 */
		private static void stopServices(Context context) {

			TDSAlarm.stopTDSCommuniction(context);
			context.stopService(new Intent(context, TDSService.class));
			
			ScanningAlarm.stopScanning(context);
			
			context.stopService(new Intent(context, TwitterService.class));	
			
			TwitterAlarm.stopTwitterAlarm(context);
		}
		
		/**
		 * Upon pressing the login button, we first get Request tokens from Twitter.
		 * @param context
		 */
		
		  static class PerformLogoutTask extends AsyncTask<Context,Void,Void> {

			@Override
			protected Void doInBackground(Context... params) {
				// Stop all services and pending alarms
				Context context = params[0];
				stopServices(context);
				
				// Delete persistent Twitter update information
				TwitterService.setFavoritesSinceId(null, context);
				TwitterService.setLastFavoritesUpdate(null, context);
				TwitterService.setMentionsSinceId(null, context);
				TwitterService.setLastMentionsUpdate(null, context);
				TwitterService.setTimelineSinceId(null, context);
				TwitterService.setLastTimelineUpdate(0, context);
				TwitterService.setLastFriendsUpdate(null, context);
				TwitterService.setLastFollowerUpdate(null, context);
				TwitterService.setLastDMsInUpdate(null, context);
				TwitterService.setLastDMsOutUpdate(null, context);
				TwitterService.setDMsOutSinceId(null, context);
				TwitterService.setDMsInSinceId(null, context);
				
				TDSService.resetLastUpdate(context);
				TDSService.resetUpdateInterval(context);
				
				// Delete our Twitter ID and screenname
				setTwitterId(null, context);
				setTwitterScreenname(null, context);
				
				// Delete Access token and secret
				setAccessToken(null, context);
				setAccessTokenSecret(null, context);
				
				// Delete Request token and secret
				setRequestToken(null, context);
				setRequestTokenSecret(null, context);
				
				// Delete key and certificate
				KeyManager km = new KeyManager(context);
				km.deleteKey();
				CertificateManager cm = new CertificateManager(context);
				cm.deleteCertificate();
				
				// Flush DB
				DBOpenHelper dbHelper = DBOpenHelper.getInstance(context);
				dbHelper.flushDB();
				
				// Flush revocation list
				RevocationDBHelper rm = new RevocationDBHelper(context);
				rm.open();
				rm.flushRevocationList();
				
				SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context,
		                TwimightSuggestionProvider.AUTHORITY, TwimightSuggestionProvider.MODE);
				suggestions.clearHistory();
				
				// Start login activity
				Intent intent = new Intent(context, LoginActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
				return null;
			}
			
			
			
		}
		
		/**
		 * Deleting all the state
		 */
		public static void logout(Context context){
			new PerformLogoutTask().execute(context);
			
			
		}
		
		/**
		 * Saves a token (in string format) to shared prefs.
		 */
		public static void setAccessToken(String token, Context context){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			SharedPreferences.Editor prefEditor = prefs.edit();
			prefEditor.putString(TWITTER_ACCESS_TOKEN, token);
			prefEditor.commit();
			
		}
		
		/**
		 * Saves a secret (in string format) to shared prefs.
		 * @param secret
		 * @param context
		 */
		public static void setAccessTokenSecret(String secret, Context context){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			SharedPreferences.Editor prefEditor = prefs.edit();
			prefEditor.putString(TWITTER_ACCESS_TOKEN_SECRET, secret);
			prefEditor.commit();
		}
		
		/**
		 * Gets the twitter access token from the shared preferences.
		 * @param context
		 * @return
		 */
		public static String getAccessToken(Context context){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			return prefs.getString(TWITTER_ACCESS_TOKEN, null);
		}
		
		/**
		 * Returns the secret stored in shared preferences
		 * @param context
		 * @return
		 */
		public static String getAccessTokenSecret(Context context){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			return prefs.getString(TWITTER_ACCESS_TOKEN_SECRET, null);
		}
		
		/**
		 * True if we have an access token in the shared preferences, false otherwise
		 * @param context
		 * @return
		 */
		public static boolean hasAccessToken(Context context){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			return prefs.getString(TWITTER_ACCESS_TOKEN, null)!= null;
		}
		
		/**
		 * True if we have a secret in the shared preferences, false otherwise
		 * @param context
		 * @return
		 */
		public static boolean hasAccessTokenSecret(Context context){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			return prefs.getString(TWITTER_ACCESS_TOKEN_SECRET, null)!=null;
		}
		
		/**
		 * Saves a request token (in string format) to shared prefs.
		 */
		public static void setRequestToken(String token, Context context){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			SharedPreferences.Editor prefEditor = prefs.edit();
			prefEditor.putString(TWITTER_REQUEST_TOKEN, token);
			prefEditor.commit();
			
		}
		
		/**
		 * Saves a secret (in string format) to shared prefs.
		 * @param secret
		 * @param context
		 */
		public static void setRequestTokenSecret(String secret, Context context){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			SharedPreferences.Editor prefEditor = prefs.edit();
			prefEditor.putString(TWITTER_REQUEST_TOKEN_SECRET, secret);
			prefEditor.commit();
		}
		
		/**
		 * True if we have a request token in the shared preferences, false otherwise
		 * @param context
		 * @return
		 */
		public static boolean hasRequestToken(Context context){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			return prefs.getString(TWITTER_REQUEST_TOKEN, null)!= null;
		}
		
		/**
		 * True if we have a request token secret in the shared preferences, false otherwise
		 * @param context
		 * @return
		 */
		public static boolean hasRequestTokenSecret(Context context){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			return prefs.getString(TWITTER_REQUEST_TOKEN_SECRET, null)!=null;
		}
		
		/**
		 * Gets the twitter request token from the shared preferences.
		 * @param context
		 * @return
		 */
		public static String getRequestToken(Context context){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			return prefs.getString(TWITTER_REQUEST_TOKEN, null);
		}
		
		/**
		 * Returns the secret stored in shared preferences
		 * @param context
		 * @return
		 */
		public static String getRequestTokenSecret(Context context){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			return prefs.getString(TWITTER_REQUEST_TOKEN_SECRET, null);
		}
		
		/**
		 * Stores the local Twitter ID in the shared preferences
		 * @param id
		 * @param context
		 */
		public static void setTwitterId(String id, Context context) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			SharedPreferences.Editor prefEditor = prefs.edit();
			prefEditor.putString(TWITTER_ID, id);
			prefEditor.commit();
		}
		
		/**
		 * Gets the Twitter ID from shared preferences
		 * @param context
		 * @return
		 */
		public static String getTwitterId(Context context) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			return prefs.getString(TWITTER_ID, null);
		}
		
		/**
		 * Do we have a Twitter ID in shared preferences?
		 * @param context
		 * @return
		 */
		public static boolean hasTwitterId(Context context) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			return prefs.getString(TWITTER_ID, null)!=null;
		}
		
		/**
		 * Stores the local Twitter screenname in the shared preferences
		 * @param id
		 * @param context
		 */
		public static void setTwitterScreenname(String screenname, Context context) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			SharedPreferences.Editor prefEditor = prefs.edit();
			prefEditor.putString(TWITTER_SCREENNAME, screenname);
			prefEditor.commit();
		}
		
		/**
		 * Gets the Twitter screenname from shared preferences
		 * @param context
		 * @return
		 */
		public static String getTwitterScreenname(Context context) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			return prefs.getString(TWITTER_SCREENNAME, null);
		}
		
		
		
		/**
		 * returns the one instance of this activity
		 */
		public static LoginActivity getInstance() {
			return instance;
		}
		
		/**
		 * @param restartIntent the restartIntent to set
		 */
		public static void setRestartIntent(PendingIntent restartIntent) {
			LoginActivity.restartIntent = restartIntent;
		}

		/**
		 * @return the restartIntent
		 */
		public static PendingIntent getRestartIntent() {
			return restartIntent;
		}
		
		/**
		 * Listens to login results from the Twitter service (verify credentials)
		 * @author thossmann
		 *
		 */
		private class LoginReceiver extends BroadcastReceiver {
		    @Override
		    public void onReceive(Context context, Intent intent) {
		    	if (intent != null)
		    		if(intent.getAction() != null) {
		    			
		    			if (intent.getAction().equals(LoginActivity.LOGIN_RESULT_ACTION)) {
		    	        	
		    	        	if(intent.hasExtra(LoginActivity.LOGIN_RESULT)){
		    	        		if (progressDialog != null)
		    	        			progressDialog.dismiss();
		    	        		if(intent.getIntExtra(LoginActivity.LOGIN_RESULT, LoginActivity.LOGIN_FAILURE)==LoginActivity.LOGIN_SUCCESS){	        			
		    	        			startTimeline(context);
		    	        		} else {
		    	        			Toast.makeText(getBaseContext(), getString(R.string.error_login), Toast.LENGTH_SHORT).show();
		    	        			
		    	        			
		    	        		}
		    	        	}
		    	        }
		    		}
		        
		    }
		}

		
	
	
}
