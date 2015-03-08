package ch.ethz.twimight.net.Html;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import ch.ethz.twimight.activities.LoginActivity;

public class StartServiceHelper {
	
	private static void startServiceWifi(Context context) {
		Intent i = new Intent(context, HtmlService.class);
		i.putExtra(HtmlService.DOWNLOAD_REQUEST, HtmlService.DOWNLOAD_ALL);		
		context.startService(i);
	}
	
    private static void startServiceUmts(Context context) {
    	Intent i = new Intent(context, HtmlService.class);
		i.putExtra(HtmlService.DOWNLOAD_REQUEST, HtmlService.DOWNLOAD_ONLY_FORCED);
		context.startService(i);
    }


    public static void startService( Context context){

    	ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo currentNetworkInfo = cm.getActiveNetworkInfo();

    	// are we connected and logged in?
    	if( currentNetworkInfo != null && currentNetworkInfo.isConnected() && LoginActivity.hasAccessToken(context) && LoginActivity.hasAccessTokenSecret(context)){
    		
    		int networkType = currentNetworkInfo.getType();		

    		if(networkType == ConnectivityManager.TYPE_WIFI){			
    			StartServiceHelper.startServiceWifi(context);
    			//start html service
    		}
    		else if(networkType == ConnectivityManager.TYPE_MOBILE){			
    			StartServiceHelper.startServiceUmts(context);

    		}



    	}

    }


}
