package mega.privacy.android.app;


import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaLoggerInterface;


public class AndroidLogger implements MegaLoggerInterface {
    public static final String LOG_FILE_NAME = "logSDK.txt";
    protected static ConcurrentLinkedDeque<String> logQueue;
    protected static String separator = "&&";
    protected SimpleDateFormat simpleDateFormat;
    protected File logFile;
    protected String dir;

    public AndroidLogger(String fileName, boolean fileLogger) {
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        logFile = null;
        dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.logDIR + "/";
        logQueue = new ConcurrentLinkedDeque<>();

        if (fileLogger) {

            //check if log file exist, create one if not
            File dirFile = new File(dir);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }

            logFile = new File(dirFile, fileName);
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        startAsyncLogger();
    }

    @Override
    public synchronized void log(String time, int logLevel, String source, String message) {
        //display to console
        if (Util.DEBUG) {
            logQueue.add("AndroidLogger" + separator + createSourceMessage(message) + ": " + createMessage(message));
        }

        //save to log file
        if (logFile != null && logFile.exists()) {
            Util.appendStringToFile(createSourceMessage(message) + ": " + createMessage(message) + "\n", logFile);
        }
    }

    protected String createMessage(String message) {
        String currentDateAndTime = simpleDateFormat.format(new Date());
        message = "(" + currentDateAndTime + ") - " + message;
        return message;
    }

    protected String createSourceMessage(String source) {
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

    protected synchronized static void startAsyncLogger() {
        AsyncTask<Void, Void, Void> myTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                while (true) {
                    String log = logQueue.pollFirst();
                    if (log != null) {
                        String[] combined = log.split(separator);
                        Log.d(combined[0], combined[1]);
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        myTask.execute();
    }
}
