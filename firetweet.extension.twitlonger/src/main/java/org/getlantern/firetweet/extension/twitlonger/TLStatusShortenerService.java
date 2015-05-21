package org.getlantern.firetweet.extension.twitlonger;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import org.getlantern.firetweet.IStatusShortener;
import org.getlantern.firetweet.extension.twitlonger.TwitLonger.TwitLongerException;
import org.getlantern.firetweet.extension.twitlonger.TwitLonger.TwitLongerResponse;
import org.getlantern.firetweet.model.ParcelableAccount;
import org.getlantern.firetweet.model.ParcelableStatusUpdate;
import org.getlantern.firetweet.model.StatusShortenResult;

import java.lang.ref.WeakReference;

/**
 * Tweet shortener example
 *
 * @author mariotaku
 */
public class TLStatusShortenerService extends Service implements Constants {

    private final StatusShortenerStub mBinder = new StatusShortenerStub(this);

    @Override
    public IBinder onBind(final Intent intent) {
        return mBinder;
    }

    /**
     * @return Shortened tweet.
     */
    public StatusShortenResult shorten(final ParcelableStatusUpdate status, final String overrideStatusText) {
        final TwitLonger tl = new TwitLonger(TWITLONGER_APP_NAME, TWITLONGER_API_KEY);
        try {
            final String text = overrideStatusText != null ? overrideStatusText : status.text;
            final ParcelableAccount account = status.accounts[0];
            final TwitLongerResponse response = tl.post(text, account.screen_name, status.in_reply_to_status_id, null);
            if (response != null) return StatusShortenResult.getInstance(response.content);
        } catch (final TwitLongerException e) {
            final int errorCode = e.getErrorCode() != 0 ? e.getErrorCode() : -1;
            return StatusShortenResult.getInstance(errorCode, e.getMessage());
        }
        return StatusShortenResult.getInstance(-1, "Unknown error");
    }

    /*
     * By making this a static class with a WeakReference to the Service, we
     * ensure that the Service can be GCd even when the system process still has
     * a remote reference to the stub.
     */
    private static final class StatusShortenerStub extends IStatusShortener.Stub {

        final WeakReference<TLStatusShortenerService> mService;

        public StatusShortenerStub(final TLStatusShortenerService service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public StatusShortenResult shorten(final ParcelableStatusUpdate status, final String overrideStatusText)
                throws RemoteException {
            return mService.get().shorten(status, overrideStatusText);
        }

    }

}
