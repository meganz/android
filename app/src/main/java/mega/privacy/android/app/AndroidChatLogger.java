package mega.privacy.android.app;

import android.util.Log;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatLoggerInterface;


public class AndroidChatLogger extends MegaLogger implements MegaChatLoggerInterface{
    public static final String LOG_FILE_NAME = "logKarere.txt";
    private final String TAG = "AndroidChatLogger";

    public AndroidChatLogger(String fileName, boolean fileLogger) {
        super(fileName, fileLogger);
    }

    @Override
    public void log(int logLevel, String message) {
        //display to console
        if (Util.DEBUG) {
            Log.d(TAG,  createMessage(message));
        }
        //save to log file
        if (isReadyToWriteToFile(Util.getFileLoggerKarere())) {
            fileLogQueue.add(createMessage(message));
        }
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
