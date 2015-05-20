package org.getlantern.firetweet.test;

import android.test.ApplicationTestCase;

import org.getlantern.firetweet.app.TwidereApplication;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class TwidereApplicationTest extends ApplicationTestCase<TwidereApplication> {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public TwidereApplicationTest() {
        super(TwidereApplication.class);
    }

}
