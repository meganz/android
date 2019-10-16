package mega.privacy.android.app;

import android.util.Log;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatLoggerInterface;

public class AndroidChatLogger extends MegaLogger implements MegaChatLoggerInterface {
    public static final String LOG_FILE_NAME = "logKarere.txt";

    public AndroidChatLogger(String fileName, boolean fileLogger) {
        super(fileName, fileLogger);
    }

    public void log(int logLevel, String message) {
        //display to console
        if (Util.DEBUG) {
            String TAG = "AndroidLogger";
            Log.d(TAG, message);
        }

        //save to log file
        if (isReadyToWriteToFile(Util.getFileLoggerKarere())) {
            fileLogQueue.add(message);
        }
    }
}
