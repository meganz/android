package mega.privacy.android.app;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatLoggerInterface;


public class AndroidChatLogger extends AndroidLogger implements MegaChatLoggerInterface {
    public static final String LOG_FILE_NAME = "logKarere.txt";
    public AndroidChatLogger(String fileName, boolean fileLogger) {
        super(fileName, fileLogger);
    }

    @Override
    public synchronized void log(int logLevel, String message) {
        //display to console
        if (Util.DEBUG) {
            logQueue.add("AndroidChatLogger" + separator + createMessage(message));
        }

        //save to log file
        if (logFile != null && logFile.exists()) {
            Util.appendStringToFile(message, logFile);
        }
    }
}
