package org.mariotaku.twidere.activity;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import android.content.Context;

import org.mariotaku.twidere.R;
import org.mariotaku.twidere.activity.MainActivity;
import org.mariotaku.twidere.activity.support.HomeActivity;

import org.mariotaku.twidere.model.Lantern;

public class SplashScreen extends Activity {
 
    // Splash screen timer
    private static int SPLASH_TIME_OUT = 2000;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        final Intent i = new Intent(SplashScreen.this, HomeActivity.class);


        new Handler().postDelayed(new Runnable() {
 
            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */
 
            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                startActivity(i);
 
                // close this activity
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
 
}
