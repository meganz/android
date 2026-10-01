package mega.privacy.android.data.model;

import nz.mega.sdk.MegaApiJava;
import timber.log.Timber;

public class MegaAttributes {

    private String askSizeDownload;
    private String showCopyright;
    private String lastPublicHandle;
    private String lastPublicHandleTimeStamp;
    private int lastPublicHandleType;
    private String myChatFilesFolderHandle;
    private String transferQueueStatus;

    public MegaAttributes(String askSizeDownload, String showCopyright,
                          String lastPublicHandle, String lastPublicHandleTimeStamp,
                          int lastPublicHandleType, String myChatFilesFolderHandle,
                          String transferQueueStatus) {
        this.askSizeDownload = askSizeDownload;
        this.showCopyright = showCopyright;
        this.lastPublicHandle = lastPublicHandle;
        this.lastPublicHandleTimeStamp = lastPublicHandleTimeStamp;
        this.lastPublicHandleType = lastPublicHandleType;
        this.myChatFilesFolderHandle = myChatFilesFolderHandle;
        this.transferQueueStatus = transferQueueStatus;
    }

    public String getAskSizeDownload() {
        return askSizeDownload;
    }

    public void setAskSizeDownload(String askSizeDownload) {
        this.askSizeDownload = askSizeDownload;
    }

    public String getShowCopyright() {
        return showCopyright;
    }

    public void setShowCopyright(String showCopyright) {
        this.showCopyright = showCopyright;
    }

    public long getLastPublicHandle() {
        return getLongValueFromStringAttribute(lastPublicHandle, "Last public handle", -1);
    }

    public void setLastPublicHandle(long lastPublicHandle) {
        this.lastPublicHandle = Long.toString(lastPublicHandle);
    }

    public long getLastPublicHandleTimeStamp() {
        return getLongValueFromStringAttribute(lastPublicHandleTimeStamp, "Last public handle time stamp", -1);
    }

    public void setLastPublicHandleTimeStamp(long lastPublicHandleTimeStamp) {
        this.lastPublicHandleTimeStamp = Long.toString(lastPublicHandleTimeStamp);
    }

    public int getLastPublicHandleType() {
        return lastPublicHandleType;
    }

    public void setLastPublicHandleType(int lastPublicHandleType) {
        this.lastPublicHandleType = lastPublicHandleType;
    }

    public long getMyChatFilesFolderHandle() {
        return getLongValueFromStringAttribute(myChatFilesFolderHandle, "My chat files folder handle", MegaApiJava.INVALID_HANDLE);
    }

    public void setMyChatFilesFolderHandle(long myChatFilesFolderHandle) {
        this.myChatFilesFolderHandle = Long.toString(myChatFilesFolderHandle);
    }

    public String getTransferQueueStatus() {
        return transferQueueStatus;
    }

    public void setTransferQueueStatus(String transferQueueStatus) {
        this.transferQueueStatus = transferQueueStatus;
    }

    /**
     * Get an attribute stored as String as a long value.
     *
     * @param attribute    Attribute to get value.
     * @param attrName     Descriptive name of the attribute.
     * @param defaultValue Default value to get in case of error.
     * @return Long value of the attribute.
     */
    private long getLongValueFromStringAttribute(String attribute, String attrName, long defaultValue) {
        long value = defaultValue;

        if (attribute != null) {
            try {
                value = Long.parseLong(attribute);
            } catch (Exception e) {
                Timber.w(e, "Exception getting %s.", attrName);
                value = defaultValue;
            }
        }

        Timber.d("%s: %d", attrName, value);
        return value;
    }
}
