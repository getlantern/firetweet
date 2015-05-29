/*
 * 				Firetweet - Twitter client for Android
 * 
 *  Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.getlantern.firetweet.activity.support;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.getlantern.firetweet.Constants;
import org.getlantern.firetweet.R;
import org.getlantern.firetweet.activity.SettingsActivity;
import org.getlantern.firetweet.app.FiretweetApplication;
import org.getlantern.firetweet.fragment.support.BaseSupportDialogFragment;
import org.getlantern.firetweet.fragment.support.SupportProgressDialogFragment;
import org.getlantern.firetweet.provider.FiretweetDataStore.Accounts;
import org.getlantern.firetweet.util.AsyncTaskUtils;
import org.getlantern.firetweet.util.AsyncTwitterWrapper;
import org.getlantern.firetweet.util.ContentValuesCreator;
import org.getlantern.firetweet.util.OAuthPasswordAuthenticator;
import org.getlantern.firetweet.util.OAuthPasswordAuthenticator.AuthenticationException;
import org.getlantern.firetweet.util.OAuthPasswordAuthenticator.AuthenticityTokenException;
import org.getlantern.firetweet.util.OAuthPasswordAuthenticator.WrongUserPassException;
import org.getlantern.firetweet.util.ParseUtils;
import org.getlantern.firetweet.util.ThemeUtils;
import org.getlantern.firetweet.util.TwitterContentUtils;
import org.getlantern.firetweet.util.Utils;
import org.getlantern.firetweet.util.accessor.ViewAccessor;
import org.getlantern.firetweet.util.net.OkHttpClientFactory;
import org.getlantern.firetweet.util.net.FiretweetHostResolverFactory;

import java.util.Locale;

import twitter4j.Twitter;
import twitter4j.TwitterConstants;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.BasicAuthorization;
import twitter4j.auth.RequestToken;
import twitter4j.auth.TwipOModeAuthorization;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import com.crashlytics.android.Crashlytics;
import 	android.graphics.Typeface;

import static android.text.TextUtils.isEmpty;
import static org.getlantern.firetweet.util.ContentValuesCreator.createAccount;
import static org.getlantern.firetweet.util.Utils.getAccountIds;
import static org.getlantern.firetweet.util.Utils.getActivatedAccountIds;
import static org.getlantern.firetweet.util.Utils.getNonEmptyString;
import static org.getlantern.firetweet.util.Utils.isUserLoggedIn;
import static org.getlantern.firetweet.util.Utils.showErrorMessage;
import static org.getlantern.firetweet.util.Utils.trim;

public class SignInActivity extends BaseActionBarActivity implements TwitterConstants, OnClickListener,
        TextWatcher {

    private static final String EXTRA_API_LAST_CHANGE = "api_last_change";
    public static final String FRAGMENT_TAG_SIGN_IN_PROGRESS = "sign_in_progress";
    private static final String DEFAULT_TWITTER_API_URL_FORMAT = "https://[DOMAIN.]twitter.com/";

    private String mAPIUrlFormat;
    private int mAuthType;
    private String mConsumerKey, mConsumerSecret;
    private String mUsername, mPassword;
    private boolean mBackPressed;
    private long mAPIChangeTimestamp;

    private EditText mEditUsername, mEditPassword;
    private Button mSignInButton, mSignUpButton, poweredByButton;
    private LinearLayout mSigninSignupContainer;

    private FiretweetApplication mApplication;
    private SharedPreferences mPreferences;
    private ContentResolver mResolver;
    private AbstractSignInTask mTask;
    private boolean mSameOAuthSigningUrl, mNoVersionSuffix;

    @Override
    public void afterTextChanged(final Editable s) {

    }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_EDIT_API: {
                if (resultCode == RESULT_OK) {
                    mAPIUrlFormat = data.getStringExtra(Accounts.API_URL_FORMAT);
                    mAuthType = data.getIntExtra(Accounts.AUTH_TYPE, Accounts.AUTH_TYPE_OAUTH);
                    mSameOAuthSigningUrl = data.getBooleanExtra(Accounts.SAME_OAUTH_SIGNING_URL, false);
                    mNoVersionSuffix = data.getBooleanExtra(Accounts.NO_VERSION_SUFFIX, false);
                    mConsumerKey = data.getStringExtra(Accounts.CONSUMER_KEY);
                    mConsumerSecret = data.getStringExtra(Accounts.CONSUMER_SECRET);
                    final boolean isTwipOMode = mAuthType == Accounts.AUTH_TYPE_TWIP_O_MODE;
                    mSigninSignupContainer
                            .setOrientation(isTwipOMode ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
                }
                setSignInButton();
                invalidateOptionsMenu();
                break;
            }
            case REQUEST_BROWSER_SIGN_IN: {
                if (resultCode == BaseActionBarActivity.RESULT_OK && data != null) {
                    doLogin(data);
                }
                break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING && !mBackPressed) {
            final Toast toast = Toast.makeText(this, R.string.signing_in_please_wait, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
            mTask.cancel(false);
        }
        super.onBackPressed();
    }

    @Override
    public void onClick(final View v) {

        int vId = v.getId();
        if (vId == R.id.sign_up || vId == R.id.sign_in) {
            if (vId == R.id.sign_up) {
                BrowserSignInActivity.action = "sign_up";
            } else {
                BrowserSignInActivity.action = "sign_in";
            }
            final Context context = this;

            final Intent intent = new Intent(this, BrowserSignInActivity.class);
            intent.putExtra(Accounts.CONSUMER_KEY, mConsumerKey);
            intent.putExtra(Accounts.CONSUMER_SECRET, mConsumerSecret);
            startActivityForResult(intent, REQUEST_BROWSER_SIGN_IN);
        }
    }




    @Override
    public void onSupportContentChanged() {
        super.onSupportContentChanged();
        mSignInButton = (Button) findViewById(R.id.sign_in);
        mSignUpButton = (Button) findViewById(R.id.sign_up);
        poweredByButton = (Button) findViewById(R.id.powered_by);
        mSigninSignupContainer = (LinearLayout) findViewById(R.id.sign_in_sign_up);
    }

    @Override
    public void onDestroy() {
        getLoaderManager().destroyLoader(0);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        //saveEditedText();
        setDefaultAPI();
        outState.putString(Accounts.API_URL_FORMAT, mAPIUrlFormat);
        outState.putInt(Accounts.AUTH_TYPE, mAuthType);
        outState.putBoolean(Accounts.SAME_OAUTH_SIGNING_URL, mSameOAuthSigningUrl);
        outState.putBoolean(Accounts.NO_VERSION_SUFFIX, mNoVersionSuffix);
        outState.putString(Accounts.CONSUMER_KEY, mConsumerKey);
        outState.putString(Accounts.CONSUMER_SECRET, mConsumerSecret);
        outState.putString(Accounts.SCREEN_NAME, mUsername);
        outState.putString(Accounts.PASSWORD, mPassword);
        outState.putLong(EXTRA_API_LAST_CHANGE, mAPIChangeTimestamp);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
        setSignInButton();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        mResolver = getContentResolver();
        mApplication = FiretweetApplication.getInstance(this);
        setContentView(R.layout.activity_sign_in);
        setSupportProgressBarIndeterminateVisibility(false);
        final long[] account_ids = getActivatedAccountIds(this);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(account_ids.length > 0);
        }

        if (savedInstanceState != null) {
            mAPIUrlFormat = savedInstanceState.getString(Accounts.API_URL_FORMAT);
            mAuthType = savedInstanceState.getInt(Accounts.AUTH_TYPE);
            mSameOAuthSigningUrl = savedInstanceState.getBoolean(Accounts.SAME_OAUTH_SIGNING_URL);
            mConsumerKey = trim(savedInstanceState.getString(Accounts.CONSUMER_KEY));
            mConsumerSecret = trim(savedInstanceState.getString(Accounts.CONSUMER_SECRET));
            mUsername = savedInstanceState.getString(Accounts.SCREEN_NAME);
            mPassword = savedInstanceState.getString(Accounts.PASSWORD);
            mAPIChangeTimestamp = savedInstanceState.getLong(EXTRA_API_LAST_CHANGE);
        }

        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/ProximaNova-Semibold.ttf");

        mSigninSignupContainer.setOrientation(mAuthType == Accounts.AUTH_TYPE_TWIP_O_MODE ? LinearLayout.VERTICAL
                : LinearLayout.HORIZONTAL);

        final Resources resources = getResources();
        final ColorStateList color = ColorStateList.valueOf(resources.getColor(R.color.material_light_blue));

        mSignInButton.setTextColor(Color.parseColor("#38c6f3"));
        mSignInButton.setBackgroundColor(Color.parseColor("#E7E7E7"));
        mSignInButton.setBackgroundResource(R.drawable.sign_in_btn);
        mSignInButton.setTypeface(font);
        mSignUpButton.setTypeface(font);
        poweredByButton.setTypeface(font);

        setSignInButton();
    }

    private void doLogin() {
        if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
            mTask.cancel(true);
        }
        setDefaultAPI();
        final Configuration conf = getConfiguration();
        mTask = new SignInTask(this, conf, mUsername, mPassword, mAuthType, mAPIUrlFormat,
                mSameOAuthSigningUrl, mNoVersionSuffix);
        AsyncTaskUtils.executeTask(mTask);
    }

    private void doLogin(final Intent intent) {
        if (intent == null) return;
        if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
            mTask.cancel(true);
        }
        setDefaultAPI();
        final Configuration conf = getConfiguration();
        final String token = intent.getStringExtra(EXTRA_REQUEST_TOKEN);
        final String secret = intent.getStringExtra(EXTRA_REQUEST_TOKEN_SECRET);
        final String verifier = intent.getStringExtra(EXTRA_OAUTH_VERIFIER);
        mTask = new BrowserSignInTask(this, conf, token, secret, verifier, mAPIUrlFormat,
                mSameOAuthSigningUrl, mNoVersionSuffix);
        AsyncTaskUtils.executeTask(mTask);
    }

    private Configuration getConfiguration() {
        final ConfigurationBuilder cb = new ConfigurationBuilder();
        final boolean enable_gzip_compressing = mPreferences.getBoolean(KEY_GZIP_COMPRESSING, false);
        final boolean ignore_ssl_error = mPreferences.getBoolean(KEY_IGNORE_SSL_ERROR, false);
        final boolean enable_proxy = mPreferences.getBoolean(KEY_ENABLE_PROXY, false);
        cb.setHostAddressResolverFactory(new FiretweetHostResolverFactory(mApplication));
        cb.setHttpClientFactory(new OkHttpClientFactory(mApplication));
        if (TwitterContentUtils.isOfficialKey(this, mConsumerKey, mConsumerSecret)) {
            Utils.setMockOfficialUserAgent(this, cb);
        } else {
            Utils.setUserAgent(this, cb);
        }
        final String apiUrlFormat = TextUtils.isEmpty(mAPIUrlFormat) ? DEFAULT_TWITTER_API_URL_FORMAT : mAPIUrlFormat;
        final String versionSuffix = mNoVersionSuffix ? null : "/1.1/";
        cb.setRestBaseURL(Utils.getApiUrl(apiUrlFormat, "api", versionSuffix));
        cb.setOAuthBaseURL(Utils.getApiUrl(apiUrlFormat, "api", "/oauth/"));
        cb.setUploadBaseURL(Utils.getApiUrl(apiUrlFormat, "upload", versionSuffix));
        cb.setOAuthAuthorizationURL(Utils.getApiUrl(apiUrlFormat, null, "/oauth/authorize"));
        cb.setHttpUserAgent(Utils.generateBrowserUserAgent());
        if (!mSameOAuthSigningUrl) {
            cb.setSigningRestBaseURL(DEFAULT_SIGNING_REST_BASE_URL);
            cb.setSigningOAuthBaseURL(DEFAULT_SIGNING_OAUTH_BASE_URL);
            cb.setSigningUploadBaseURL(DEFAULT_SIGNING_UPLOAD_BASE_URL);
        }
        if (isEmpty(mConsumerKey) || isEmpty(mConsumerSecret)) {
            cb.setOAuthConsumerKey(TWITTER_CONSUMER_KEY_3);
            cb.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET_3);
        } else {
            cb.setOAuthConsumerKey(mConsumerKey);
            cb.setOAuthConsumerSecret(mConsumerSecret);
        }
        cb.setGZIPEnabled(enable_gzip_compressing);
        cb.setIgnoreSSLError(ignore_ssl_error);
        if (enable_proxy) {
            final String proxy_host = mPreferences.getString(KEY_PROXY_HOST, null);
            final int proxy_port = ParseUtils.parseInt(mPreferences.getString(KEY_PROXY_PORT, "-1"));
            if (!isEmpty(proxy_host) && proxy_port > 0) {
                cb.setHttpProxyHost(proxy_host);
                cb.setHttpProxyPort(proxy_port);
            }
        }
        return cb.build();
    }

    private void saveEditedText() {
        mUsername = ParseUtils.parseString(mEditUsername.getText());
        mPassword = ParseUtils.parseString(mEditPassword.getText());
    }

    private void setDefaultAPI() {
        final long apiLastChange = mPreferences.getLong(KEY_API_LAST_CHANGE, mAPIChangeTimestamp);
        final boolean defaultApiChanged = apiLastChange != mAPIChangeTimestamp;
        final String apiUrlFormat = getNonEmptyString(mPreferences, KEY_API_URL_FORMAT, null);
        final int authType = mPreferences.getInt(KEY_AUTH_TYPE, Accounts.AUTH_TYPE_OAUTH);
        final boolean sameOAuthSigningUrl = mPreferences.getBoolean(KEY_SAME_OAUTH_SIGNING_URL, false);
        final boolean noVersionSuffix = mPreferences.getBoolean(KEY_NO_VERSION_SUFFIX, false);
        final String consumerKey = getNonEmptyString(mPreferences, KEY_CONSUMER_KEY, TWITTER_CONSUMER_KEY_3);
        final String consumerSecret = getNonEmptyString(mPreferences, KEY_CONSUMER_SECRET, TWITTER_CONSUMER_SECRET_3);
        if (isEmpty(mAPIUrlFormat) || defaultApiChanged) {
            mAPIUrlFormat = apiUrlFormat;
        }
        if (defaultApiChanged) {
            mAuthType = authType;
        }
        if (defaultApiChanged) {
            mSameOAuthSigningUrl = sameOAuthSigningUrl;
        }
        if (defaultApiChanged) {
            mNoVersionSuffix = noVersionSuffix;
        }
        if (isEmpty(mConsumerKey) || defaultApiChanged) {
            mConsumerKey = consumerKey;
        }
        if (isEmpty(mConsumerSecret) || defaultApiChanged) {
            mConsumerSecret = consumerSecret;
        }
        if (defaultApiChanged) {
            mAPIChangeTimestamp = apiLastChange;
        }
    }

    private void setSignInButton() {
        mSignInButton.setEnabled(true);
    }

    private void friendDefaultAccounts(final long accountId) {

        if (!mPreferences.contains("firstRun")) {

            Log.d(LOGTAG, "Friending default accounts and sending initial tweet");

            final Context context = this;
            final long[] accountIds = new long[1];
            accountIds[0] = accountId;

            final AsyncTwitterWrapper twitter = getTwitterWrapper();
            twitter.createFriendshipAsync(accountId, Constants.LANTERN_ACCOUNT_ID);
            twitter.createFriendshipAsync(accountId, Constants.FIRETWEET_ACCOUNT_ID);

            String initialTweetText = this.getString(R.string.initial_tweet);

            twitter.updateStatusAsync(accountIds, initialTweetText, null, null, -1,
                    false);

            mPreferences.edit().putBoolean("firstRun", true).apply();
        }
    }

    void onSignInResult(final SignInResponse result) {
        final FragmentManager fm = getSupportFragmentManager();
        final Fragment f = fm.findFragmentByTag(FRAGMENT_TAG_SIGN_IN_PROGRESS);
        if (f instanceof DialogFragment) {
            ((DialogFragment) f).dismissAllowingStateLoss();
        }
        if (result != null) {
            if (result.succeed) {
                final ContentValues values;
                switch (result.auth_type) {
                    case Accounts.AUTH_TYPE_BASIC: {
                        values = createAccount(result.conf, result.basic_username,
                                result.basic_password, result.user, result.color,
                                result.api_url_format, result.no_version_suffix);
                        break;
                    }
                    case Accounts.AUTH_TYPE_TWIP_O_MODE: {
                        values = ContentValuesCreator.createAccount(result.conf, result.user, result.color,
                                result.api_url_format, result.no_version_suffix);
                        break;
                    }
                    case Accounts.AUTH_TYPE_OAUTH:
                    case Accounts.AUTH_TYPE_XAUTH: {
                        values = ContentValuesCreator.createAccount(result.conf, result.access_token,
                                result.user, result.auth_type, result.color, result.api_url_format,
                                result.same_oauth_signing_url, result.no_version_suffix);
                        break;
                    }
                    default: {
                        values = null;
                    }
                }
                if (values != null) {
                    mResolver.insert(Accounts.CONTENT_URI, values);
                }
                final long loggedId = result.user.getId();
                final Intent intent = new Intent(this, HomeActivity.class);

                intent.putExtra(EXTRA_REFRESH_IDS, new long[]{loggedId});
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

                friendDefaultAccounts(loggedId);

                startActivity(intent);
                finish();
            } else if (result.already_logged_in) {
                Toast.makeText(this, R.string.error_already_logged_in, Toast.LENGTH_SHORT).show();
            } else {
                if (result.exception instanceof AuthenticityTokenException) {
                    Toast.makeText(this, R.string.wrong_api_key, Toast.LENGTH_SHORT).show();
                } else if (result.exception instanceof WrongUserPassException) {
                    Toast.makeText(this, R.string.wrong_username_password, Toast.LENGTH_SHORT).show();
                } else if (result.exception instanceof AuthenticationException) {
                    showErrorMessage(this, getString(R.string.action_signing_in), result.exception.getCause(), true);
                } else {
                    showErrorMessage(this, getString(R.string.action_signing_in), result.exception, true);
                }
            }
        }
        setSignInButton();
    }

    private final Handler mHandler = new Handler();

    void onSignInStart() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) return;
                final FragmentManager fm = getSupportFragmentManager();
                final FragmentTransaction ft = fm.beginTransaction();
                final SupportProgressDialogFragment fragment = new SupportProgressDialogFragment();
                fragment.setCancelable(false);
                fragment.show(ft, FRAGMENT_TAG_SIGN_IN_PROGRESS);
            }
        });
    }

    public static abstract class AbstractSignInTask extends AsyncTask<Object, Object, SignInResponse> {

        protected final Configuration conf;
        protected final SignInActivity callback;

        public AbstractSignInTask(final SignInActivity callback, final Configuration conf) {
            this.conf = conf;
            this.callback = callback;
        }

        @Override
        protected void onPostExecute(final SignInResponse result) {
            if (callback != null) {
                callback.onSignInResult(result);
            }
        }

        @Override
        protected void onPreExecute() {
            if (callback != null) {
                callback.onSignInStart();
            }
        }

        int analyseUserProfileColor(final User user) throws TwitterException {
            if (user == null) throw new TwitterException("Unable to get user info");
            return ParseUtils.parseColor("#" + user.getProfileLinkColor(), Color.TRANSPARENT);
        }

    }

    public static class BrowserSignInTask extends AbstractSignInTask {

        private final Configuration conf;
        private final String request_token, request_token_secret, oauth_verifier;

        private final Context context;
        private final String api_url_format;
        private final boolean same_oauth_signing_url, no_version_suffix;

        public BrowserSignInTask(final SignInActivity context, final Configuration conf,
                                 final String request_token, final String request_token_secret,
                                 final String oauth_verifier, final String api_url_format,
                                 final boolean same_oauth_signing_url, final boolean no_version_suffix) {
            super(context, conf);
            this.context = context;
            this.conf = conf;
            this.request_token = request_token;
            this.request_token_secret = request_token_secret;
            this.oauth_verifier = oauth_verifier;
            this.api_url_format = api_url_format;
            this.same_oauth_signing_url = same_oauth_signing_url;
            this.no_version_suffix = no_version_suffix;
        }

        @Override
        protected SignInResponse doInBackground(final Object... params) {
            try {
                final Twitter twitter = new TwitterFactory(conf).getInstance();
                final AccessToken access_token = twitter.getOAuthAccessToken(new RequestToken(conf, request_token,
                        request_token_secret), oauth_verifier);
                final long userId = access_token.getUserId();
                if (userId <= 0) return new SignInResponse(false, false, null);
                final User user = twitter.verifyCredentials();
                final int color = analyseUserProfileColor(user);
                return new SignInResponse(conf, access_token, user, Accounts.AUTH_TYPE_OAUTH, color,
                        api_url_format, same_oauth_signing_url, no_version_suffix);
            } catch (final TwitterException e) {
                Crashlytics.logException(e);
                return new SignInResponse(false, false, e);
            }
        }
    }

    public static class SignInMethodIntroductionDialogFragment extends BaseSupportDialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final Context wrapped = ThemeUtils.getDialogThemedContext(getActivity());
            final AlertDialog.Builder builder = new AlertDialog.Builder(wrapped);
            builder.setTitle(R.string.sign_in_method_introduction_title);
            builder.setMessage(R.string.sign_in_method_introduction);
            builder.setPositiveButton(android.R.string.ok, null);
            return builder.create();
        }

    }

    public static class SignInTask extends AbstractSignInTask {

        private final Configuration conf;
        private final String username, password;
        private final int auth_type;

        private final Context context;
        private final String api_url_format;
        private final boolean same_oauth_signing_url, no_version_suffix;

        public SignInTask(final SignInActivity context, final Configuration conf,
                          final String username, final String password, final int auth_type,
                          final String api_url_format, final boolean same_oauth_signing_url,
                          final boolean no_version_suffix) {
            super(context, conf);
            this.context = context;
            this.conf = conf;
            this.username = username;
            this.password = password;
            this.auth_type = auth_type;
            this.api_url_format = api_url_format;
            this.same_oauth_signing_url = same_oauth_signing_url;
            this.no_version_suffix = no_version_suffix;
        }

        @Override
        protected SignInResponse doInBackground(final Object... params) {
            try {
                switch (auth_type) {
                    case Accounts.AUTH_TYPE_OAUTH:
                        return authOAuth();
                    case Accounts.AUTH_TYPE_XAUTH:
                        return authxAuth();
                    case Accounts.AUTH_TYPE_BASIC:
                        return authBasic();
                    case Accounts.AUTH_TYPE_TWIP_O_MODE:
                        return authTwipOMode();
                }
                return authOAuth();
            } catch (final TwitterException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                return new SignInResponse(false, false, e);
            } catch (final AuthenticationException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                return new SignInResponse(false, false, e);
            }
        }

        private SignInResponse authBasic() throws TwitterException {
            final Twitter twitter = new TwitterFactory(conf).getInstance(new BasicAuthorization(username, password));
            final User user = twitter.verifyCredentials();
            final long user_id = user.getId();
            if (user_id <= 0) return new SignInResponse(false, false, null);
            final int color = analyseUserProfileColor(user);
            return new SignInResponse(conf, username, password, user, color, api_url_format,
                    no_version_suffix);
        }

        private SignInResponse authOAuth() throws AuthenticationException, TwitterException {
            final Twitter twitter = new TwitterFactory(conf).getInstance();
            final OAuthPasswordAuthenticator authenticator = new OAuthPasswordAuthenticator(twitter);
            final AccessToken access_token = authenticator.getOAuthAccessToken(username, password);
            final long user_id = access_token.getUserId();
            if (user_id <= 0) return new SignInResponse(false, false, null);
            final User user = twitter.verifyCredentials();
            final int color = analyseUserProfileColor(user);
            return new SignInResponse(conf, access_token, user, Accounts.AUTH_TYPE_OAUTH, color,
                    api_url_format, same_oauth_signing_url, no_version_suffix);
        }

        private SignInResponse authTwipOMode() throws TwitterException {
            final Twitter twitter = new TwitterFactory(conf).getInstance(new TwipOModeAuthorization());
            final User user = twitter.verifyCredentials();
            final long user_id = user.getId();
            if (user_id <= 0) return new SignInResponse(false, false, null);
            final int color = analyseUserProfileColor(user);
            return new SignInResponse(conf, user, color, api_url_format, no_version_suffix);
        }

        private SignInResponse authxAuth() throws TwitterException {
            final Twitter twitter = new TwitterFactory(conf).getInstance();
            final AccessToken accessToken = twitter.getOAuthAccessToken(username, password);
            final User user = twitter.verifyCredentials();
            final long user_id = user.getId();
            if (user_id <= 0) return new SignInResponse(false, false, null);
            final int color = analyseUserProfileColor(user);
            return new SignInResponse(conf, accessToken, user, Accounts.AUTH_TYPE_XAUTH, color,
                    api_url_format, same_oauth_signing_url, no_version_suffix);
        }

    }

    /*@Override
    public int getThemeResourceId() {
        return ThemeUtils.getThemeResource(this);
    }*/

    static class SignInResponse {

        public final boolean already_logged_in, succeed;
        public final Exception exception;
        public final Configuration conf;
        public final String basic_username, basic_password;
        public final AccessToken access_token;
        public final User user;
        public final int auth_type, color;
        public final String api_url_format;
        public final boolean same_oauth_signing_url, no_version_suffix;

        public SignInResponse(final boolean already_logged_in, final boolean succeed, final Exception exception) {
            this(already_logged_in, succeed, exception, null, null, null, null, null, 0, 0, null, false, false);
        }

        public SignInResponse(final boolean already_logged_in, final boolean succeed, final Exception exception,
                              final Configuration conf, final String basic_username, final String basic_password,
                              final AccessToken access_token, final User user, final int auth_type, final int color,
                              final String api_url_format, final boolean same_oauth_signing_url, final boolean no_version_suffix) {
            this.already_logged_in = already_logged_in;
            this.succeed = succeed;
            this.exception = exception;
            this.conf = conf;
            this.basic_username = basic_username;
            this.basic_password = basic_password;
            this.access_token = access_token;
            this.user = user;
            this.auth_type = auth_type;
            this.color = color;
            this.api_url_format = api_url_format;
            this.same_oauth_signing_url = same_oauth_signing_url;
            this.no_version_suffix = no_version_suffix;
        }

        public SignInResponse(final Configuration conf, final AccessToken access_token, final User user,
                              final int auth_type, final int color, final String api_url_format,
                              final boolean same_oauth_signing_url, final boolean no_version_suffix) {
            this(false, true, null, conf, null, null, access_token, user, auth_type, color, api_url_format,
                    same_oauth_signing_url, no_version_suffix);
        }

        public SignInResponse(final Configuration conf, final String basic_username, final String basic_password,
                              final User user, final int color, final String api_url_format,
                              final boolean no_version_suffix) {
            this(false, true, null, conf, basic_username, basic_password, null, user, Accounts.AUTH_TYPE_BASIC, color,
                    api_url_format, false, no_version_suffix);
        }

        public SignInResponse(final Configuration conf, final User user, final int color,
                              final String api_url_format, final boolean no_version_suffix) {
            this(false, true, null, conf, null, null, null, user, Accounts.AUTH_TYPE_TWIP_O_MODE, color,
                    api_url_format, false, no_version_suffix);
        }
    }
}
