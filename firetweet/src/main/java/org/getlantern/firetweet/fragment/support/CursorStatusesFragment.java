/*
 * Firetweet - Twitter client for Android
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

package org.getlantern.firetweet.fragment.support;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.Loader;

import com.squareup.otto.Subscribe;

import org.getlantern.querybuilder.Columns.Column;
import org.getlantern.querybuilder.Expression;
import org.getlantern.querybuilder.RawItemArray;
import org.getlantern.firetweet.adapter.CursorStatusesAdapter;
import org.getlantern.firetweet.loader.support.ExtendedCursorLoader;
import org.getlantern.firetweet.provider.FiretweetDataStore.Accounts;
import org.getlantern.firetweet.provider.FiretweetDataStore.Statuses;
import org.getlantern.firetweet.util.AsyncTaskUtils;
import org.getlantern.firetweet.util.Utils;
import org.getlantern.firetweet.util.message.FavoriteCreatedEvent;
import org.getlantern.firetweet.util.message.FavoriteDestroyedEvent;
import org.getlantern.firetweet.util.message.GetStatusesTaskEvent;
import org.getlantern.firetweet.util.message.StatusDestroyedEvent;
import org.getlantern.firetweet.util.message.StatusListChangedEvent;
import org.getlantern.firetweet.util.message.StatusRetweetedEvent;

import static org.getlantern.firetweet.util.Utils.buildStatusFilterWhereClause;
import static org.getlantern.firetweet.util.Utils.getNewestStatusIdsFromDatabase;
import static org.getlantern.firetweet.util.Utils.getOldestStatusIdsFromDatabase;
import static org.getlantern.firetweet.util.Utils.getTableNameByUri;

/**
 * Created by mariotaku on 14/12/3.
 */
public abstract class CursorStatusesFragment extends AbsStatusesFragment<Cursor> {

    @Override
    protected void onLoadingFinished() {

    }

    private ContentObserver mContentObserver;

    public abstract Uri getContentUri();

    @Override
    public Loader<Cursor> onCreateStatusesLoader(final Context context,
                                                 final Bundle args,
                                                 final boolean fromUser) {
        final Uri uri = getContentUri();
        final String table = getTableNameByUri(uri);
        final String sortOrder = getSortOrder();
        final long[] accountIds = getAccountIds();
        final Expression accountWhere = Expression.in(new Column(Statuses.ACCOUNT_ID), new RawItemArray(accountIds));
        final Expression filterWhere = getFiltersWhere(table), where;
        if (filterWhere != null) {
            where = Expression.and(accountWhere, filterWhere);
        } else {
            where = accountWhere;
        }
        final String selection = processWhere(where).getSQL();
        getAdapter().setShowAccountsColor(accountIds.length > 1);
        return new ExtendedCursorLoader(context, uri, Statuses.COLUMNS, selection, null, sortOrder, fromUser);
    }

    @Override
    protected Object createMessageBusCallback() {
        return new CursorStatusesBusCallback();
    }


    protected class CursorStatusesBusCallback {

        @Subscribe
        public void notifyGetStatusesTaskChanged(GetStatusesTaskEvent event) {
            if (!event.uri.equals(getContentUri())) return;
            setRefreshing(event.running);
            if (!event.running) {
                setLoadMoreIndicatorVisible(false);
                setRefreshEnabled(true);
            }
        }

        @Subscribe
        public void notifyFavoriteCreated(FavoriteCreatedEvent event) {
        }

        @Subscribe
        public void notifyFavoriteDestroyed(FavoriteDestroyedEvent event) {
        }

        @Subscribe
        public void notifyStatusDestroyed(StatusDestroyedEvent event) {
        }

        @Subscribe
        public void notifyStatusListChanged(StatusListChangedEvent event) {
        }

        @Subscribe
        public void notifyStatusRetweeted(StatusRetweetedEvent event) {
        }

    }

    @Override
    protected long[] getAccountIds() {
        final Bundle args = getArguments();
        if (args != null && args.getLong(EXTRA_ACCOUNT_ID) > 0) {
            return new long[]{args.getLong(EXTRA_ACCOUNT_ID)};
        }
        return Utils.getActivatedAccountIds(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
        final ContentResolver cr = getContentResolver();
        mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                reloadStatuses();
            }
        };
        cr.registerContentObserver(Accounts.CONTENT_URI, true, mContentObserver);
        updateRefreshState();
    }

    protected void reloadStatuses() {
        final Bundle args = new Bundle(), fragmentArgs = getArguments();
        if (fragmentArgs != null) {
            args.putAll(fragmentArgs);
            args.putBoolean(EXTRA_FROM_USER, true);
        }
        getLoaderManager().restartLoader(0, args, this);
    }

    @Override
    public void onStop() {
        final ContentResolver cr = getContentResolver();
        cr.unregisterContentObserver(mContentObserver);
        super.onStop();
    }

    @Override
    protected boolean hasMoreData(final Cursor cursor) {
        return cursor != null && cursor.getCount() != 0;
    }

    @Override
    protected CursorStatusesAdapter onCreateAdapter(final Context context, final boolean compact) {
        return new CursorStatusesAdapter(context, compact);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        getAdapter().setData(null);
    }

    @Override
    public void onLoadMoreContents() {
        super.onLoadMoreContents();
        AsyncTaskUtils.executeTask(new AsyncTask<Object, Object, long[][]>() {

            @Override
            protected long[][] doInBackground(final Object... params) {
                final long[][] result = new long[3][];
                result[0] = getAccountIds();
                result[1] = getOldestStatusIds(result[0]);
                return result;
            }

            @Override
            protected void onPostExecute(final long[][] result) {
                getStatuses(result[0], result[1], result[2]);
            }

        });
    }

    @Override
    public boolean triggerRefresh() {
        AsyncTaskUtils.executeTask(new AsyncTask<Object, Object, long[][]>() {

            @Override
            protected long[][] doInBackground(final Object... params) {
                final long[][] result = new long[3][];
                result[0] = getAccountIds();
                result[2] = getNewestStatusIds(result[0]);
                return result;
            }

            @Override
            protected void onPostExecute(final long[][] result) {
                getStatuses(result[0], result[1], result[2]);
            }

        });
        return true;
    }

    protected Expression getFiltersWhere(String table) {
        if (!isFilterEnabled()) return null;
        return buildStatusFilterWhereClause(table, null);
    }

    protected long[] getNewestStatusIds(long[] accountIds) {
        return getNewestStatusIdsFromDatabase(getActivity(), getContentUri(), accountIds);
    }

    protected abstract int getNotificationType();

    protected long[] getOldestStatusIds(long[] accountIds) {
        return getOldestStatusIdsFromDatabase(getActivity(), getContentUri(), accountIds);
    }

    protected abstract boolean isFilterEnabled();

    protected Expression processWhere(final Expression where) {
        return where;
    }

    protected abstract void updateRefreshState();

    private String getSortOrder() {
        final SharedPreferences preferences = getSharedPreferences();
        final boolean sortById = preferences.getBoolean(KEY_SORT_TIMELINE_BY_ID, false);
        return sortById ? Statuses.SORT_ORDER_STATUS_ID_DESC : Statuses.SORT_ORDER_TIMESTAMP_DESC;
    }
}
