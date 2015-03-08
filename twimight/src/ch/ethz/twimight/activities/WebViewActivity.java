package ch.ethz.twimight.activities;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import ch.ethz.twimight.R;
import ch.ethz.twimight.data.HtmlPagesDbHelper;
import ch.ethz.twimight.net.Html.HtmlPage;
import ch.ethz.twimight.net.Html.WebArchiveReader;
import ch.ethz.twimight.net.twitter.Tweets;
import ch.ethz.twimight.util.SDCardHelper;

public class WebViewActivity extends Activity {
	
	public static final String HTML_PAGE = "html_page";	
	public static final String TAG = "WebViewActivity";
	private SDCardHelper sdCardHelper;
	String url;
	private ProgressDialog progressBar; 
	Uri webUri;

	private class ReadWebArchiveTask extends AsyncTask<InputStream,Void,Boolean>{

		WebArchiveReader wr;
		WebView web;
		
		public ReadWebArchiveTask(WebArchiveReader wr, WebView web) {
			this.wr = wr;
			this.web = web;
		}

		@Override
		protected Boolean doInBackground(InputStream... params) {
			// To read from a file instead of an asset, use:
			// FileInputStream is = new FileInputStream(fileName);
			InputStream is =  params[0];
			if (wr.readWebArchive(is)) 
				return true;
			else
				return false;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (success)
				try {
					wr.loadToWebView(web);
				} catch (Exception e) {	
					//Log.e(TAG,"faulty page",e);
					markFaultyPage();					
				}

		}

		private void markFaultyPage() {
			HtmlPagesDbHelper htmlDbHelper = new HtmlPagesDbHelper(getApplicationContext());
			htmlDbHelper.open();	
			Cursor c = htmlDbHelper.getPageInfo(url);			
			sdCardHelper.deleteFile(webUri.getPath());
			htmlDbHelper.updatePage(url,
									null,
									c.getLong(c.getColumnIndex(HtmlPage.COL_DISASTERID)),									
									c.getInt(c.getColumnIndex(HtmlPage.COL_FORCED)),
									c.getInt(c.getColumnIndex(HtmlPage.COL_ATTEMPTS)));
			getContentResolver().notifyChange(Tweets.TABLE_TIMELINE_URI, null);		
			Toast.makeText(getBaseContext(), getString(R.string.faulty_page), Toast.LENGTH_LONG).show();
			finish();
			
		}



	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.webview);
		
		Intent intent = getIntent();
		
		url = intent.getStringExtra("url");	
		String[] filePath = {HtmlPage.HTML_PATH + "/" + LoginActivity.getTwitterId(this)};

		sdCardHelper = new SDCardHelper();
		WebView web = (WebView) findViewById(R.id.webview);
		web.getSettings().setJavaScriptEnabled(true);
		web.getSettings().setDomStorageEnabled(true); //twitter api and youtube api hack		 
		web.getSettings().setBuiltInZoomControls(true); //Enable Multitouch if supported
		

		if(sdCardHelper.checkSDState(filePath)){		
			
			progressBar = ProgressDialog.show(this, getString(R.string.loading), url);
			progressBar.setCancelable(true);
			
		    webUri = Uri.fromFile(sdCardHelper.getFileFromSDCard(filePath[0], intent.getStringExtra("filename")));
			Log.i(TAG, webUri.getPath());

			try {

				FileInputStream is = new FileInputStream(webUri.getPath());
				WebArchiveReader wr = new WebArchiveReader() {
					protected void onFinished(WebView v) {
						// we are notified here when the page is fully loaded.
						Log.d(TAG, "load finished");
						continueWhenLoaded(v);
					}
				};
				new ReadWebArchiveTask(wr, web).execute(is);
				
			} catch (IOException e) {			            
			}
			
			


		}

	}
	
	

	private void continueWhenLoaded(WebView webView) {
        Log.d(TAG, "Page from WebArchive fully loaded.");
        // If you need to set your own WebViewClient, do it here,
        // after the WebArchive was fully loaded:
       
        webView.setWebViewClient(new WebClientView());      

        // Any other code we need to execute after loading a page from a WebArchive...
        if(progressBar.isShowing()){
			progressBar.dismiss();
			Toast.makeText(getBaseContext(), getString(R.string.loading_finished), Toast.LENGTH_LONG).show();
		}
    }
	

    
	private class WebClientView extends WebViewClient {

		@Override
		public void onLoadResource(WebView view, String url) {
			// TODO Auto-generated method stub
			super.onLoadResource(view, url);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			// TODO Auto-generated method stub
			Log.d(TAG, "onPageFinished without download");
			super.onPageFinished(view, url);
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			// TODO Auto-generated method stub
			Log.d(TAG, "onPageStarted");
			super.onPageStarted(view, url, favicon);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// TODO Auto-generated method stubsuper.shouldOverrideUrlLoading(view, url);
			return true;
		}
		
		
	}
	

}
