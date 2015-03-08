package ch.ethz.twimight.util;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import ch.ethz.bluetest.credentials.Obfuscator;
import ch.ethz.twimight.activities.LoginActivity;

import com.crittercism.app.Crittercism;

public class LogCollector {
	
	
public static void setUpCrittercism(Context context) {
		
		JSONObject parameters =  setUpParams(context);
		
		if (parameters != null)
			Crittercism.init(context, Obfuscator.getCrittercismId(), parameters);
		else
			Crittercism.init(context, Obfuscator.getCrittercismId() );		
		
		//binding twitter id to user screenName and sending it to the log server
		if (LoginActivity.hasTwitterId(context)) {
			
			//Crittercism.setUsername(LoginActivity.getTwitterId(context));
			
			// instantiate metadata json object
			JSONObject metadata = new JSONObject();
			// add arbitrary metadata
			try {
				metadata.put("user_id", LoginActivity.getTwitterId(context));
				metadata.put("screen_name", LoginActivity.getTwitterScreenname(context));
				// send metadata to crittercism (asynchronously)
				Crittercism.setMetadata(metadata);
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
			
		}
		
	}

	public static void leaveBreadcrumb() {
		// Did the user get here before crashing?
	    String breadcrumb = "My Breadcrumb";
		Crittercism.leaveBreadcrumb(breadcrumb);
	}
	
	public static void logException(Exception exception) {
		Crittercism.logHandledException(exception);
	}
	
	private static JSONObject setUpParams(Context context) {
		PackageInfo pInfo;
		String version=null;
		
		try {			
			pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			version = pInfo.versionName;
			
		} catch (NameNotFoundException e) {}
		
		return getJSON(version);
	}
	
	private static JSONObject getJSON(String version) {
		// create the JSONObject.  (Do not forget to import org.json.JSONObject!)
		JSONObject crittercismConfig = new JSONObject();		
		try
		{
		    crittercismConfig.put("customVersionName", version);
		    crittercismConfig.put("shouldCollectLogcat", true);
		}
		catch (JSONException je){ return null;}
		return crittercismConfig;
	}
	
}
