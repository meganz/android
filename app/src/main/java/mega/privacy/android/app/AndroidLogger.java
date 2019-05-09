package mega.privacy.android.app;


import android.util.Log;

import java.util.concurrent.ConcurrentLinkedDeque;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaLoggerInterface;


public class AndroidLogger extends MegaLogger implements MegaLoggerInterface {
    
    public static final String LOG_FILE_NAME = "logSDK.txt";
    private final String TAG = "AndroidLogger";
    private ConcurrentLinkedDeque<String> sdkFileLogQueue;
    
    public AndroidLogger(String fileName,boolean fileLogger) {
        super(fileName,fileLogger);
    }

    @Override
    public void log(String time,int logLevel,String source,String message) {
        //display to console
        if (Util.DEBUG) {
            Log.d(TAG,createMessage(source,time,message));
        }

        //save to log file
        if (isReadyToWriteToFile(Util.getFileLoggerSDK())) {
            sdkFileLogQueue.add(createMessage(source,time,message));
        }
    }
    
    //create SDK specific log message
    private String createMessage(String source,String time,String message) {
        String sourceMessage = "";
        if (source != null) {
            String[] s = source.split("jni/mega");
            if (s != null) {
                if (s.length > 1) {
                    sourceMessage = s[1];
                } else {
                    sourceMessage = source;
                }
            }
        }
        
        sourceMessage = "[" + time + "] - " + sourceMessage + " - " + message + "\n";
        
        return sourceMessage;
    }

    //save logs to file in new thread
    @Override
    protected void logToFile() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (sdkFileLogQueue == null) {
                        sdkFileLogQueue = new ConcurrentLinkedDeque<>();
                    }
                    
                    String log = sdkFileLogQueue.pollFirst();
                    if (log != null) {
                        writeToFile(log);
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        thread.start();
    }
}
