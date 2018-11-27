package mega.privacy.android.app.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Logger for testing.
 */
public class TL {

    private final static boolean OUTPUT = true;

    private static final String LOG_FILE = Environment.getExternalStorageDirectory() + File.separator +
            "test_log.txt";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd HH:mm:ss.SSS");

    public static void log(Object context,Object any) {
        String msg = (any == null) ? "NULL" : any.toString();
        String dateStr = DATE_FORMAT.format(new Date());
        if (context != null) {
            if (context instanceof String) {
                msg = "[" + dateStr + "] " + context + "--->" + msg;
            } else {
                msg = "[" + dateStr + "] " + context.getClass().getSimpleName() + "--->" + msg;
            }
        }
        if (OUTPUT) {
            try {
                File log = new File(LOG_FILE);
                if (!log.exists()){
                    log.createNewFile();
                }
                FileWriter writer = new FileWriter(LOG_FILE,true);
                writer.write(msg + "\n");
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.e("@#@",msg);
    }
}
