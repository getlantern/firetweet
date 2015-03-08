package ch.ethz.twimight.net.Html;

import java.io.File;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import ch.ethz.twimight.activities.LoginActivity;
import ch.ethz.twimight.data.HtmlPagesDbHelper;
import ch.ethz.twimight.net.twitter.Tweets;
import ch.ethz.twimight.util.SDCardHelper;

public class HtmlService extends Service {
	
	ArrayList<String> htmlUrls =  new ArrayList<String>();
	public static final String TAG = "HtmlService";
	
	public static final int DOWNLOAD_ALL = 1;	
	public static final int DOWNLOAD_ONLY_FORCED = 2;
	public static final String DOWNLOAD_REQUEST = "download_request";	
	public static final String DOWNLOAD_SINCE_TIME = "downloadSinceTime";
	
	private SDCardHelper sdCardHelper;
	private HtmlPagesDbHelper htmlDbHelper;
		
	Cursor c = null;
	


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {		
		
		ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		if(cm.getActiveNetworkInfo()==null || !cm.getActiveNetworkInfo().isConnected())			
			return START_NOT_STICKY;		
		
		if(intent != null){
			
			sdCardHelper = new SDCardHelper();
			htmlDbHelper = new HtmlPagesDbHelper(getApplicationContext());
			htmlDbHelper.open();			

			int serviceCommand = intent.getIntExtra(DOWNLOAD_REQUEST,DOWNLOAD_ALL);
			switch(serviceCommand){			

			case DOWNLOAD_ALL:
				bulkDownloadHtmlPages(false);
				break;			

			case DOWNLOAD_ONLY_FORCED:
				
				bulkDownloadHtmlPages(true);
				break;

			default:
				throw new IllegalArgumentException("Exception: Unknown download request");
			}
			return START_STICKY;
		}
		else return START_NOT_STICKY;

	}



	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	


	/**
	 * download and insert html pages with bulk tweets
	 * @author fshi
	 *
	 */
	private void bulkDownloadHtmlPages(boolean forced){
		
		long lastTime = getLastDownloadedTime(getBaseContext());
		if((System.currentTimeMillis() - lastTime) < 1000*30){
			return;
		}
		else{			
			new CleanCacheDownload(forced).execute();					
			
		}
		

	}
		
	//if downloaded pages > 100, clear those created 1 days ago
	private void checkCacheSize(){
		
		Log.i(TAG, "check cache size");
		Cursor c = htmlDbHelper.getDownloadedHtmls();
		if(c != null && c.getCount() > 100){
			htmlDbHelper.clearHtmlPages(1*24*3600*1000);
		}
	}
	
	
	private class CleanCacheDownload extends AsyncTask<Void,Void,Void>{
		boolean forced;
		
		public CleanCacheDownload(boolean forced) {
			this.forced = forced;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			
			checkCacheSize();				
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			downloadPages(forced);	
		}
		
		
		
	}
	
	
	/*
	private class fileDownload extends AsyncTask<ContentValues, Void, Boolean>{

		@Override
		protected Boolean doInBackground(ContentValues... params) {
			// TODO Auto-generated method stub
			
			ContentValues fileCV = params[0];
			
			Long tweetId = fileCV.getAsLong(HtmlPage.COL_TID);
		
			String url = fileCV.getAsString(HtmlPage.COL_URL);
			String filename = fileCV.getAsString(HtmlPage.COL_FILENAME);
			int forced = fileCV.getAsInteger(HtmlPage.COL_FORCED);
			int tries = fileCV.getAsInteger(HtmlPage.COL_ATTEMPTS);
			String[] filePath = {HtmlPage.HTML_PATH + "/" + LoginActivity.getTwitterId(getApplicationContext())};
			if(sdCardHelper.checkSDState(filePath)){
				File targetFile = sdCardHelper.getFileFromSDCard(filePath[0], filename);
				try {
			        URL fileUrl = new URL(url);
			        
		            URLConnection connection = fileUrl.openConnection();
		            connection.connect();

		            // download the file
		            InputStream input = new BufferedInputStream(fileUrl.openStream());
		            OutputStream output = new FileOutputStream(targetFile);

		            byte data[] = new byte[1024];
		            int count;
		            while ((count = input.read(data)) != -1) {
		                
		                output.write(data, 0, count);
		            }

		            output.flush();
		            output.close();
		            input.close();
					htmlDbHelper.updatePage(url, filename, tweetId, forced, tries + 1);
					Log.d(TAG, "file download finished");
				} catch (NotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			return null;
		}
		
	}
	*/
	
	private class GetPagesTask extends AsyncTask<Void,Void,Void>{
		
        boolean forced;
		
		public GetPagesTask(boolean forced) {
			this.forced = forced;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub			
			//download unsuccessfully downloaded pages			
			cleanupMess();
			c = htmlDbHelper.getUndownloadedHtmls(forced);	
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();				
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(Void param) {		
			storePage();			

		}



	}


	private void downloadPages(boolean forced){
		
		setRecentDownloadedTime(System.currentTimeMillis(), getBaseContext());	
		new GetPagesTask(forced).execute();	
		
	}
	
	public void storePage() {
		
		if (c != null && c.getCount() > 0 && (!c.isAfterLast())) {
			
			String htmlUrl = c.getString(c.getColumnIndex(HtmlPage.COL_URL));
		
			String[] filePath = {HtmlPage.HTML_PATH + "/" + LoginActivity.getTwitterId(getApplicationContext())};

			if(sdCardHelper.checkSDState(filePath)){

				Cursor cursorInfo = htmlDbHelper.getPageInfo(htmlUrl);

				switch(sdCardHelper.checkFileType(htmlUrl)){
				case SDCardHelper.TYPE_XML:							
					
					
					webDownload(cursorInfo);
					break;
				/*
				case SDCardHelper.TYPE_PDF:

					processFiles(htmlCV, "pdf");
					break;

				case SDCardHelper.TYPE_JPG:
					processFiles(htmlCV, "jpg");
					break;

				case SDCardHelper.TYPE_PNG:
					processFiles(htmlCV, "png");
					break;	

				case SDCardHelper.TYPE_GIF:
					processFiles(htmlCV, "gif");
					break;

				case SDCardHelper.TYPE_MP3:
					processFiles(htmlCV, "mp3");
					break;

				case SDCardHelper.TYPE_FLV:
					processFiles(htmlCV, "flv");
					break;		

				case SDCardHelper.TYPE_RMVB:
					processFiles(htmlCV, "rmvb");
					break;

				case SDCardHelper.TYPE_MP4:
					processFiles(htmlCV, "mp4");
					break;
				*/
				default:
					break;

				}

			}
			c.moveToNext();
			
		} else if (c != null)
			c.close();
		
		
	}


/*
	private void processFiles(, String fileSuffix){
		Log.i(TAG, "file type: " + fileSuffix);
		int len = fileSuffix.length();
		String filename = fileCV.getAsString(HtmlPage.COL_FILENAME).
				substring(0, fileCV.getAsString(HtmlPage.COL_FILENAME).length()-len-1) + "." + fileSuffix;
		
		fileCV.put(HtmlPage.COL_FILENAME, filename);
		
		htmlDbHelper.updatePage(fileCV.getAsString(HtmlPage.COL_URL), 
				 filename, 
				 fileCV.getAsLong(HtmlPage.COL_TID), 
				 fileCV.getAsInteger(HtmlPage.COL_DOWNLOADED), fileCV.getAsInteger(HtmlPage.COL_FORCED), 
				 fileCV.getAsInteger(HtmlPage.COL_ATTEMPTS));
		new fileDownload().execute(fileCV);
	}
	
	*/
	
	/**
	 * correct download errors caused by network interrupt
	 */
	private void cleanupMess(){
		
		Cursor c = htmlDbHelper.getDownloadedHtmls();
		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
		{   
			if (!c.isNull(c.getColumnIndex(HtmlPage.COL_FILENAME))) {
				
				String htmlUrl = c.getString(c.getColumnIndex(HtmlPage.COL_URL));
				Long tweetId = c.getLong(c.getColumnIndex(HtmlPage.COL_DISASTERID));
				String filename = c.getString(c.getColumnIndex(HtmlPage.COL_FILENAME));
				int forced = c.getInt(c.getColumnIndex(HtmlPage.COL_FORCED));
				int tries = c.getInt(c.getColumnIndex(HtmlPage.COL_ATTEMPTS));	
				
				String[] filePath = {HtmlPage.HTML_PATH + "/" + LoginActivity.getTwitterId(getApplicationContext())};
				switch(sdCardHelper.checkFileType(htmlUrl)){
					case SDCardHelper.TYPE_XML:
						if(sdCardHelper.checkSDState(filePath)){					
		
							File htmlPage = sdCardHelper.getFileFromSDCard(filePath[0], filename);
		
							if(!htmlPage.exists() || (sdCardHelper.getFileFromSDCard(filePath[0], filename).length() <= 1) ){
								sdCardHelper.deleteFile(sdCardHelper.getFileFromSDCard(filePath[0], filename).getPath());
								htmlDbHelper.updatePage(htmlUrl, null, tweetId, forced, tries);
							}	
						}
						break;	
					default:
						break;
				}
				
			}
			
		}
	}


	private boolean webDownload(Cursor cursorHtml){
		boolean result = true;
		
		WebView web = new WebView(getBaseContext());
		// TODO Auto-generated method stub				
		web.setWebViewClient(new WebClientDownload( cursorHtml));			
		web.getSettings().setJavaScriptEnabled(true);
		web.getSettings().setDomStorageEnabled(true);		
		web.loadUrl(cursorHtml.getString(cursorHtml.getColumnIndex(HtmlPage.COL_URL)));
		
		return result;
	}
	

	

	/**
	 * store the id for the tweets of which html pages have been downloaded
	 * @param sinceId
	 * @param context
	 */
	public static void setRecentDownloadedTime(long sinceTime, Context context) {		
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putLong(DOWNLOAD_SINCE_TIME , sinceTime);
		prefEditor.commit();
	}	
	
	/**
	 * get the last timetamp for performing bulkdownload action
	 * @param context
	 * @return
	 */
	public static long getLastDownloadedTime(Context context) {
		
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Long lastTime = prefs.getLong(DOWNLOAD_SINCE_TIME,Long.valueOf(0));	
		return lastTime;
		
	}
	
	
	

	//webview only to download webarchive
	@SuppressLint("NewApi")
	private class WebClientDownload extends WebViewClient {		
		private String baseUrl;
		private Long tweetId;		
		private int forced;
		private int attempts;
		private boolean loadingFailed;
		private String basePath = HtmlPage.HTML_PATH + "/" + LoginActivity.getTwitterId(HtmlService.this);

		private class WebArchiveCallback implements ValueCallback<String> {

			@Override
			public void onReceiveValue(String filePath) {				
				if (filePath == null) {
					htmlDbHelper.updatePage(baseUrl, null, tweetId, forced, attempts);				   
				}
				else {							
					
					String[] pathSlices = filePath.split("/");
					String filename = pathSlices[pathSlices.length-1];					
					if (sdCardHelper.getFileFromSDCard(basePath, filename).length() > 1) {						

						try {
							htmlDbHelper.updatePage(baseUrl, filename, tweetId, forced, attempts);
							getContentResolver().notifyChange(Tweets.TABLE_TIMELINE_URI, null);			
						} catch (SQLException ex){
							Log.i(TAG,"error updating page: ", ex);
						}
						
					} 
				}				
				storePage();
			}
		}

		public WebClientDownload(Cursor cursorHtml){

			this.baseUrl = cursorHtml.getString(cursorHtml.getColumnIndex(HtmlPage.COL_URL));
			this.tweetId = cursorHtml.getLong(cursorHtml.getColumnIndex(HtmlPage.COL_DISASTERID));	
			this.forced = cursorHtml.getInt(cursorHtml.getColumnIndex(HtmlPage.COL_FORCED));
			this.attempts = cursorHtml.getInt(cursorHtml.getColumnIndex(HtmlPage.COL_ATTEMPTS)) + 1;
			this.loadingFailed = false;			
		}
		
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			// TODO Auto-generated method stub
			//htmlDbHelper.updatePage(baseUrl, null, tweetId, forced, attempts);
			Log.d(TAG, "on page started");			
			super.onPageStarted(view, url, favicon);
		}


		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			// TODO Auto-generated method stub
			Log.d(TAG, "on received error" + failingUrl);
			htmlDbHelper.updatePage(baseUrl, null, tweetId, forced, attempts);
			loadingFailed = true;
			super.onReceivedError(view, errorCode, description, failingUrl);
		}


		@Override
		public void onReceivedSslError(WebView view, SslErrorHandler handler,
				SslError error) {
			// TODO Auto-generated method stub
			Log.d(TAG, "on received ssl error");
			htmlDbHelper.updatePage(baseUrl, null, tweetId, forced, attempts);
			loadingFailed = true;
			super.onReceivedSslError(view, handler, error);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			// TODO Auto-generated method stub
			if(!loadingFailed){			
				String filename = "twimight" + String.valueOf(System.currentTimeMillis()) + ".xml";		

				//if (sdCardHelper.getFileFromSDCard(basePath, filename).createNewFile())
				view.saveWebArchive(sdCardHelper.getFileFromSDCard(basePath, filename).getPath(), false , new WebArchiveCallback() );	

			}

		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// TODO Auto-generated method stub
			view.loadUrl(url);
			Log.d(TAG, baseUrl + " redirect to:" + url);
			return true;
		}	

	}

}
