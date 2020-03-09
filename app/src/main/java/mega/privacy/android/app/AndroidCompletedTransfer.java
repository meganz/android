package mega.privacy.android.app;


import java.io.File;

import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaTransfer;

import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class AndroidCompletedTransfer {

    private String fileName;
    private int type;
    private int state;
    private String size;
    private String nodeHandle;
    private String path;
    private boolean isOfflineFile;

    public AndroidCompletedTransfer(String fileName, int type, int state, String size, String nodeHandle, String path, boolean isOfflineFile) {
        this.fileName = fileName;
        this.type = type;
        this.state = state;
        this.size = size;
        this.nodeHandle = nodeHandle;
        this.path = removeLastFileSeparator(path);
        this.isOfflineFile = isOfflineFile;
    }

    public AndroidCompletedTransfer (MegaTransfer transfer) {
        this.fileName = transfer.getFileName();
        this.type = transfer.getType();
        this.state = transfer.getState();
        this.size = getSizeString(transfer.getTotalBytes());
        this.nodeHandle = transfer.getNodeHandle() + "";
        this.path = getTransferPath(transfer);
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
