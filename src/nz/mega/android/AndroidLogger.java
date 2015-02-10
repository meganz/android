package nz.mega.android;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaLoggerInterface;
import android.os.Environment;
import android.util.Log;


public class AndroidLogger implements MegaLoggerInterface {

	public void log(String time, int loglevel, String source, String message) {
		if (Util.DEBUG){
			Log.d("AndroidLogger", source + ": " + message);
//			addRecordToLog("AndroidLogger" + source + ": " + message);
		}
	}
	 
	public static void addRecordToLog(String message) {
		 
		File logDir;
		if (Environment.getExternalStorageDirectory() != null){
			logDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.logDIR + "/");
			logDir.mkdirs();
			
			File logFile = new File (logDir, "log.txt");
			if (!logFile.exists()){
				try  {
                    logFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
			}
			
			try { 
				BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
				buf.write(message + "\r\n");
				buf.newLine();
                buf.flush();
                buf.close();
			}
			catch (IOException e) {
                e.printStackTrace();
            }
			
		}
	}
}
