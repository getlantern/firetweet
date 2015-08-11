package org.getlantern.firetweet.model;

import org.getlantern.firetweet.FiretweetConstants;

// Flashlight client
import go.client.Client;

import android.content.Context;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.analytics.GoogleAnalytics;

import android.os.StrictMode;

import com.crashlytics.android.Crashlytics;

/**
 * Created by todd on 4/25/15.
 */
public class Lantern {

    private static boolean lanternStarted = false;

    public static Analytics analytics;

    public static void start(Context context) {

        if (!lanternStarted) {
            // Initializing application context.
            try {
                // init loads libgojni.so and starts the runtime
                Client.RunClientProxy("127.0.0.1:9192",
                                      FiretweetConstants.APP_NAME,
                                      new Client.GoCallback.Stub() {
                                          public void Do() {
                                              // Proxy ready callback: does nothing at the moment
                                          }
                                      });
                System.setProperty("http.proxyHost", "127.0.0.1");
                System.setProperty("http.proxyPort", "9192");
                System.setProperty("https.proxyHost", "127.0.0.1");
                System.setProperty("https.proxyPort", "9192");
                // specify that all of our HTTP traffic should be routed through
                // our local proxy

                lanternStarted = true;

                analytics = new Analytics(context);
                analytics.sendNewSessionEvent();

            } catch (Exception e) {
                Crashlytics.logException(e);
                throw new RuntimeException(e);
            }
        }
    }

    public static void stop() {
        if (lanternStarted) {
            try {
                Client.StopClientProxy();
                lanternStarted = false;
            } catch (Exception e) {
                Crashlytics.logException(e);
            }
        }
    }
}
