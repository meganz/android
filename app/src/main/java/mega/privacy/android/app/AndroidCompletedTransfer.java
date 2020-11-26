package mega.privacy.android.app;


import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

import mega.privacy.android.app.objects.SDTransfer;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaTransfer;

import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.SDCardUtils.getSDCardTargetPath;
import static mega.privacy.android.app.utils.StringResourcesUtils.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class AndroidCompletedTransfer implements Parcelable {

    private long id;
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

    public AndroidCompletedTransfer(long id, String fileName, int type, int state, String size,
                                    String nodeHandle, String path, boolean isOfflineFile,
                                    long timeStamp, String error, String originalPath,
                                    long parentHandle) {
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
        this.error = getTranslatedErrorString(error);
        this.originalPath = transfer.getPath();
        this.parentHandle = transfer.getParentHandle();
    }

    public AndroidCompletedTransfer (SDTransfer transfer) {
        this.fileName = transfer.getName();
        this.type = MegaTransfer.TYPE_DOWNLOAD;
        this.state = MegaTransfer.STATE_COMPLETED;
        this.size = transfer.getSize();
        this.nodeHandle = transfer.getNodeHandle();
        this.path = removeLastFileSeparator(getSDCardTargetPath(transfer.getAppData()));
        this.timeStamp = System.currentTimeMillis();
        this.error = getString(R.string.api_ok);
        this.originalPath = transfer.getPath();
        this.parentHandle = INVALID_HANDLE;
        setIsOfflineFile(false);
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    /**
     * Remove the last character of the path if it is a file separator.
     *
     * @param path  path of a file.
     * @return The path without the last item if it is a file separator.
     */
    private String removeLastFileSeparator(String path) {
        if (!isTextEmpty(path) && path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    /**
     * Gets the path of a transfer.
     *
     * @param transfer  MegaTransfer from which the path has to be obtained
     * @return  The path of the transfer.
     */
    private String getTransferPath(MegaTransfer transfer) {
        MegaApplication app = MegaApplication.getInstance();

        switch (type) {
            case MegaTransfer.TYPE_UPLOAD:
                setIsOfflineFile(false);
                MegaNode parentNode = app.getMegaApi().getNodeByHandle(transfer.getParentHandle());

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

    protected AndroidCompletedTransfer(Parcel in) {
        id = in.readLong();
        fileName = in.readString();
        type = in.readInt();
        state = in.readInt();
        size = in.readString();
        nodeHandle = in.readString();
        path = in.readString();
        isOfflineFile = in.readByte() != 0;
        timeStamp = in.readLong();
        error = in.readString();
        originalPath = in.readString();
        parentHandle = in.readLong();
    }

    public static final Creator<AndroidCompletedTransfer> CREATOR = new Creator<AndroidCompletedTransfer>() {
        @Override
        public AndroidCompletedTransfer createFromParcel(Parcel in) {
            return new AndroidCompletedTransfer(in);
        }

        @Override
        public AndroidCompletedTransfer[] newArray(int size) {
            return new AndroidCompletedTransfer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(fileName);
        dest.writeInt(type);
        dest.writeInt(state);
        dest.writeString(size);
        dest.writeString(nodeHandle);
        dest.writeString(path);
        dest.writeByte((byte) (isOfflineFile ? 1 : 0));
        dest.writeLong(timeStamp);
        dest.writeString(error);
        dest.writeString(originalPath);
        dest.writeLong(parentHandle);
    }
}
