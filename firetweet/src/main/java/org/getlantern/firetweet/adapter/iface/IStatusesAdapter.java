package org.getlantern.firetweet.adapter.iface;

import org.getlantern.firetweet.model.ParcelableStatus;
import org.getlantern.firetweet.util.FiretweetLinkify;
import org.getlantern.firetweet.view.holder.StatusViewHolder.StatusClickListener;

/**
 * Created by mariotaku on 14/11/18.
 */
public interface IStatusesAdapter<Data> extends IContentCardAdapter, StatusClickListener {

    ParcelableStatus getStatus(int position);

    int getStatusesCount();

    long getStatusId(int position);

    FiretweetLinkify getFiretweetLinkify();

    boolean isMediaPreviewEnabled();

    int getLinkHighlightingStyle();

    boolean isNameFirst();

    boolean isSensitiveContentEnabled();

    boolean isCardActionsHidden();

    void setData(Data data);

    boolean shouldShowAccountsColor();
}
