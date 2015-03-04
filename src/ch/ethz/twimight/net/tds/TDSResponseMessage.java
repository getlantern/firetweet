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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.jce.provider.X509CertificateObject;

import android.content.Context;
import android.util.Log;
import ch.ethz.twimight.activities.TwimightBaseActivity;
import ch.ethz.twimight.security.CertificateManager;
import ch.ethz.twimight.security.RevocationListEntry;
import ch.ethz.twimight.util.Constants;

public class TDSResponseMessage {

	private static final String TAG = "TDSResponseMessage";

	private int version;
	
	private JSONObject authenticationObject;
	private JSONObject locationObject;
	private JSONObject bluetoothObject;
	private JSONObject certificateObject;
	private JSONObject revocationObject;
	private JSONObject followerObject;
	private JSONObject notificationObject;
	private JSONObject disTweetsObject;
	
	private Context context;
	
	/**
	 * Constructor
	 */
	public TDSResponseMessage(Context context){
		version = Constants.TDS_MESSAGE_VERSION;
		this.context = context;
	}

	public void setAuthenticationObject(JSONObject authenticationObject) {		
		this.authenticationObject = authenticationObject;
	}	
	
	
	public void setBluetoothObject(JSONObject bluetoothObject){
		this.bluetoothObject = bluetoothObject;
	}
	
	
	public void setCertificateObject(JSONObject certificateObject){
		this.certificateObject = certificateObject;
	}
	
	public void setRevocationObject(JSONObject revocationObject) {
		this.revocationObject = revocationObject;
	}

	public void setFollowerObject(JSONObject followerObject) {
		this.followerObject = followerObject;
	}
	
	public void setNotificationObject(JSONObject notificationObject) {
		this.notificationObject = notificationObject;
	}
	
	public void setDisTweetsObject(JSONObject disTweetsObject) {
		this.disTweetsObject = disTweetsObject;
	}

	public boolean hasAuthenticationObject(){
		return authenticationObject != null;
	}

	public boolean hasBluetoothObject(){
		return bluetoothObject != null;
	}	
	
	public boolean hasCertificateObject(){
		return certificateObject != null;
	}

	public boolean hasRevocationObject(){
		return revocationObject != null;
	}

	public boolean hasFollowerObject(){
		return followerObject != null;
	}	

	public boolean hasNotificationObject(){
		return notificationObject != null;
	}
	
	public boolean hasDisTweetsObject(){
		return disTweetsObject != null;
	}
	
	/**
	 * Parses the Notification object
	 */
	public JSONObject getNotification() throws JSONException{

		if(!hasNotificationObject()) 
			return null;
		else return notificationObject;	
		
		
	}
	
	
	
	/**
	 * Parses the authentication object
	 */
	public Map<Long,Long> parseDisTweetsResponse() throws JSONException{
		
		Log.i(TAG,"parsing dis tweets result");
		if(!hasDisTweetsObject()) return null;
		
		return new HashMap<Long,Long>();
		
	}
	

	/**
	 * Parses the authentication object
	 */
	public String parseAuthentication() throws JSONException{

		if(!hasAuthenticationObject()) return null;
		
		String twitterId = authenticationObject.getString("twitter_id");
		if (TwimightBaseActivity.D) Log.i(TAG,"showing twitter id inside response");
		return twitterId;
		
	}
	
	
		
	/**
	 * Requests a list of MAC addresses of potential Bluetooth peers 
	 */
	public int getBluetoothResponse() throws JSONException{

		if(!hasBluetoothObject()) return -1;

		int statusCode = bluetoothObject.getInt("status"); 
		return statusCode;
		
		
	}
	
	/**
	 * Parses the returned certificate object and stores the certificate .
	 * Returns the PEM encoding of the certificate for easy handling.
	 * @throws JSONException 
	 */
	public String parseCertificate() throws JSONException{

		if(!hasCertificateObject()) return null;
		
		int statusCode =certificateObject.getInt("status"); 
		if(statusCode != 200) {
			if (TwimightBaseActivity.D) Log.e(TAG, "TDS returned certificate status error code" + statusCode);
			return null;
		}

		// check and return the certificate
		CertificateManager cm = new CertificateManager(context);
		String pemString = certificateObject.getString("pem");
		
		X509CertificateObject cert = cm.parsePem(pemString);
		if(cm.checkCertificate(cert)){
			return pemString;
		}
		
		return null;
		
	}
	
	/**
	 * Returns the status code of the certificate object
	 * @return
	 */
	public int parseCertificateStatus() throws JSONException{
		if(!hasCertificateObject()) return 0;
		
		return certificateObject.getInt("status");
	}

	
	/**
	 * Extract the list of new entries for the revocation list.
	 * @return
	 * @throws JSONException
	 */
	public List<RevocationListEntry> parseRevocation() throws JSONException{
		if(!hasRevocationObject()) return null;

		int statusCode = revocationObject.getInt("status"); 
		if(statusCode != 200) {
			if (TwimightBaseActivity.D) Log.e(TAG, "TDS returned revocation status error code" + statusCode);
			return null;
		}
		
		List<RevocationListEntry> revocationList = new ArrayList<RevocationListEntry>();
		JSONArray entries = revocationObject.getJSONArray("update");
		
		if(entries!= null){
			for(int i = 0 ; i < entries.length(); i++){
				JSONArray entry = entries.getJSONArray(i);
				String serial = entry.getString(0);
				Date until = new Date(1000*entry.getLong(1)); // we get the date in Seconds since 1970
				revocationList.add(new RevocationListEntry(serial, until));
			}
		}

		return revocationList;
	}
	
	/**
	 * Reads the version number of the revocation list in the message
	 * @return
	 * @throws JSONException
	 */
	public int parseRevocationVersion() throws JSONException{
		if(!hasRevocationObject()) return 0;

		int statusCode = revocationObject.getInt("status"); 
		if(statusCode != 200) {
			if (TwimightBaseActivity.D) Log.e(TAG, "TDS returned revocation status error code" + statusCode);
			return 0;
		}
		return revocationObject.getInt("version");
	}
	
	/**
	 * Reads the public keys in the follower object
	 * @return
	 * @throws JSONException
	 */
	public List<TDSPublicKey> parseFollower() throws JSONException{
		if(!hasFollowerObject()) return null;

		if (TwimightBaseActivity.D) Log.d(TAG,"reading followers");
		int statusCode = followerObject.getInt("status"); 
		if(statusCode != 200) {
			if (TwimightBaseActivity.D) Log.e(TAG, "TDS returned follower status error code" + statusCode);
			return null;
		}

		List<TDSPublicKey> keyList = null;
		
		if(followerObject.has("update")){
			keyList = new ArrayList<TDSPublicKey>();
			JSONArray entries = followerObject.getJSONArray("update");
			
			if (TwimightBaseActivity.D) Log.i(TAG, "reading update");
			
			if(entries!= null){
				for(int i = 0 ; i < entries.length(); i++){
					JSONArray entry = entries.getJSONArray(i);
					long twitterId = entry.getLong(0);
					String keyPem = entry.getString(1);
					if(twitterId!=0 && keyPem!=null){
						keyList.add(new TDSPublicKey(twitterId, keyPem));
					}
				}
			}
		}
		
		return keyList;

	}

	/**
	 * Reads the last_update field in the follower object
	 * @return
	 * @throws JSONException
	 */
	public long parseFollowerLastUpdate() throws JSONException {
		if(!hasFollowerObject()) return 0;

		int statusCode = followerObject.getInt("status"); 
		if(statusCode != 200) {
			if (TwimightBaseActivity.D) Log.e(TAG, "TDS returned follower status error code" + statusCode);
			return 0;
		}
		
		return followerObject.getLong("last_update");
	}

	
	public int getVersion(){
		return version;
	}

}
