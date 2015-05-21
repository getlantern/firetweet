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

package org.getlantern.firetweet.view.holder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import org.mariotaku.messagebubbleview.library.MessageBubbleView;
import org.getlantern.firetweet.R;
import org.getlantern.firetweet.adapter.MessageConversationAdapter;
import org.getlantern.firetweet.model.ParcelableDirectMessage.CursorIndices;
import org.getlantern.firetweet.model.ParcelableMedia;
import org.getlantern.firetweet.util.ColorUtils;
import org.getlantern.firetweet.util.MediaLoaderWrapper;
import org.getlantern.firetweet.util.SimpleValueSerializer;
import org.getlantern.firetweet.util.FiretweetLinkify;
import org.getlantern.firetweet.util.Utils;
import org.getlantern.firetweet.util.Utils.OnMediaClickListener;
import org.getlantern.firetweet.view.CardMediaContainer;

public class MessageConversationViewHolder extends ViewHolder implements OnMediaClickListener {

    public final CardMediaContainer mediaContainer;
    public final TextView text, time;

    private final MessageBubbleView messageContent;
    private final MessageConversationAdapter adapter;

    private final int textColorPrimary, textColorPrimaryInverse, textColorSecondary, textColorSecondaryInverse;

    private float textSize;

    public MessageConversationViewHolder(final MessageConversationAdapter adapter, final View itemView) {
        super(itemView);
        this.adapter = adapter;
        final Context context = itemView.getContext();
        final TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary,
                android.R.attr.textColorPrimaryInverse, android.R.attr.textColorSecondary,
                android.R.attr.textColorSecondaryInverse});
        textColorPrimary = a.getColor(0, 0);
        textColorPrimaryInverse = a.getColor(1, 0);
        textColorSecondary = a.getColor(2, 0);
        textColorSecondaryInverse = a.getColor(3, 0);
        a.recycle();
        messageContent = (MessageBubbleView) itemView.findViewById(R.id.message_content);
        text = (TextView) itemView.findViewById(R.id.text);
        time = (TextView) itemView.findViewById(R.id.time);
        mediaContainer = (CardMediaContainer) itemView.findViewById(R.id.media_preview_container);
    }

    public void displayMessage(Cursor cursor, CursorIndices indices) {
        final Context context = adapter.getContext();
        final FiretweetLinkify linkify = adapter.getLinkify();
        final MediaLoaderWrapper loader = adapter.getImageLoader();

        final long accountId = cursor.getLong(indices.account_id);
        final long timestamp = cursor.getLong(indices.message_timestamp);
        final ParcelableMedia[] media = SimpleValueSerializer.fromSerializedString(cursor.getString(indices.media), ParcelableMedia.SIMPLE_CREATOR);
        text.setText(Html.fromHtml(cursor.getString(indices.text)));
        linkify.applyAllLinks(text, accountId, false);
        text.setMovementMethod(null);
        time.setText(Utils.formatToLongTimeString(context, timestamp));
        mediaContainer.setVisibility(media != null && media.length > 0 ? View.VISIBLE : View.GONE);
        mediaContainer.displayMedia(media, loader, accountId, this, null);
    }

    @Override
    public void onMediaClick(View view, ParcelableMedia media, long accountId) {
        Utils.openMedia(adapter.getContext(), adapter.getDirectMessage(getAdapterPosition()), media);
    }

    public void setMessageColor(int color) {
        final ColorStateList colorStateList = ColorStateList.valueOf(color);
        messageContent.setBubbleColor(colorStateList);
        final int textLuminancePrimary = ColorUtils.getYIQLuminance(textColorPrimary);
        final int textPrimaryDark, textPrimaryLight, textSecondaryDark, textSecondaryLight;
        if (textLuminancePrimary < 128) {
            textPrimaryDark = textColorPrimary;
            textPrimaryLight = textColorPrimaryInverse;
            textSecondaryDark = textColorSecondary;
            textSecondaryLight = textColorSecondaryInverse;
        } else {
            textPrimaryDark = textColorPrimaryInverse;
            textPrimaryLight = textColorPrimary;
            textSecondaryDark = textColorSecondaryInverse;
            textSecondaryLight = textColorSecondary;
        }
        final int textContrastPrimary = ColorUtils.getContrastYIQ(color, 192, textPrimaryDark, textPrimaryLight);
        final int textContrastSecondary = ColorUtils.getContrastYIQ(color, 192, textSecondaryDark, textSecondaryLight);
        text.setTextColor(textContrastPrimary);
        text.setLinkTextColor(textContrastSecondary);
        time.setTextColor(textContrastSecondary);
    }

    public void setTextSize(final float textSize) {
        if (this.textSize != textSize) {
            this.textSize = textSize;
            text.setTextSize(textSize);
            time.setTextSize(textSize * 0.75f);
        }
    }

}
