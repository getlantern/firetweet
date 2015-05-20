package org.getlantern.firetweet.util.net;

import org.getlantern.firetweet.app.FiretweetApplication;

import twitter4j.http.HostAddressResolver;
import twitter4j.http.HostAddressResolverFactory;
import twitter4j.http.HttpClientConfiguration;

public class FiretweetHostResolverFactory implements HostAddressResolverFactory {

	private final FiretweetApplication mApplication;

	public FiretweetHostResolverFactory(final FiretweetApplication application) {
		mApplication = application;
	}

	@Override
	public HostAddressResolver getInstance(final HttpClientConfiguration conf) {
		return mApplication.getHostAddressResolver();
	}

}
