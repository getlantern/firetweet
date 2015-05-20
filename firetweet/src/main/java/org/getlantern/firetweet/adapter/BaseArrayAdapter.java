/*
 * 				Firetweet - Twitter client for Android
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

package org.getlantern.firetweet.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import org.getlantern.firetweet.adapter.iface.IBaseAdapter;
import org.getlantern.firetweet.app.FiretweetApplication;
import org.getlantern.firetweet.util.MediaLoaderWrapper;
import org.getlantern.firetweet.util.OnLinkClickHandler;
import org.getlantern.firetweet.util.FiretweetLinkify;
import org.getlantern.firetweet.util.Utils;

import java.util.Collection;

public class BaseArrayAdapter<T> extends ArrayAdapter<T> implements IBaseAdapter, OnSharedPreferenceChangeListener {

    private final FiretweetLinkify mLinkify;

    private float mTextSize;
    private int mLinkHighlightOption;

    private boolean mDisplayProfileImage, mDisplayNameFirst, mShowAccountColor;

    private final SharedPreferences mNicknamePrefs, mColorPrefs;
    private final MediaLoaderWrapper mImageLoader;

    public BaseArrayAdapter(final Context context, final int layoutRes) {
        this(context, layoutRes, null);
    }

    public BaseArrayAdapter(final Context context, final int layoutRes, final Collection<? extends T> collection) {
        super(context, layoutRes, collection);
        final FiretweetApplication app = FiretweetApplication.getInstance(context);
        mLinkify = new FiretweetLinkify(new OnLinkClickHandler(context, app.getMultiSelectManager()));
        mImageLoader = app.getMediaLoaderWrapper();
        mNicknamePrefs = context.getSharedPreferences(USER_NICKNAME_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mColorPrefs = context.getSharedPreferences(USER_COLOR_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mNicknamePrefs.registerOnSharedPreferenceChangeListener(this);
        mColorPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public MediaLoaderWrapper getImageLoader() {
        return mImageLoader;
    }

    @Override
    public final int getLinkHighlightOption() {
        return mLinkHighlightOption;
    }

    public final FiretweetLinkify getLinkify() {
        return mLinkify;
    }

    @Override
    public final float getTextSize() {
        return mTextSize;
    }

    @Override
    public final boolean isDisplayNameFirst() {
        return mDisplayNameFirst;
    }

    @Override
    public final boolean isProfileImageDisplayed() {
        return mDisplayProfileImage;
    }

    @Override
    public final boolean isShowAccountColor() {
        return mShowAccountColor;
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences preferences, final String key) {
        if (KEY_DISPLAY_PROFILE_IMAGE.equals(key) || KEY_MEDIA_PREVIEW_STYLE.equals(key)
                || KEY_DISPLAY_SENSITIVE_CONTENTS.equals(key)) {
            notifyDataSetChanged();
        }
    }

    @Override
    public final void setDisplayNameFirst(final boolean nameFirst) {
        mDisplayNameFirst = nameFirst;
    }

    @Override
    public final void setDisplayProfileImage(final boolean display) {
        mDisplayProfileImage = display;
    }

    @Override
    public final void setLinkHighlightOption(final String option) {
        final int optionInt = Utils.getLinkHighlightingStyleInt(option);
        mLinkify.setHighlightOption(optionInt);
        if (optionInt == mLinkHighlightOption) return;
        mLinkHighlightOption = optionInt;
    }

    @Override
    public final void setShowAccountColor(final boolean show) {
        mShowAccountColor = show;
    }

    @Override
    public final void setTextSize(final float textSize) {
        mTextSize = textSize;
    }

}
