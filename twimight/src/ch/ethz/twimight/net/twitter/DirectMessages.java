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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Direct Message definitions: Column names for the DB and MIME types, columns and URIs for the content provider
 * @author thossmann
 *
 */
public class DirectMessages implements BaseColumns {

	// This class cannot be instantiated
	private DirectMessages(){ }

	public static final String DM_AUTHORITY = "ch.ethz.twimight.DMs"; /** authority part of the URI */
	public static final String DMS = "dms"; /** the direct messages part of the URI */
	public static final Uri DMS_URI = Uri.parse("content://" + DM_AUTHORITY + "/" + DMS); /** URI to reference all direct messages */
	public static final Uri CONTENT_URI = DMS_URI; /** the content URI */	
	
	// MIME type definitions
	public static final String DMS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.twimight.dm"; /** the MIME type of a set of direct messages */
	public static final String DM_CONTENT_TYPE = "vnd.android.cursor.item/vnd.twimight.dm"; /** the MIME type of a single direct message */
	public static final String DMUSERS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.twimight.dmuser"; /** the MIME type of a set of users with directmessages */
	
	// URI name definitions
	public static final String DMS_SOURCE_NORMAL = "normal"; /** only normal direct messages (no disaster messages) */
	public static final String DMS_SOURCE_DISASTER = "disaster"; /** only disaster direct messages */
	public static final String DMS_SOURCE_ALL = "all"; /** both, normal and disaster messages */
	
	public static final String DMS_LIST = "list"; /** all messages */
	public static final String DMS_USERS = "users"; /** all users we have conversations with */
	public static final String DMS_USER = "user"; /** all direct message from and to a user */
	
	// here start the column names
	public static final String COL_TEXT = "text"; /** the dm text */
	public static final String COL_SENDER = "user_id"; /** the user id of the sender */
	public static final String COL_RECEIVER = "receiver"; /** the user id of the receiver */
	public static final String COL_RECEIVER_SCREENNAME = "receiver_screenname"; /** the screenname of the receiver (required for sending messages) */
	public static final String COL_DMID = "dm_id"; /** the "official" message ID from twitter */
	public static final String COL_CREATED = "created"; /** the creation timestamp (millisecs since 1970) */
	public static final String COL_RECEIVED = "received"; /** timestamp we insert the tweet into the DB */
	public static final String COL_BUFFER = "buffer_flags"; /** which buffer(s) is the message in */
	public static final String COL_FLAGS = "flags"; /** Transactional flags */
	
	// for disaster mode
	public static final String COL_ISDISASTER = "is_disaster_dm"; /** disaster or normal message? */

	public static final String COL_DISASTERID = "d_id"; /** the disaster ID of the message (for both, disaster and normal message) */
	public static final String COL_ISVERIFIED = "is_verified"; /** is the signature of the disaster message valid? */
	public static final String COL_SIGNATURE = "signature"; /** the signature of the disaster message */
	public static final String COL_CERTIFICATE = "certificate"; /** the certificate of the user */
	public static final String COL_CRYPTEXT = "cryptext"; /** the encrypted message */
	
	public static final String DEFAULT_SORT_ORDER = COL_CREATED + " DESC";
	
	// flags for synchronizing with twitter
	public static final int FLAG_TO_INSERT = 1; /** The message is new and should be posted to twitter */
	public static final int FLAG_TO_DELETE = 2; /** Delete the message from Twitter */

	
	// flags to mark which buffer(s) a tweet belongs to. (Buffer sizes are defined in class Constants)
	public static final int BUFFER_MESSAGES = 1; /** Holds all messages of the local user (to and from) */
	public static final int BUFFER_DISASTER_ME = 2; /** Disaster messages written by other users for me */
	public static final int BUFFER_MYDISASTER = 4; /** The disaster messages of the local user */
	public static final int BUFFER_DISASTER_OTHERS = 8; /** Disaster messages for other users */


}
