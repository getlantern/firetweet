package org.getlantern.firetweet.model;

import org.getlantern.firetweet.FiretweetConstants;

import go.Go;
import go.flashlight.Flashlight;

import android.content.Context;

import 	android.os.StrictMode;

import com.crashlytics.android.Crashlytics;

/**
 * Created by todd on 4/25/15.
 */
public class Lantern {

    private static boolean lanternStarted = false;

    public static void start(Context context) {

        StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old)
                .permitDiskWrites()
                .build());

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

            } catch (Exception e) {
                Crashlytics.logException(e);
                throw new RuntimeException(e);
            }

            StrictMode.setThreadPolicy(old);
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
