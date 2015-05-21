/*
 * 				Twidere - Twitter client for Android
 * 
 *  Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.getlantern.firetweet.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;

import org.getlantern.firetweet.Constants;
import org.getlantern.firetweet.app.FireTweetApplication;
import org.getlantern.firetweet.fragment.support.BasePullToRefreshListFragment;
import org.getlantern.firetweet.util.AsyncTwitterWrapper;
import org.getlantern.firetweet.util.MessagesManager;
import org.getlantern.firetweet.util.ThemeUtils;

@SuppressLint("Registered")
public class BaseActivity extends BaseThemedActivity implements Constants {

	private boolean mInstanceStateSaved, mIsVisible, mIsOnTop;

	public MessagesManager getMessagesManager() {
		return getFireTweetApplication() != null ? getTwidereApplication().getMessagesManager() : null;
	}

    @Override
	public int getThemeColor() {
		return ThemeUtils.getUserAccentColor(this);
	}

	@Override
	public int getThemeResourceId() {
		return ThemeUtils.getThemeResource(this);
	}

	public FireTweetApplication getFireTweetApplication() {
		return (FireTweetApplication) getApplication();
	}

	public AsyncTwitterWrapper getTwitterWrapper() {
		return getFireTweetApplication() != null ? getTwidereApplication().getTwitterWrapper() : null;
	}

	public boolean isOnTop() {
		return mIsOnTop;
	}

	public boolean isVisible() {
		return mIsVisible;
	}

	protected BasePullToRefreshListFragment getCurrentPullToRefreshFragment() {
		return null;
	}

	protected boolean isStateSaved() {
		return mInstanceStateSaved;
	}

	@Override
	protected void onPause() {
		mIsOnTop = false;
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mInstanceStateSaved = false;
		mIsOnTop = true;
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		mInstanceStateSaved = true;
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStart() {
		super.onStart();
		mIsVisible = true;
		final MessagesManager croutons = getMessagesManager();
		if (croutons != null) {
			croutons.addMessageCallback(this);
		}
	}

	@Override
	protected void onStop() {
		mIsVisible = false;
		final MessagesManager croutons = getMessagesManager();
		if (croutons != null) {
			croutons.removeMessageCallback(this);
		}
		super.onStop();
	}

}
