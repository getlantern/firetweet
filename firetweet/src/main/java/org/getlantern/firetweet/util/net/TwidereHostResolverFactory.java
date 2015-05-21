package org.getlantern.firetweet.util.net;

import org.getlantern.firetweet.app.FireTweetApplication;

import twitter4j.http.HostAddressResolver;
import twitter4j.http.HostAddressResolverFactory;
import twitter4j.http.HttpClientConfiguration;

public class TwidereHostResolverFactory implements HostAddressResolverFactory {

	private final FireTweetApplication mApplication;

	public TwidereHostResolverFactory(final FireTweetApplication application) {
		mApplication = application;
	}

	@Override
	public HostAddressResolver getInstance(final HttpClientConfiguration conf) {
		return mApplication.getHostAddressResolver();
	}

}
