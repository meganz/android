/*
 * Created by Yuan Gao on 27/07/18.
 * Copyright (c) 2018 mega.co.nz
 */
package mega.privacy.android.app;


import java.util.concurrent.ConcurrentLinkedDeque;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaLoggerInterface;


public class AndroidLogger extends MegaLogger implements MegaLoggerInterface{

    public static final String LOG_FILE_NAME = "logSDK.txt";
    private static ConcurrentLinkedDeque<String> fileLogQueue;

    public AndroidLogger(String fileName, boolean fileLogger) {
        super(fileName, fileLogger);
        fileLogQueue = new ConcurrentLinkedDeque<>();
    }

    @Override
    public void log(String time, int logLevel, String source, String message) {
        //display to console
        if (Util.DEBUG) {
            logQueue.add("AndroidLogger" + separator + createSourceMessage(message) + ": " + createMessage(message));
        }

        //save to log file
        if (isReadyToWriteToFile(Util.getFileLoggerSDK())) {
            fileLogQueue.add(createSourceMessage(message) + ": " + createMessage(message) + "\n");
        }
    }

    //create SDK specific log prefix
    private String createSourceMessage(String source) {
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

        return sourceMessage;
    }

    //save logs to file in new thread
    @Override
    protected void logToFile(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String log = fileLogQueue.pollFirst();
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
