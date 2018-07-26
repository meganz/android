package mega.privacy.android.app;

import java.util.concurrent.ConcurrentLinkedDeque;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatLoggerInterface;


public class AndroidChatLogger extends MegaLogger implements MegaChatLoggerInterface{
    public static final String LOG_FILE_NAME = "logKarere.txt";
    protected static ConcurrentLinkedDeque<String> chatFileLogQueue;

    public AndroidChatLogger(String fileName, boolean fileLogger) {
        super(fileName, fileLogger);
        chatFileLogQueue = new ConcurrentLinkedDeque<>();
        logToFile();
    }

    @Override
    public void log(int logLevel, String message) {
        //display to console
        if (Util.DEBUG) {
            logQueue.add("AndroidChatLogger" + separator + createMessage(message));
        }

        //save to log file
        if (isReadyToWriteToFile(Util.getFileLoggerKarere())) {
            chatFileLogQueue.add(createMessage(message));
        }
    }

    @Override
    protected void logToFile(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String log = chatFileLogQueue.pollFirst();
                    if (log != null) {
                        writeToFile(log);
                    } else {
                        try {
                            Thread.sleep(1000);
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
