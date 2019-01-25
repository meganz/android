package mega.privacy.android.app.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TL {

    private final static boolean OUTPUT = false;

    private static final String LOG_FILE = Environment.getExternalStorageDirectory() + File.separator + "tl_log.txt";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss");

    private static final File log = new File(LOG_FILE);

    public static void log(Object context,String tag,Object any) {

        String msg = (any == null) ? "NULL" : any.toString();
        String dateStr = DATE_FORMAT.format(new Date());
        if (context != null) {
            if (context instanceof String) {
                msg = "[" + dateStr + "] " + context + "--->" + msg;
            } else {
                msg = "[" + dateStr + "] " + context.getClass().getSimpleName() + "--->" + msg;
            }
        }
        Log.e(tag,msg);

        FileWriter writer;
        if (OUTPUT) {
            try {
                if (!log.exists()) {
                    log.createNewFile();
                }
                writer = new FileWriter(LOG_FILE,true);
                writer.write(msg + "\n");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
