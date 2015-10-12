package org.getlantern.firetweet.model;

import org.getlantern.firetweet.FiretweetConstants;

// Flashlight client
import go.client.Client;

import android.content.Context;
import android.util.Log;
import android.os.StrictMode;

import com.crashlytics.android.Crashlytics;

/**
 * Created by todd on 4/25/15.
 */
public class Lantern {

    private static final String TAG = "Lantern";
    private static boolean lanternStarted = false;
    public static Analytics analytics;

    public static void start(final Context context) {

        if (!lanternStarted) {
            // Initializing application context.
            try {


                Client.GoCallback.Stub callback = new Client.GoCallback.Stub() {
                    public void AfterStart() {
                        Log.d(TAG, "Lantern successfully started.");
                        // specify that all of our HTTP traffic should be routed through
                        // our local proxy
                        System.setProperty("http.proxyHost", "127.0.0.1");
                        System.setProperty("http.proxyPort", "9192");
                        System.setProperty("https.proxyHost", "127.0.0.1");
                        System.setProperty("https.proxyPort", "9192");

                        lanternStarted = true;

                        analytics = new Analytics(context);
                        analytics.sendNewSessionEvent();
                    }

                    public void AfterConfigure() {

                    }
                };
                // init loads libgojni.so and starts the runtime
                Client.RunClientProxy("127.0.0.1:9192",
                                      FiretweetConstants.APP_NAME,
                                      null,
                                      callback);
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
