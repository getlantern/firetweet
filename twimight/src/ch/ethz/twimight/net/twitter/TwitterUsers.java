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
 * Twitter Users columns
 * @author thossmann
 *
 */
public class TwitterUsers implements BaseColumns {

	// This class cannot be instantiated
	private TwitterUsers(){ }

	/**
	 * The authority part of the URI
	 */
	public static final String TWITTERUSERS_AUTHORITY = "ch.ethz.twimight.TwitterUsers";

	/**
	 * The twitter users
	 */
	public static final String TWITTERUSERS = "users";
	
	
	// MIME type definitions
	/**
	 * The MIME type for a set of twitter users
	 */
	public static final String TWITTERUSERS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.twimight.twitteruser";
	
	/**
	 * The MIME type of a single twitter user
	 */
	public static final String TWITTERUSER_CONTENT_TYPE = "vnd.android.cursor.item/vnd.twimight.twitteruser";
	
	// URI name definitions

	/**
	 * Selecting only friends
	 */
	public static final String TWITTERUSERS_FRIENDS = "friends";
	/**
	 * Selecting only followers
	 */
	public static final String TWITTERUSERS_FOLLOWERS = "followers";
	/**
	 * Selecting only disaster peers
	 */
	public static final String TWITTERUSERS_DISASTER = "disaster_peers";
	/**
	 * Selecting users for the searching operation
	 */
	public static final String TWITTERUSERS_SEARCH = "search_users";
	/**
	 * Only the screenname of a user
	 */
	public static final String TWITTERUSERS_SCREENNAME = null;
	
	/**
	 * The name of the twitter users id
	 */
	public static final String TWITTERUSERS_ID = "id";
	/**
	 * The name of the twitter users id
	 */
	public static final String TWITTERUSERS_PICTURE = "pictures";
	
	// here start the column names
	public static final String COL_SCREENNAME = "user_screenname";
	public static final String COL_TWITTERUSER_ID = "twitteruser_id";
	public static final String COL_NAME = "name";
	public static final String COL_LANG = "lang";
	public static final String COL_DESCRIPTION = "description";
	public static final String COL_IMAGEURL = "profile_image_url";
	public static final String COL_STATUSES = "statuses_count";
	public static final String COL_FOLLOWERS = "followers_count";
	public static final String COL_FRIENDS = "friends_count";
	public static final String COL_LISTED = "listed_count";
	public static final String COL_FAVORITES = "favorites_count";
	public static final String COL_LOCATION = "location";
	public static final String COL_UTCOFFSET = "utc_offset";
	public static final String COL_TIMEZONE = "timezone";
	public static final String COL_URL = "url";
	public static final String COL_CREATED = "u_created_at";
	public static final String COL_PROTECTED = "protected";
	public static final String COL_VERIFIED = "verified";
	
	public static final String COL_ISFOLLOWER = "following"; /** is the user following us? */
	public static final String COL_ISFRIEND = "follow"; /** are we following the user? */
	public static final String COL_ISDISASTER_PEER = "disaster_peer"; /** have we met the user in disaster mode */
	public static final String COL_IS_SEARCH_RESULT = "search_result"; /** have we met the user in disaster mode */
	public static final String COL_FOLLOWREQUEST = "follow_request_sent"; /** was a following request sent to twitter? */	
	public static final String COL_LASTUPDATE = "last_update";
	public static final String COL_LAST_PICTURE_UPDATE = "last_picture_update";
	public static final String COL_FLAGS = "u_flags";

	public static final String COL_PROFILEIMAGE_PATH = "_data";
	public static final String COL_PROFILEIMAGE = "image";  /** this is used only in disaster mode to sent the profile image */

	
	public static final String DEFAULT_SORT_ORDER = COL_SCREENNAME;
	
	// flags for synchronizing with twitter
	public static final int FLAG_TO_UPDATE = 1;
	public static final int FLAG_TO_FOLLOW = 2;
	public static final int FLAG_TO_UNFOLLOW = 4;
	public static final int FLAG_TO_UPDATEIMAGE = 8;
	
	
	/**
	 *  URI to reference all twitter users
	 */
	private static final String BASE_URI = "content://" + TWITTERUSERS_AUTHORITY + "/" + TWITTERUSERS + "/";
	public static final Uri TWITTERUSERS_URI = Uri.parse(BASE_URI);
	
	/**
	 * The content:// style URL
	 */
	public static final Uri CONTENT_URI = TWITTERUSERS_URI;
	public static final Uri USERS_FRIENDS_URI = Uri.parse(BASE_URI + COL_ISFRIEND);
	public static final Uri USERS_FOLLOWERS_URI = Uri.parse(BASE_URI +  COL_ISFOLLOWER );
	public static final Uri USERS_SEARCH_URI = Uri.parse(BASE_URI + TWITTERUSERS_SEARCH );
	public static final Uri USERS_DISASTER_URI = Uri.parse(BASE_URI + COL_ISDISASTER_PEER);


	
}
