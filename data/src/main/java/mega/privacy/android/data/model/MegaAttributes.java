package mega.privacy.android.data.model;

import androidx.annotation.NonNull;

import mega.privacy.android.domain.entity.StorageState;
import nz.mega.sdk.MegaApiJava;
import timber.log.Timber;

public class MegaAttributes {

    private String online;
    private int attempts;
    private String askSizeDownload;
    private String askNoAppDownload;
    private String accountDetailsTimeStamp;
    private String extendedAccountDetailsTimeStamp;
    private String invalidateSdkCache;
    private String useHttpsOnly;
    private String showCopyright;
    private String showNotifOff;
    private String lastPublicHandle;
    private String lastPublicHandleTimeStamp;
    private int lastPublicHandleType;
    private StorageState storageState;
    private String myChatFilesFolderHandle;
    private String transferQueueStatus;

    public MegaAttributes(String online, int attemps, String askSizeDownload, String askNoAppDownload,
                          String accountDetailsTimeStamp, String extendedAccountDetailsTimeStamp,
                          String invalidateSdkCache, String useHttpsOnly, String showCopyright,
                          String showNotifOff, String lastPublicHandle,
                          String lastPublicHandleTimeStamp, int lastPublicHandleType, @NonNull StorageState storageState,
                          String myChatFilesFolderHandle, String transferQueueStatus) {
        this.online = online;
        this.attempts = attemps;
        this.askNoAppDownload = askNoAppDownload;
        this.askSizeDownload = askSizeDownload;
        this.accountDetailsTimeStamp = accountDetailsTimeStamp;
        this.extendedAccountDetailsTimeStamp = extendedAccountDetailsTimeStamp;
        this.invalidateSdkCache = invalidateSdkCache;
        this.useHttpsOnly = useHttpsOnly;
        this.showCopyright = showCopyright;
        this.showNotifOff = showNotifOff;
        this.lastPublicHandle = lastPublicHandle;
        this.lastPublicHandleTimeStamp = lastPublicHandleTimeStamp;
        this.lastPublicHandleType = lastPublicHandleType;
        this.storageState = storageState;
        this.myChatFilesFolderHandle = myChatFilesFolderHandle;
        this.transferQueueStatus = transferQueueStatus;
    }

    public String getOnline() {
        return online;
    }

    public void setOnline(String online) {
        this.online = online;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attemps) {
        this.attempts = attemps;
    }

    public String getAskSizeDownload() {
        return askSizeDownload;
    }

    public void setAskSizeDownload(String askSizeDownload) {
        this.askSizeDownload = askSizeDownload;
    }

    public String getAskNoAppDownload() {
        return askNoAppDownload;
    }

    public void setAskNoAppDownload(String askNoAppDownload) {
        this.askNoAppDownload = askNoAppDownload;
    }

    public String getAccountDetailsTimeStamp() {
        return accountDetailsTimeStamp;
    }

    public void setAccountDetailsTimeStamp(String accountDetailsTimeStamp) {
        this.accountDetailsTimeStamp = accountDetailsTimeStamp;
    }

    public String getExtendedAccountDetailsTimeStamp() {
        return extendedAccountDetailsTimeStamp;
    }

    public void setExtendedAccountDetailsTimeStamp(String extendedAccountDetailsTimeStamp) {
        this.extendedAccountDetailsTimeStamp = extendedAccountDetailsTimeStamp;
    }

    public String getInvalidateSdkCache() {
        return invalidateSdkCache;
    }

    public void setInvalidateSdkCache(String invalidateSdkCache) {
        this.invalidateSdkCache = invalidateSdkCache;
    }

    public String getUseHttpsOnly() {
        return useHttpsOnly;
    }

    public void setUseHttpsOnly(String useHttpsOnly) {
        this.useHttpsOnly = useHttpsOnly;
    }

    public String getShowCopyright() {
        return showCopyright;
    }

    public void setShowCopyright(String showCopyright) {
        this.showCopyright = showCopyright;
    }

    public String getShowNotifOff() {
        return showNotifOff;
    }

    public void setShowNotifOff(String showNotifOff) {
        this.showNotifOff = showNotifOff;
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

    public @NonNull StorageState getStorageState() {
        return storageState;
    }

    public void setStorageState(@NonNull StorageState storageState) {
        this.storageState = storageState;
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
