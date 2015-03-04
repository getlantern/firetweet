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

import java.util.Observable;
import java.util.Observer;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import ch.ethz.twimight.R;
import ch.ethz.twimight.data.HtmlPagesDbHelper;
import ch.ethz.twimight.net.Html.StartServiceHelper;
import ch.ethz.twimight.net.opportunistic.BluetoothStatus;
import ch.ethz.twimight.net.twitter.Tweets;
import ch.ethz.twimight.net.twitter.TwitterUsers;
import ch.ethz.twimight.util.Constants;
import ch.ethz.twimight.util.LogCollector;

/**
 * The base activity for all Twimight activities.
 * 
 * @author thossmann
 * 
 */
public abstract class TwimightBaseActivity extends FragmentActivity implements
		Observer {
	
	static TwimightBaseActivity instance;
	private static final String TAG = "TwimightBaseActivity";
	public static final boolean D = true;

	public static Context context;
	
	ActionBar actionBar;
	static Drawable dd, dn;

	private View bottomStatusBar;
	private TextView tvNeighborCount;
	private TextView tvStatus;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		LogCollector.setUpCrittercism(getApplicationContext());
		LogCollector.leaveBreadcrumb();

		// action bar
		actionBar = getActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle("@" + LoginActivity.getTwitterScreenname(this));	
		
		TwimightBaseActivity.context = this.getApplicationContext();

	}
	
    public static Context getAppContext() {
        return TwimightBaseActivity.context;
    }

	/**
	 * on Resume
	 */
	@Override
	public void onResume() {
		super.onResume();
		instance = this;
		
		if(dd == null || dn == null) {
			Log.i(TAG,"loading action bar backgrounds");
			Resources resources = getResources();
			dd = resources.getDrawable(R.drawable.top_bar_background_disaster);
			dn = resources.getDrawable(R.drawable.top_bar_background);
		}	
		
		// bottom status bar (can't do it in onCreate because layout is not set yet)
		bottomStatusBar = findViewById(R.id.bottomStatusBar);
		tvNeighborCount = (TextView) findViewById(R.id.tvNeighborCount);
		tvStatus = (TextView) findViewById(R.id.tvStatus);
		// setup disaster mode specific stuff
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				"prefDisasterMode", false) == true) {
			if (dd == null)
				Log.i(TAG,"dd null");			
			actionBar.setBackgroundDrawable(dd);
			Log.i(TAG,"setting disaster background");
			// does the current layout have a bottom status bar?
			if (bottomStatusBar != null) {
				bottomStatusBar.setVisibility(View.VISIBLE);
				updateStatusBar();
				// register for bluetooth status updates
				BluetoothStatus.getInstance().addObserver(this);
			}
		} else {
			actionBar.setBackgroundDrawable(dn);
			Log.i(TAG,"setting normal background");
			if (bottomStatusBar != null) {
				bottomStatusBar.setVisibility(View.GONE);
			}
		}
		// actionbar hack to make sure the background drawable is applied
		// http://stackoverflow.com/questions/11002691/actionbar-setbackgrounddrawable-nulling-background-from-thread-handler
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowTitleEnabled(true);
	}


	@Override
	protected void onPause() {
		// unregister from bluetooth status updates
		BluetoothStatus.getInstance().deleteObserver(this);
		super.onPause();
	}
	/*
	 * 
	 * @Override protected void onRestart() { // TODO Auto-generated method stub
	 * super.onRestart(); restartOnThemeSwitch(TwimightBaseActivity.this); }
	 * 
	 * 
	 * public static void restartOnThemeSwitch(Activity act) {
	 * 
	 * 
	 * 
	 * if (PreferenceManager.getDefaultSharedPreferences(act).getBoolean(
	 * "prefDisasterMode", false) == true) {
	 * 
	 * 
	 * Intent it = act.getIntent(); it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	 * 
	 * act.startActivity(it);
	 * 
	 * }
	 * 
	 * }
	 */
	/**
	 * Populate the Options menu with the "home" option. For the "main" activity
	 * ShowTweetListActivity we don't add the home option.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	/**
	 * Handle options menu selection
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent i;
		switch (item.getItemId()) {

		case R.id.menu_write_tweet:
			startActivity(new Intent(getBaseContext(), NewTweetActivity.class));
			break;

		case R.id.menu_search:
			onSearchRequested();
			break;

		case android.R.id.home:
			// app icon in action bar clicked; go home
			i = new Intent(this, ShowTweetListActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return true;

		case R.id.menu_my_profile:
			Uri uri = Uri.parse("content://"+TwitterUsers.TWITTERUSERS_AUTHORITY+"/"+TwitterUsers.TWITTERUSERS);
			Cursor c = getContentResolver().query(uri, null, TwitterUsers.COL_TWITTERUSER_ID+"="+LoginActivity.getTwitterId(this), null, null);
			if(c.getCount()!=1) return false;
			c.moveToFirst();
			int rowId = c.getInt(c.getColumnIndex("_id"));

			if (rowId > 0) {
				// show the local user
				i = new Intent(this, ShowUserActivity.class);
				i.putExtra("rowId", rowId);
				startActivity(i);
			}
			c.close();
			break;

		case R.id.menu_messages:
			// Launch User Messages
			i = new Intent(this, ShowDMUsersListActivity.class);
			startActivity(i);
			break;

		case R.id.menu_settings:
			// Launch PrefsActivity
			i = new Intent(this, PrefsActivity.class);
			startActivity(i);
			break;

		case R.id.menu_logout:
			// In disaster mode we don't allow logging out
			if (PreferenceManager.getDefaultSharedPreferences(
					getApplicationContext()).getBoolean("prefDisasterMode",
					Constants.DISASTER_DEFAULT_ON) == false) {
				showLogoutDialog();
			} else {
				Toast.makeText(this, R.string.disable_disastermode,
						Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.menu_about:
			// Launch AboutActivity
			i = new Intent(this, AboutActivity.class);
			startActivity(i);
			break;
		case R.id.menu_cache:
			new CacheTask().execute();
			break;

		case R.id.menu_cache_clear:

			AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);
			confirmDialog.setMessage(R.string.clear_cache_question);
			confirmDialog.setTitle(R.string.clear_cache_title);
			confirmDialog.setPositiveButton(R.string.yes,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							dialog.dismiss();
							new DeleteCacheTask().execute();

						}

					});
			confirmDialog.setNegativeButton(R.string.no,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							dialog.dismiss();
						}
					});
			confirmDialog.show();

			break;

		case R.id.menu_feedback:
			// Launch FeedbacktActivity
			i = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.TDS_BASE_URL
					+ "/bugs/new"));
			startActivity(i);
			break;
		default:
			return false;
		}
		return true;
	}

	private class CacheTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			// TODO Auto-generated method stub
			ContentResolver resolver = getContentResolver();
			Cursor cursor = resolver.query(
					Uri.parse("content://" + Tweets.TWEET_AUTHORITY + "/"
							+ Tweets.TWEETS + "/"
							+ Tweets.TWEETS_TABLE_TIMELINE + "/"
							+ Tweets.TWEETS_SINCE_LAST_UPDATE), null, null,
					null, null);
			HtmlPagesDbHelper htmlDbHelper = new HtmlPagesDbHelper(
					getApplicationContext());
			htmlDbHelper.open();
			htmlDbHelper.saveLinksFromCursor(cursor,
					HtmlPagesDbHelper.DOWNLOAD_FORCED);

			return null;
		}

		@Override
		protected void onPostExecute(Void params) {
			StartServiceHelper.startService(getApplicationContext());
			getContentResolver().notifyChange(Tweets.TABLE_TIMELINE_URI, null);
		}
	}

	private class DeleteCacheTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			Long timeSpan = (long) (0 * 24 * 3600 * 1000);
			HtmlPagesDbHelper htmlDbHelper = new HtmlPagesDbHelper(
					getApplicationContext());
			htmlDbHelper.open();
			htmlDbHelper.clearHtmlPages(timeSpan);
			return null;
		}

	}

	/**
	 * Asks the user if she really want to log out
	 */
	private void showLogoutDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.logout_question)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								LoginActivity.logout(TwimightBaseActivity.this
										.getApplicationContext());
								finish();
							}
						})
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Turns the loading icon on and off
	 * 
	 * @param isLoading
	 */
	public static void setLoading(final boolean isLoading) {

		if (instance != null) {
			try {

				instance.runOnUiThread(new Runnable() {
					public void run() {
						instance.setProgressBarIndeterminateVisibility(isLoading);
					}
				});

			} catch (Exception ex) {
				Log.e(TAG, "error: ", ex);
			}

		} else {
			Log.v(TAG, "Cannot show loading icon");
		}

	}

	/**
	 * Clean up the views
	 * 
	 * @param view
	 */
	public static void unbindDrawables(View view) {
		if (view != null) {
			if (view.getBackground() != null) {
				view.getBackground().setCallback(null);
			}
			if (view instanceof ViewGroup) {
				for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
					unbindDrawables(((ViewGroup) view).getChildAt(i));
				}
				try {
					((ViewGroup) view).removeAllViews();
				} catch (UnsupportedOperationException e) {
					// No problem, nothing to do here
				}
			}
		}

	}

	/**
	 * Receives notificatios from BluetoothState to update the bottom status
	 * bar.
	 */
	@Override
	public void update(Observable observable, Object data) {
		updateStatusBar();
	}

	/**
	 * Updates the bottom status bar. Runs on UI thread and can thus be called
	 * directly from anywhere.
	 */
	private void updateStatusBar() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// update number of neighbors
				tvNeighborCount.setText(String.valueOf(BluetoothStatus
						.getInstance().getNeighborCount()));
				// update state description
				tvStatus.setText(BluetoothStatus.getInstance()
						.getStatusDescription());
			}
		});
	}

}
