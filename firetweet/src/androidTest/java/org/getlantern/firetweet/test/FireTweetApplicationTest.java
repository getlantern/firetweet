package org.getlantern.firetweet.test;

import android.test.ApplicationTestCase;

import org.getlantern.firetweet.app.FireTweetApplication;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class FireTweetApplicationTest extends ApplicationTestCase<FireTweetApplication> {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public FireTweetApplicationTest() {
        super(FireTweetApplication.class);
    }

}
