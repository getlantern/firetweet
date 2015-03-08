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
package ch.ethz.twimight.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This is where all global constants for configuration go!
 * @author thossmann
 *
 */
public final class Constants { 
    
	
	/**
	 * Do not instantiate!
	 */
	private Constants() { throw new AssertionError("Constants is uninstantiable"); }

	// TDS configuration
	public static final int TDS_MESSAGE_VERSION = 1; /** Which message version */
    public static final long TDS_UPDATE_INTERVAL = 6*60*60*1000L; /** Interval (millisec) for updating MAC list from the TDS */
    public static final long TDS_UPDATE_RETRY_INTERVAL = 30*1000L; /** Initial interval for re-trying to connect to TDS */
    public static final boolean TDS_DEFAULT_ON = true; /** Opt in or opt out for TDS communication? */
    public static final long WAIT_FOR_CONNECTIVITY = 5*1000L; /** After waking up we have to wait to get connectivity */
    public static final String TDS_BASE_URL = "https://twimightserver-ethz.rhcloud.com/"; /** The URL of the TDS */ 
//    public static final String TDS_BASE_URL = "http://nb-10483.ethz.ch:3000/"; /** TDS URL for local server debugging */
    public static final int HTTP_CONNECTION_TIMEOUT = 3*1000; /** How long do we wait for a connection? */
    public static final int HTTP_SOCKET_TIMEOUT = 20*1000; /** How long do we wait for data? HINT: We have to wait long, since this includes authentication in the Twitter server*/
    
    // Bluetooth scanning configuration
    public static final long SCANNING_INTERVAL = 2*60*1000L; /** Interval for Bluetooth scans */
    public static final long MIN_LISTEN_TIME = 10*1000L; /** Interval for Bluetooth scans */
    public static final long RANDOMIZATION_INTERVAL = 10*1000L; /** Randomization interval for scanning */
	public static final boolean DISASTER_DEFAULT_ON = false; /** are we in disaster mode by default? */
	public static final boolean OFFLINE_DEFAULT_ON = false;
	public static final long WAIT_FOR_BLUETOOTH = 20*1000L; /** If Bluetooth is off, how long to we wait for it to enable? */
	public static final long MEETINGS_INTERVAL = 60*1000L; /** time interval between to successive encounters */

	//Message types from the BluetoothService Handler	  
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_CONNECTION_SUCCEEDED = 4;	
	public static final int MESSAGE_CONNECTION_FAILED =6;
	public static final int MESSAGE_CONNECTION_LOST =8;
	public static final int BLUETOOTH_RESTART =10;


	// Key names from the BluetoothService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String DEVICE_ADDRESS = "device address";
	public static final String TOAST = "toast";
	public static final String DEVICE = "device";
	public static final String PEERS_MET = "peers_met";
	
	// Location
	public static final long LOCATION_UPDATE_TIME = 6*60*60*1000L; /** Location update interval */
	public static final long LOCATION_WAIT = 60*1000L; /** How long should we wait for a location. This MUST NOT be larger than LOCATION_UPDATE_TIME */
	public static final int LOCATION_ACCURACY = 150; /** What is a satisfying accuracy? */
	public static final boolean LOCATION_DEFAULT_ON = true; /** Do we send location updates by default? */
	public static Map<String, Integer> locationProvidersMap = new HashMap<String, Integer>();
	static {
        locationProvidersMap.put("gps", 1);
        locationProvidersMap.put("network", 2);
        locationProvidersMap = Collections.unmodifiableMap(locationProvidersMap);
    }	
	
	
	// Security
	public static final long DISASTER_DURATION = 7*24*60*60*1000L; /** How long do we need the certificate to be valid during loss of connectivity? */
	public static final int SECURITY_KEY_SIZE = 2048; /** RSA Key length */
	
	// Twitter
	public static final int CONSUMER_ID = 1;
	public static final int LOGIN_ATTEMPTS = 2; /** How many times do we attempt to log in before giving up? */
	public static final int TIMELINE_ATTEMPTS = 2; /** How many times do we attempt to update the timeline before giving up? */
	public static final int TWEET_LENGTH = 140; /** The max length of a tweet */
	public static final boolean TWEET_DEFAULT_LOCATION = true; /** Are tweets by default geo-tagged or not? */
	public static final boolean TWEET_DEFAULT_RUN_AT_BOOT = true; /** Are the updates started at boot time ? */
	public static long UPDATER_UPDATE_PERIOD = 5 * 60 * 1000L; /** frequency of background updates */
	
	public static final int NR_TWEETS = 50; /** how many tweets to request from twitter in timeline update */
	public static final int NR_FAVORITES = 20; /** how many favorites to request from twitter */
	public static final int NR_MENTIONS = 20; /** how many mentions to request from twitter */
	public static final int NR_DMS = 20; /** how many direct messages to request from twitter */
	public static final int NR_SEARCH_TWEETS = 20; /** how many tweets to request from twitter in search */
	
	public static final long TIMELINE_MIN_SYNCH = 120*1000L; /** Minimum time between two timeline updates */
	public static final long FAVORITES_MIN_SYNCH = 120*1000L; /** Minimum time between two favorite updates */
	public static final long MENTIONS_MIN_SYNCH = 120*1000L; /** Minimum time between two mentions updates */
	public static final long FRIENDS_MIN_SYNCH = 120*60*1000L; /** Minimum time between two updates of the friends list */
	public static final long FOLLOWERS_MIN_SYNCH = 120*60*1000L; /** Minimum time between two updates of the list of followers */
	public static final long USERS_MIN_SYNCH = 24*3600*1000L; /** Minmum time between two updates of a user profile */
	public static final long DMS_MIN_SYNCH = 20*1000L; /** Minimum time between two updates of the direct messages */
	
	public static int TIMELINE_BUFFER_SIZE = 100; /** How many "normal" tweets (not favorites, mentions, etc) to store locally */
	public static final int FAVORITES_BUFFER_SIZE = 20; /** How many favorites to store locally */
	public static final int MENTIONS_BUFFER_SIZE = 20; /** How many mentions to store locally */
	public static final int DTWEET_BUFFER_SIZE = 100; /** How many disaster tweets of other users */
	public static final int MYDTWEET_BUFFER_SIZE = 50; /** How many of our own disaster tweets */
	public static final int MESSAGES_BUFFER_SIZE = 100; /** How many direct messages to and from the local user do we buffer? */
	public static final int DISASTERDM_BUFFER_SIZE = 100; /** How many disaster direct messages of remote users do we carry? */
	public static final int MYDISASTERDM_BUFFER_SIZE = 100; /** How many disaster messages sent by the local user do we carry? */
	public static final int USERTWEETS_BUFFER_SIZE = 100; /** How many tweets to cache for showing user profiles */
	public static final int SEARCHTWEETS_BUFFER_SIZE = 100; /** How many tweets to cache from searching Twitter */
	public static final int SEARCHUSERS_BUFFER_SIZE = 100; /** How many users to cache from searching Twitter */
	
	
	//Other
	public static final long FRIENDS_FOLLOWERS_DELAY = 60*1000L; /** delay after which friends and followers are downloaded */
	public static final String DIS_MODE_USED = "dis_mode_used";
		
	
}
