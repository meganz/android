package mega.privacy.android.app;

import android.util.Log;

import mega.privacy.android.app.logging.LegacyLogUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatLoggerInterface;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AndroidChatLogger extends MegaLogger implements MegaChatLoggerInterface {
    public static final String LOG_FILE_NAME = "logKarere.txt";
    private LegacyLogUtil legacyLogUtil;

    public AndroidChatLogger(@Nullable String fileName, @NotNull LegacyLogUtil legacyLogUtil) {
        super(fileName);
        this.legacyLogUtil = legacyLogUtil;
    }

    public void log(int logLevel, String message) {
        //display to console
        if (Util.DEBUG) {
            String TAG = "AndroidLogger";
            Log.d(TAG, message);
        }

        //save to log file
        if (isReadyToWriteToFile(legacyLogUtil.getStatusLoggerKarere())) {
            fileLogQueue.add(message);
        }
    }
}
