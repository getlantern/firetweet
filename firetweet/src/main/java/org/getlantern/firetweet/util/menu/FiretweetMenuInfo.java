package org.getlantern.firetweet.util.menu;

import android.view.ContextMenu.ContextMenuInfo;

/**
 * Created by mariotaku on 14/10/27.
 */
public class FiretweetMenuInfo implements ContextMenuInfo {
    private final int highlightColor;
    private final boolean isHighlight;


    public FiretweetMenuInfo(boolean isHighlight) {
        this(isHighlight, 0);
    }

    public FiretweetMenuInfo(boolean isHighlight, int highlightColor) {
        this.isHighlight = isHighlight;
        this.highlightColor = highlightColor;
    }

    public int getHighlightColor(int def) {
        return highlightColor != 0 ? highlightColor : def;
    }

    public boolean isHighlight() {
        return isHighlight;
    }
}
