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

package ch.ethz.twimight.net.tds;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import ch.ethz.twimight.activities.TwimightBaseActivity;
import ch.ethz.twimight.security.RevocationListEntry;

/**
 * API for communication with the Twimight Disaster Server
 * @author thossmann
 *
 */
public class TDSCommunication {
	// the object names in requests and responses
	private static final String MESSAGE = "message";
	private static final String CERTIFICATE = "certificate";
	private static final String BLUETOOTH = "bluetooth";
	private static final String AUTHENTICATION = "authentication";
	private static final String VERSION = "version";
	private static final String REVOCATION = "revocation";
	private static final String FOLLOWER = "follower";	
	private static final String STATISTIC = "statistic";	
	private static final String DISASTER_TWEETS = "disaster_tweets";	
	private static final String NOTIFICATION = "notification";
	
	private static final String TAG = "TDSCommunication";
	private TDSRequestMessage tdsRequest;
	private TDSResponseMessage tdsResponse;
	
	/**
	 * In the constructor we create the request message and populate it with the mandatory objects
	 * @throws JSONException 
	 */
	public TDSCommunication(Context context, int consumerId, String oauthAccessToken, String oauthAccessTokenSecret) throws JSONException{
		tdsRequest = new TDSRequestMessage(context);
		tdsRequest.createAuthenticationObject(consumerId, oauthAccessToken, oauthAccessTokenSecret);
		
		tdsResponse = new TDSResponseMessage(context);
	}
	
	/**
	 * Creates a new Bluetooth object in the reqeust
	 * @return
	 * @throws JSONException
	 */
	public int createBluetoothObject(String mac) throws JSONException{
		tdsRequest.createBluetoothObject(mac);
		return 0;
	}
	

	
	/**
	 * Creates a new certiricate object in the request
	 * @return
	 * @throws JSONException
	 */
	public int createCertificateObject(KeyPair toSign, KeyPair toRevoke) throws Exception{
		tdsRequest.createCertificateObject(toSign, toRevoke);
		return 0;
	}
	

	
	/**
	 * Creates a new revocation object in the request
	 * @return
	 * @throws JSONException 
	 */
	public int createRevocationObject(int currentVersion) throws Exception{
		tdsRequest.createRevocationObject(currentVersion);
		return 0;
	}
	
	/**
	 * Creates a new follower object in the request
	 * @return
	 * @throws JSONException 
	 */
	public int createFollowerObject(long lastUpdate) throws Exception{
		tdsRequest.createFollowerObject(lastUpdate);
		return 0;
	}
	
	/**
	 * Creates a new Statistics object in the request
	 * @return
	 * @throws JSONException 
	 */
	public int createStatisticObject(Cursor stats, long follCount) throws Exception{
		tdsRequest.createStatisticObject(stats,follCount);
		return 0;
	}
	
	/**
	 * Creates a new disaster tweets object in the request
	 * @return
	 * @throws JSONException 
	 */
	public int createDisTweetsObject(Cursor tweets ) throws Exception{
		tdsRequest.createDisTweetsObject(tweets);
		return 0;
	}
	
	/**
	 * Sends the request to the Twimight disaster server. Blocking!
	 * @return
	 */
	public boolean sendRequest(HttpClient client, String url){
		
		// check the parameters
		if(client==null) return false;
		
		// first we fetch the request object
		JSONObject requestObject;
		try {
			requestObject = assembleRequest();
		} catch (Exception e) {
			if (TwimightBaseActivity.D) Log.e(TAG, "JSON exception while assembling request!");
			return false;
		}
		
		// do we have a request object?
		if(requestObject== null) return false;
		

		// create the HTTP request
		HttpPost post = new HttpPost(url);

		// now we serialize the JSON Object into the post request
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(MESSAGE,requestObject.toString()));

		UrlEncodedFormEntity ent = null;
		try {
			ent = new UrlEncodedFormEntity(params);
		} catch (UnsupportedEncodingException e) {
			if (TwimightBaseActivity.D) Log.e(TAG,"Unsupported Encoding Exception!",e);
			return false;
		}

		post.setEntity(ent);			

		// and make the actual request!
		HttpResponse response = null;
		try {
			response = client.execute(post);
		} catch (ClientProtocolException e) {
			if (TwimightBaseActivity.D) Log.d(TAG,"HTTP POST request failed! " + e.toString());
			return false;
		} catch (IOException e) {
			if (TwimightBaseActivity.D) Log.d(TAG,"HTTP POST request failed!" + e.toString());
			return false;
		}	

		// read the response
		HttpEntity resEntity = response.getEntity();
		
		if (resEntity != null) {		
			
			String result = null;
			try {
				result = EntityUtils.toString(resEntity);
			} catch (ParseException e) {
				if (TwimightBaseActivity.D) Log.e(TAG,"Parse Error while parsing response!" + e.toString());
				return false;
			} catch (IOException e) {
				if (TwimightBaseActivity.D) Log.e(TAG,"IO Error while parsing response!" + e.toString());
				return false;
			}
			if (TwimightBaseActivity.D) Log.i(TAG,"result = " + result);

			try {
				if(disassembleResponse(result) != 0){
					if (TwimightBaseActivity.D) Log.e(TAG, "Error while parsing result");
				}
				
			} catch (Exception e) {
				if (TwimightBaseActivity.D) Log.e(TAG,"JSON Error while parsing result!" + e.toString());
				return false;
			}					
		} else 
			return false;
		
		return true;
		
	}
	
	/**
	 * Assemples the request to one big JSON Object. Returns null if mandatory fields are missing.
	 * @throws JSONException 
	 */
	private JSONObject assembleRequest() throws Exception{
		JSONObject requestObject = new JSONObject();
		
		// first, we add the version
		if(tdsRequest.hasVersion()){
			requestObject.put(VERSION, tdsRequest.getVersion());
		} else {
			return null;
		}
		
		// the authentication
		if(tdsRequest.hasAuthenticationObject()){
			requestObject.put(AUTHENTICATION, tdsRequest.getAuthenticationObject());
		} else {
			return null;
		}		
	
		// bluetooth
		if(tdsRequest.hasBluetoothObject()){
			requestObject.put(BLUETOOTH, tdsRequest.getBluetoothObject());
		}		

		// certificate
		if(tdsRequest.hasCertificatObject()){
			requestObject.put(CERTIFICATE, tdsRequest.getCertificateObject());
		}

		// revocation
		if(tdsRequest.hasRevocationObject()){
			requestObject.put(REVOCATION, tdsRequest.getRevocationObject());
		}

		// follower
		if(tdsRequest.hasFollowerObject()){
			requestObject.put(FOLLOWER, tdsRequest.getFollowerObject());
		}
		
		// statistics
		if(tdsRequest.hasStatisticObject()){
			requestObject.put(STATISTIC, tdsRequest.getStatisticObject());
		}
		

		// disaster tweets
		if(tdsRequest.hasDisTweetsObject()){
			requestObject.put(DISASTER_TWEETS, tdsRequest.getDisTweetsObject());
		}

		if (TwimightBaseActivity.D) Log.i(TAG, requestObject.toString(5));
		return requestObject;
	}
	
	private int disassembleResponse(String responseString) throws Exception{
		
		JSONObject responseObject = new JSONObject(responseString);
		JSONObject messageObject = responseObject.getJSONObject(MESSAGE);		
		
		if(messageObject == null) return -1;	
		
		// version
		int responseVersion = messageObject.getInt("version"); 
		if(responseVersion != tdsResponse.getVersion()){
			if (TwimightBaseActivity.D) Log.e(TAG, "TDS message version mismatch!");
			return -1;
		}
		
		// authentication
		try {
			JSONObject authenticationObject = messageObject.getJSONObject(AUTHENTICATION);			
			if(authenticationObject != null){				
				tdsResponse.setAuthenticationObject(authenticationObject);
			} else {
				if (TwimightBaseActivity.D) Log.e(TAG, "Authentication failed");
				return -1;
			}
			
		} catch (JSONException ex) {}
		
	
		
		try{
			// bluetooth
			JSONObject bluetoothObject = messageObject.getJSONObject(BLUETOOTH);
			tdsResponse.setBluetoothObject(bluetoothObject);
		} catch(JSONException e) {
			if (TwimightBaseActivity.D) Log.i(TAG, "No Bluetooth object");
		}		

		try{
			// certificate
			JSONObject certificateObject = messageObject.getJSONObject(CERTIFICATE);
			tdsResponse.setCertificateObject(certificateObject);
		} catch(JSONException e){
			if (TwimightBaseActivity.D) Log.i(TAG, "No certificate object");
		}

		try{
			// revocation
			JSONObject revocationObject = messageObject.getJSONObject(REVOCATION);
			tdsResponse.setRevocationObject(revocationObject);
		} catch(JSONException e){
			if (TwimightBaseActivity.D) Log.i(TAG, "No revocation object");
		}

		try{
			// follower
			JSONObject followerObject = messageObject.getJSONObject(FOLLOWER);
			tdsResponse.setFollowerObject(followerObject);
		} catch(JSONException e){
			if (TwimightBaseActivity.D) Log.i(TAG, "No follower object");
		}
		
		try{
			// notification
			JSONObject notificationObject = messageObject.getJSONObject(NOTIFICATION);
			tdsResponse.setNotificationObject(notificationObject);
		} catch(JSONException e){
			if (TwimightBaseActivity.D) Log.i(TAG, "No notification object");
		}

		return 0;
	}
	
	public String parseAuthentication() throws Exception{
		return tdsResponse.parseAuthentication();
	}
	
	public JSONObject getNotification() throws Exception{
		return tdsResponse.getNotification();
	}
	
	public Map<Long,Long> parseDisTweets() throws Exception{
		return tdsResponse.parseDisTweetsResponse();
	}
	
	public String parseCertificate() throws Exception{
		return tdsResponse.parseCertificate();
	}

	public int parseCertificateStatus() throws Exception{
		return tdsResponse.parseCertificateStatus();
	}

	public List<RevocationListEntry> parseRevocation() throws Exception{
		return tdsResponse.parseRevocation();
	}

	public int parseRevocationVersion() throws Exception{
		return tdsResponse.parseRevocationVersion();
	}

	public List<TDSPublicKey> parseFollower() throws Exception{
		return tdsResponse.parseFollower();
	}


	public long parseFollowerLastUpdate() throws Exception{
		return tdsResponse.parseFollowerLastUpdate();
	}


	
}
