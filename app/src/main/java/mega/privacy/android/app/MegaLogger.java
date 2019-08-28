package mega.privacy.android.app;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import static mega.privacy.android.app.utils.FileUtils.*;

/**
 * Used to display console log from app, SDK and chatSDK,
 * and also save logs to corresponding log file
 */

public abstract class MegaLogger {
    protected File logFile;
    protected String dir, fileName;
    
    public MegaLogger(String fileName,boolean fileLogger) {
        logFile = null;
        dir = getExternalStoragePath(LOG_DIR);
        this.fileName = fileName;
        logToFile();
    }
    
    protected boolean isReadyToWriteToFile(boolean enabled) {
        if (enabled) {
            if (logFile == null || !logFile.exists()) {
                File dirFile = new File(dir);
                if (!dirFile.exists()) {
                    dirFile.mkdirs();
                }
                
                logFile = new File(dirFile,fileName);
                if (!logFile.exists()) {
                    try {
                        logFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return true;
        }
        return false;
    }

    protected void writeToFile(String appendContents) {
        try {
            if (logFile != null && logFile.canWrite()) {
                logFile.createNewFile(); // ok if returns false, overwrite
                Writer out = new BufferedWriter(new FileWriter(logFile,true),256);
                out.write(appendContents);
                out.close();
            }
        } catch (IOException e) {
            Log.e("Mega Logger","Error appending string data to file " + e.getMessage(),e);
        }
    }

    protected abstract void logToFile();
}
