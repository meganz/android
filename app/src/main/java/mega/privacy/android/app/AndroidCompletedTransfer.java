package mega.privacy.android.app;


import java.io.File;

import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaTransfer;

import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class AndroidCompletedTransfer {

    private int id;
    private String fileName;
    private int type;
    private int state;
    private String size;
    private String nodeHandle;
    private String path;
    private boolean isOfflineFile;
    private long timeStamp;
    private String error;
    private String originalPath;
    private long parentHandle;

    public AndroidCompletedTransfer(int id, String fileName, int type, int state, String size, String nodeHandle, String path, boolean isOfflineFile, long timeStamp, String error, String originalPath, long parentHandle) {
        this.id = id;
        this.fileName = fileName;
        this.type = type;
        this.state = state;
        this.size = size;
        this.nodeHandle = nodeHandle;
        this.path = removeLastFileSeparator(path);
        this.isOfflineFile = isOfflineFile;
        this.timeStamp = timeStamp;
        this.error = error;
        this.originalPath = originalPath;
        this.parentHandle = parentHandle;
    }

    public AndroidCompletedTransfer (MegaTransfer transfer, MegaError error) {
        this.fileName = transfer.getFileName();
        this.type = transfer.getType();
        this.state = transfer.getState();
        this.size = getSizeString(transfer.getTotalBytes());
        this.nodeHandle = transfer.getNodeHandle() + "";
        this.path = getTransferPath(transfer);
        this.timeStamp = System.currentTimeMillis();
        this.error = MegaApiJava.getTranslatedErrorString(error);
        this.originalPath = transfer.getPath();
        this.parentHandle = transfer.getParentHandle();
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getNodeHandle() {
        return nodeHandle;
    }

    public void setNodeHandle(String nodeHandle) {
        this.nodeHandle = nodeHandle;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean getIsOfflineFile() {
        return isOfflineFile;
    }

    public void setIsOfflineFile(boolean isOfflineFile) {
        this.isOfflineFile = isOfflineFile;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public long getParentHandle() {
        return parentHandle;
    }

    public void setParentHandle(long parentHandle) {
        this.parentHandle = parentHandle;
    }

    private String removeLastFileSeparator(String path) {
        if (!isTextEmpty(path) && path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    private String getTransferPath(MegaTransfer transfer) {
        MegaApplication app = MegaApplication.getInstance();
        MegaApiAndroid megaApi = app.getMegaApi();

        switch (type) {
            case MegaTransfer.TYPE_UPLOAD:
                setIsOfflineFile(false);
                MegaNode parentNode = megaApi.getNodeByHandle(transfer.getParentHandle());

                if (parentNode != null) {
                    return removeLastFileSeparator(getParentFolderPath(parentNode));
                }
                break;

            case MegaTransfer.TYPE_DOWNLOAD:
                String path = transfer.getParentPath();
                File offlineFolder = getOfflineFolder(app, OFFLINE_DIR);
                setIsOfflineFile(!isTextEmpty(path) && offlineFolder != null && path.startsWith(offlineFolder.getAbsolutePath()));

                if (isOfflineFile) {
                    path = removeInitialOfflinePath(path, transfer.getNodeHandle());
                }
                return removeLastFileSeparator(path);
        }

        setIsOfflineFile(false);
        return "";
    }
}
