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

import java.io.File;
import java.io.FileWriter;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

public class LogFilesOperations {
	private static final String TAG = "LogFilesOperations";
	 File logFile;	 
	 File root;
	 String path;
	
	 boolean getSdState() {
		 String state = Environment.getExternalStorageState(); //get state of the SD card
	     if (Environment.MEDIA_MOUNTED.equals(state)) {
	    	 // We can read and write the media     	 
	    	 return true;
	     }
	     else
	    	 return false;
	 }
	 
	  public void createLogsFolder() {
		 if (getSdState()) { 
			 try {
				 root =  Environment.getExternalStorageDirectory();       	        		
	 			 path = root.getPath()+"/logs/"; 	            	 
	 			 // Test if the path exists      	            	
	 			 boolean exists = (new File(path).exists());
	 			 // If not, create dirs
					 if (!exists) 
						 new File(path).mkdirs();					 			 
			 }
	 		 catch (Exception ex) {
	 			 Log.e(TAG, "error: ", ex);      	        		 
	 		 }
		 }
	 }
	  public FileWriter createLogFile(String name) {
		  FileWriter logWriter = null;
		  // log file     
	      if (getSdState()) {     	
	     	 try {     		
				String date = new Date().toString();				
				date = date.substring(4,(date.indexOf(":") - 3));				
				date = date.replace(" ", "_");
				//date = date.replace(":", "-");				
				logFile = new File(path, "log_" + name + "_" + date + ".txt");
				if (!logFile.exists()) {
				    logFile.createNewFile();			
					logWriter =new FileWriter(logFile);	
				}
				else
					logWriter = new FileWriter(logFile,true);
				return logWriter;			
	     	 }
	     	 catch (Exception ex) {
	     		 Log.e(TAG, "error: ", ex); 
	     		 return null;
	     	 }
	      } 
	      else return null;
	}
}
