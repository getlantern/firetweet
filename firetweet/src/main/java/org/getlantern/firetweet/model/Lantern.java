package org.getlantern.firetweet.model;

import org.getlantern.firetweet.FiretweetConstants;
import org.getlantern.firetweet.activity.support.BrowserSignInActivity;
import org.getlantern.firetweet.activity.support.UpdaterActivity;
import org.getlantern.firetweet.provider.FiretweetDataStore;

import go.Go;
import go.flashlight.Flashlight;

import android.content.Context;
import android.os.Handler;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.analytics.GoogleAnalytics;

import android.content.Intent;
import android.content.pm.PackageManager;
import 	android.os.StrictMode;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

/**
 * Created by todd on 4/25/15.
 */
public class Lantern {

    private static boolean lanternStarted = false;
    private static Context context = null;

    public static Analytics analytics;

    private static final String LOG_TAG = "Lantern";

    public static void start(Context ctx) {

        context = ctx;

        if (!lanternStarted) {
            // Initializing application context.
            try {
                Go.init(context);

                Log.d(LOG_TAG, "Starting Lantern...");

                // init loads libgojni.so and starts the runtime
                Flashlight.RunClientProxy("127.0.0.1:9192", FiretweetConstants.APP_NAME);
                System.setProperty("http.proxyHost", "127.0.0.1");
                System.setProperty("http.proxyPort", "9192");
                System.setProperty("https.proxyHost", "127.0.0.1");
                System.setProperty("https.proxyPort", "9192");
                // specify that all of our HTTP traffic should be routed through
                // our local proxy

                lanternStarted = true;


                analytics = new Analytics(context);
                analytics.sendNewSessionEvent();


                // small delay and then check for a new version of FireTweet
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkNewVersion();
                    }
                }, 1000);


            } catch (Exception e) {
                Crashlytics.logException(e);
                throw new RuntimeException(e);
            }
        }
    }

    public static void stop() {
        if (lanternStarted) {
            try {
                Flashlight.StopClientProxy();
                lanternStarted = false;
            } catch (Exception e) {
                Crashlytics.logException(e);
            }
        }
    }

    public static void checkNewVersion() {
        if (context != null) {
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                String version = pInfo.versionName;
                Log.d(LOG_TAG, "Latest version of FireTweet is " + Flashlight.GetFireTweetVersion() +
                        "; currently running version of FireTweet: " + version);
                if (Flashlight.GetFireTweetVersion() != version) {
                    // Latest version of FireTweet and the version currently running differ
                    // display the update view
                    final Intent intent = new Intent(context, UpdaterActivity.class);
                    context.startActivity(intent);
                }

            } catch (PackageManager.NameNotFoundException e) {
                Log.e(LOG_TAG, "Error fetching package information");
            }
        }
    }
}
