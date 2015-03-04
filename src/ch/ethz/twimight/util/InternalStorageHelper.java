package ch.ethz.twimight.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.util.Log;

public class InternalStorageHelper {
	
	Context context;
	private static final String TAG = "InternalStorageHelper";
	
	public InternalStorageHelper(Context context) {
		this.context=context;
	}
	
	public boolean writeImage(byte[] image, String filename) {
		FileOutputStream out = null ;
		try {				
			out = context.openFileOutput(filename, Context.MODE_PRIVATE);
			out.write(image);			
			return true;
			
		} catch (FileNotFoundException e) {
			return false;
			
		} catch (IOException e) {
			return false;
		} 
		finally {
			try {
				if (out != null)
					out.close();
			} catch (IOException e) {}
		}
	}
	
	public boolean delete(String filename) {
		return context.deleteFile(filename);
		
	}
	
/*
	public byte[] readImage(String filename) {
		FileInputStream fis = null;
		try {
			fis = context.openFileInput(filename);
			File file =  new File(context.getFilesDir(),filename);
			int len = (int)file.length();
			byte[] buf = new byte[len];
			fis.read(buf);
			return buf;
			//BufferedInputStream bis = new BufferedInputStream(fis);
			//ByteArrayBuffer baf = new ByteArrayBuffer(2048);	
			//get the bytes one by one			
			/*int current = 0;			
			while ((current = bis.read()) != -1) {			
			        baf.append((byte) current);			
			}	
			return baf.toByteArray();
			
		} catch (FileNotFoundException e) {			
			Log.e(TAG,"file not found exception" );
			return null;
			
		} catch (IOException e) {
			return null;
		}
		finally{
			
			try {
				if (fis != null)
					fis.close();
			} catch (IOException e) {}
		}
	}
	
	*/
	
	
	    		
			
		
		
}
