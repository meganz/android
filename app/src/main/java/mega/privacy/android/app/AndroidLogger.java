package mega.privacy.android.app;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaLoggerInterface;
import android.os.Environment;
import android.util.Log;


public class AndroidLogger implements MegaLoggerInterface {

	public void log(String time, int loglevel, String source, String message) {

		if (Util.DEBUG){
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String currentDateandTime = sdf.format(new Date());

				message = "(" + currentDateandTime + ") - " + message;
			}
			catch (Exception e){}

			String sourceMessage = "";
			if (source != null) {
				String[] s = source.split("jni/mega");
				if (s != null) {
					if (s.length > 1) {
						sourceMessage = s[1] + "";
					} else {
						sourceMessage = source + "";
					}
				}
			}

			Log.d("AndroidLogger", sourceMessage + ": " + message);
//			addRecordToLog("AndroidLogger: " + source + ": " + message);
		}

		File logFile=null;
		boolean fileLogger = Util.getFileLogger();
		if (fileLogger) {
			//Send the log to a file

			String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.logDIR + "/";
			//			String file = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+logDIR+"/log.txt";
			File dirFile = new File(dir);
			if (!dirFile.exists()) {
				dirFile.mkdirs();
				logFile = new File(dirFile, "log.txt");
				if (!logFile.exists()) {
					try {
						logFile.createNewFile();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else {
				logFile = new File(dirFile, "log.txt");
				if (!logFile.exists()) {
					try {
						logFile.createNewFile();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			if (logFile != null && logFile.exists()) {
				String sourceMessage = "";
				if (source != null) {
					String[] s = source.split("jni/mega");
					if (s != null) {
						if (s.length > 1) {
							sourceMessage = s[1] + "";
						} else {
							sourceMessage = source + "";
						}
					}
				}

				Util.appendStringToFile(sourceMessage + ": " + message + "\n", logFile);
			}
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
