package org.getlantern.firetweet.model;

import org.getlantern.firetweet.FiretweetConstants;

import go.Go;
import go.flashlight.Flashlight;

import android.content.Context;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.analytics.GoogleAnalytics;

import 	android.os.StrictMode;

import com.crashlytics.android.Crashlytics;

/**
 * Created by todd on 4/25/15.
 */
public class Lantern {

    private static boolean lanternStarted = false;

    public static GoogleAnalytics analytics;
    public static Tracker tracker;

    public static void start(Context context) {

        if (!lanternStarted) {
            // Initializing application context.
            try {
                Go.init(context);

                // init loads libgojni.so and starts the runtime
                Flashlight.RunClientProxy("127.0.0.1:9192", FiretweetConstants.APP_NAME);
                System.setProperty("http.proxyHost", "127.0.0.1");
                System.setProperty("http.proxyPort", "9192");
                System.setProperty("https.proxyHost", "127.0.0.1");
                System.setProperty("https.proxyPort", "9192");
                // specify that all of our HTTP traffic should be routed through
                // our local proxy

                lanternStarted = true;


                analytics = GoogleAnalytics.getInstance(context);
                analytics.setLocalDispatchPeriod(1800);

                tracker = analytics.newTracker("UA-21408036-4"); // Replace with actual tracker/property Id
                tracker.enableAdvertisingIdCollection(true);
                tracker.enableAutoActivityTracking(true);

                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Session")
                        .setAction("Start")
                        .setLabel("android")
                        .build());

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
}
