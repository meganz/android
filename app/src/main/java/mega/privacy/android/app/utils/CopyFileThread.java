package mega.privacy.android.app.utils;

import java.io.File;

import static mega.privacy.android.app.utils.FileUtils.copyFile;
import static mega.privacy.android.app.utils.LogUtil.*;

public class CopyFileThread implements Runnable {

    private boolean copyToSDCard;

    private String path;

    private String targetPath;

    private String fileName;

    private SDCardOperator operator;

    public CopyFileThread(boolean copyToSDCard, String path, String targetPath, String fileName, SDCardOperator operator) {
        this.copyToSDCard = copyToSDCard;
        this.path = path;
        this.targetPath = targetPath;
        this.fileName = fileName;
        this.operator = operator;
    }

    @Override
    public void run() {
        logDebug("Call to copyFile");
        try {
            if (copyToSDCard) {
                operator.moveFile(targetPath, new File(path));
            } else {
                copyFile(new File(path), new File(targetPath, fileName));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logError("Copy file error", e);
        }
    }
}
