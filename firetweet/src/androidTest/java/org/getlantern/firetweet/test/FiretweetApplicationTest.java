package org.getlantern.firetweet.test;

import android.test.ApplicationTestCase;

import org.getlantern.firetweet.app.FiretweetApplication;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class FiretweetApplicationTest extends ApplicationTestCase<FiretweetApplication> {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public FiretweetApplicationTest() {
        super(FiretweetApplication.class);
    }

}
