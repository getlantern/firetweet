/*******************************************************************************
 * Copyright (c) 2011 ETH Zurich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Paolo Carta - Implementation
 *     Theus Hossmann - Implementation
 *     Dominik Schatzmann - Message specification
 ******************************************************************************/

package ch.ethz.twimight.util;

import org.xml.sax.XMLReader;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.Html.TagHandler;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Toast;
import ch.ethz.twimight.activities.SearchableActivity;
import ch.ethz.twimight.activities.ShowUserActivity;

public class TweetTagHandler implements TagHandler {

	Context context;
	private static final String TAG = "TweetTagHandler";

	public TweetTagHandler(Context context) {
		this.context = context;
	}

	/**
	 * Recognizes the tags
	 */
	@Override
	public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
		if(tag.equalsIgnoreCase("hashtag")) {
			processHashtag(opening, output);
		} else if(tag.equalsIgnoreCase("url")) {
			// no special treatments of url tags
		} else if(tag.equalsIgnoreCase("mention")){
			processMention(opening, output);
		}
	}

	/**
	 * Processes a hastag tag
	 * @param opening
	 * @param output
	 */
	private void processHashtag(boolean opening, Editable output) {
		int len = output.length();

		HashtagClickableSpan clickableSpan = new HashtagClickableSpan();

		if(opening) {
			output.setSpan(clickableSpan, len, len, Spannable.SPAN_MARK_MARK);
		} else {
			Object obj = getLastHashtag(output, ClickableSpan.class);
			int where = output.getSpanStart(obj);

			output.removeSpan(obj);

			if (where != len) {
				clickableSpan.setHashtag(output.subSequence(where, len).toString());
				output.setSpan(clickableSpan, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
	}

	private Object getLastHashtag(Editable text, Class kind) {
		Object[] objs = text.getSpans(0, text.length(), kind);

		if (objs.length == 0) {
			return null;
		} else {
			for(int i = objs.length;i>0;i--) {
				if(text.getSpanFlags(objs[i-1]) == Spannable.SPAN_MARK_MARK) {
					return objs[i-1];
				}
			}
			return null;
		}
	}

	/**
	 * Processes a mention tag
	 * @param opening
	 * @param output
	 */
	 private void processMention(boolean opening, Editable output) {

		 int len = output.length();

		 MentionClickableSpan clickableSpan = new MentionClickableSpan();

		 if(opening) {
			 output.setSpan(clickableSpan, len, len, Spannable.SPAN_MARK_MARK);
		 } else {
			 Object obj = getLastMention(output, ClickableSpan.class);
			 int where = output.getSpanStart(obj);

			 output.removeSpan(obj);

			 if (where != len) {
				 clickableSpan.setScreenname(output.subSequence(where+1, len).toString());
				 output.setSpan(clickableSpan, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			 }
		 }
	 }

	 private Object getLastMention(Editable text, Class kind) {
		 Object[] objs = text.getSpans(0, text.length(), kind);

		 if (objs.length == 0) {
			 return null;
		 } else {
			 for(int i = objs.length;i>0;i--) {
				 if(text.getSpanFlags(objs[i-1]) == Spannable.SPAN_MARK_MARK) {
					 return objs[i-1];
				 }
			 }
			 return null;
		 }
	 }

	 private class HashtagClickableSpan extends ClickableSpan {
		 private String hashtag;
		 
		 public void setHashtag(String hashtag){
			 this.hashtag = hashtag;
		 }
		 
		 public void onClick(View view) {
			 if(hashtag==null){
				 Toast.makeText(context, "There was an error, please try again", Toast.LENGTH_SHORT).show();
			 }else{
				 Intent i = new Intent(context, SearchableActivity.class);
				 i.putExtra(SearchManager.QUERY, hashtag);
				
				 context.startActivity(i);
			 }

		 }}

	 private class MentionClickableSpan extends ClickableSpan {
		 private String screenname;
		 public void setScreenname(String screenname){
			 this.screenname = screenname;
		 }
		 public void onClick(View view) {
			 if(screenname==null)
				 Toast.makeText(context, "There was an error, please try again.", Toast.LENGTH_SHORT).show();
			 else{
				 Intent i = new Intent(context, ShowUserActivity.class);
				 i.putExtra("screenname", screenname);
				 context.startActivity(i);
			 }

		 }}

}