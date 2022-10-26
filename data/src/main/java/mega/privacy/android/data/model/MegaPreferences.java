package mega.privacy.android.data.model;

import timber.log.Timber;

public class MegaPreferences {

    String firstTime;
    String camSyncWifi;
    String camSyncEnabled;
    String camSyncHandle;
    String camSyncLocalPath;
    String camSyncFileUpload;
    String camSyncTimeStamp;
    String camVideoSyncTimeStamp;
    String passcodeLockEnabled;
    String passcodeLockCode;
    String storageAskAlways;
    String storageDownloadLocation;
    String lastFolderUpload;
    String lastFolderCloud;
    String secondaryMediaFolderEnabled;
    String localPathSecondaryFolder;
    String megaHandleSecondaryFolder;
    String secSyncTimeStamp;
    String secVideoSyncTimeStamp;
    String keepFileNames;
    String storageAdvancedDevices;
    String preferredViewList;
    String preferredViewListCameraUploads;
    String uriExternalSDCard;
    String cameraFolderExternalSDCard;
    String passcodeLockType;
    String preferredSortCloud;
    private String preferredSortCameraUpload;
    String preferredSortOthers;
    String firstTimeChat;
    String uploadVideoQuality;
    String conversionOnCharging;
    private String removeGPS;
    String chargingOnSize;
    String shouldClearCameraSyncRecords;
    String isAutoPlayEnabled;
    private String showInviteBanner;
    String sdCardUri;
    private String askForDisplayOver;
    private String askForSetDownloadLocation;
    private String mediaSDCardUri;
    private String isMediaOnSDCard;
    private String passcodeLockRequireTime;
    private String fingerprintLock;

    public final static int ONLY_PHOTOS = 1001;
    public final static int ONLY_VIDEOS = 1002;
    public final static int PHOTOS_AND_VIDEOS = 1003;
    public final static int CHARGING_ON_SIZE_DEFAULT = 200;

    public MegaPreferences(String firstTime, String camSyncWifi, String camSyncEnabled,
                    String camSyncHandle, String camSyncLocalPath, String camSyncFileUpload,
                    String camSyncTimeStamp, String passcodeLockEnabled, String passcodeLockCode,
                    String storageAskAlways, String storageDownloadLocation,
                    String lastFolderUpload, String lastFolderCloud,
                    String secondaryMediaFolderEnabled, String localPathSecondaryFolder,
                    String megaHandleSecondaryFolder, String secSyncTimeStamp,
                    String keepFileNames, String storageAdvancedDevices, String preferredViewList,
                    String preferredViewListCameraUploads, String uriExternalSDCard,
                    String cameraFolderExternalSDCard, String passcodeLockType,
                    String preferredSortCloud, String preferredSortOthers, String firstTimeChat,
                    String uploadVideoQuality, String conversionOnCharging,
                    String chargingOnSize, String shouldClearCameraSyncRecords,
                    String camVideoSyncTimeStamp, String secVideoSyncTimeStamp,
                    String isAutoPlayEnabled, String removeGPS, String showInviteBanner,
                    String preferredSortCameraUpload, String sdCardUri, String askForDisplayOver,
                    String askForSetDownloadLocation, String mediaSDCardUri, String isMediaOnSDCard,
                    String passcodeLockRequireTime, String fingerprintLock) {
        this.firstTime = firstTime;
        this.camSyncWifi = camSyncWifi;
        this.camSyncEnabled = camSyncEnabled;
        this.camSyncHandle = camSyncHandle;
        this.camSyncLocalPath = camSyncLocalPath;
        this.camSyncFileUpload = camSyncFileUpload;
        this.camSyncTimeStamp = camSyncTimeStamp;
        this.passcodeLockEnabled = passcodeLockEnabled;
        this.passcodeLockCode = passcodeLockCode;
        this.storageAskAlways = storageAskAlways;
        this.storageDownloadLocation = storageDownloadLocation;
        this.lastFolderUpload = lastFolderUpload;
        this.lastFolderCloud = lastFolderCloud;
        this.secondaryMediaFolderEnabled = secondaryMediaFolderEnabled;
        this.localPathSecondaryFolder = localPathSecondaryFolder;
        this.megaHandleSecondaryFolder = megaHandleSecondaryFolder;
        this.secSyncTimeStamp = secSyncTimeStamp;
        this.keepFileNames = keepFileNames;
        this.storageAdvancedDevices = storageAdvancedDevices;
        this.preferredViewList = preferredViewList;
        this.preferredViewListCameraUploads = preferredViewListCameraUploads;
        this.uriExternalSDCard = uriExternalSDCard;
        this.cameraFolderExternalSDCard = cameraFolderExternalSDCard;
        this.passcodeLockType = passcodeLockType;
        this.preferredSortCloud = preferredSortCloud;
        this.preferredSortOthers = preferredSortOthers;
        this.firstTimeChat = firstTimeChat;
        this.uploadVideoQuality = uploadVideoQuality;
        this.conversionOnCharging = conversionOnCharging;
        this.chargingOnSize = chargingOnSize;
        this.shouldClearCameraSyncRecords = shouldClearCameraSyncRecords;
        this.camVideoSyncTimeStamp = camVideoSyncTimeStamp;
        this.secVideoSyncTimeStamp = secVideoSyncTimeStamp;
        this.isAutoPlayEnabled = isAutoPlayEnabled;
        this.removeGPS = removeGPS;
        this.showInviteBanner = showInviteBanner;
        this.preferredSortCameraUpload = preferredSortCameraUpload;
        this.sdCardUri = sdCardUri;
        this.askForDisplayOver = askForDisplayOver;
        this.askForSetDownloadLocation = askForSetDownloadLocation;
        this.mediaSDCardUri = mediaSDCardUri;
        this.isMediaOnSDCard = isMediaOnSDCard;
        this.passcodeLockRequireTime = passcodeLockRequireTime;
        this.fingerprintLock = fingerprintLock;
    }

    public String getFirstTime() {
        return firstTime;
    }

    public void setFirstTime(String firstTime) {
        this.firstTime = firstTime;
    }

    public String getCamSyncEnabled() {
        return camSyncEnabled;
    }

    public void setCamSyncEnabled(String camSyncEnabled) {
        this.camSyncEnabled = camSyncEnabled;
    }

    public String getCamSyncHandle() {
        return camSyncHandle;
    }

    public void setCamSyncHandle(String camSyncHandle) {
        this.camSyncHandle = camSyncHandle;
    }

    public String getCamSyncLocalPath() {
        return camSyncLocalPath;
    }

    public void setCamSyncLocalPath(String camSyncLocalPath) {
        this.camSyncLocalPath = camSyncLocalPath;
    }

    public String getCamSyncWifi() {
        return camSyncWifi;
    }

    public void setCamSyncWifi(String camSyncWifi) {
        this.camSyncWifi = camSyncWifi;
    }

    public String getShouldClearCameraSyncRecords() {
        return shouldClearCameraSyncRecords;
    }

    public void setShouldClearCameraSyncRecords(String shouldClearCameraSyncRecords) {
        this.shouldClearCameraSyncRecords = shouldClearCameraSyncRecords;
    }

    public String getCamSyncFileUpload() {
        return camSyncFileUpload;
    }

    public void setCamSyncFileUpload(String camSyncFileUpload) {
        this.camSyncFileUpload = camSyncFileUpload;
    }

    public String getCamSyncTimeStamp() {
        return camSyncTimeStamp;
    }

    public void setCamSyncTimeStamp(String camSyncTimeStamp) {
        this.camSyncTimeStamp = camSyncTimeStamp;
    }

    public String getPasscodeLockEnabled() {
        return passcodeLockEnabled;
    }

    public void setPasscodeLockEnabled(String passcodeLockEnabled) {
        this.passcodeLockEnabled = passcodeLockEnabled;
    }

    public String getPasscodeLockCode() {
        return passcodeLockCode;
    }

    public void setPasscodeLockCode(String passcodeLockCode) {
        this.passcodeLockCode = passcodeLockCode;
    }

    public String getStorageAskAlways() {
        return storageAskAlways;
    }

    public void setStorageAskAlways(String storageAskAlways) {
        this.storageAskAlways = storageAskAlways;
    }

    public String getStorageDownloadLocation() {
        return storageDownloadLocation;
    }

    public void setStorageDownloadLocation(String storageDownloadLocation) {
        this.storageDownloadLocation = storageDownloadLocation;
    }

    public String getPreferredSortCameraUpload() {
        return preferredSortCameraUpload;
    }

    public void setPreferredSortCameraUpload(String preferredSortCameraUpload) {
        this.preferredSortCameraUpload = preferredSortCameraUpload;
    }

    public String getLastFolderUpload() {
        if (lastFolderUpload == null || lastFolderUpload.length() == 0)
            return null;
        return lastFolderUpload;
    }

    public void setLastFolderUpload(String lastFolderUpload) {
        this.lastFolderUpload = lastFolderUpload;
    }

    public String getShowInviteBanner() {
        return showInviteBanner;
    }

    public String getLastFolderCloud() {
        if (lastFolderCloud == null || lastFolderCloud.length() == 0)
            return null;

        return lastFolderCloud;
    }

    public void setLastFolderCloud(String lastFolderCloud) {
        this.lastFolderCloud = lastFolderCloud;
    }

    public String getSecondaryMediaFolderEnabled() {
        return secondaryMediaFolderEnabled;
    }

    public void setSecondaryMediaFolderEnabled(String secondaryMediaFolderEnabled) {
        this.secondaryMediaFolderEnabled = secondaryMediaFolderEnabled;
    }

    public String getLocalPathSecondaryFolder() {
        return localPathSecondaryFolder;
    }

    public void setLocalPathSecondaryFolder(String localPathSecondaryFolder) {
        this.localPathSecondaryFolder = localPathSecondaryFolder;
    }

    public String getMegaHandleSecondaryFolder() {
        Timber.d("getMegaHandleSecondaryFolder %s", megaHandleSecondaryFolder);
        return megaHandleSecondaryFolder;
    }

    public void setMegaHandleSecondaryFolder(String megaHandleSecondaryFolder) {
        this.megaHandleSecondaryFolder = megaHandleSecondaryFolder;
    }

    public String getSecSyncTimeStamp() {
        return secSyncTimeStamp;
    }

    public void setSecSyncTimeStamp(String secSyncTimeStamp) {
        this.secSyncTimeStamp = secSyncTimeStamp;
    }

    public String getKeepFileNames() {
        return keepFileNames;
    }

    public void setKeepFileNames(String keepFileNames) {
        this.keepFileNames = keepFileNames;
    }

    public String getStorageAdvancedDevices() {
        return storageAdvancedDevices;
    }

    public void setStorageAdvancedDevices(String storageAdvancedDevices) {
        this.storageAdvancedDevices = storageAdvancedDevices;
    }

    public String getPreferredViewList() {
        return preferredViewList;
    }

    public void setPreferredViewList(String preferredViewList) {
        this.preferredViewList = preferredViewList;
    }

    public String getPreferredViewListCameraUploads() {
        return preferredViewListCameraUploads;
    }

    public void setPreferredViewListCameraUploads(
            String preferredViewListCameraUploads) {
        this.preferredViewListCameraUploads = preferredViewListCameraUploads;
    }

    public String getUriExternalSDCard() {
        return this.uriExternalSDCard;
    }

    public void setUriExternalSDCard(String uriExternalSDCard) {
        this.uriExternalSDCard = uriExternalSDCard;
    }

    public String getCameraFolderExternalSDCard() {
        return this.cameraFolderExternalSDCard;
    }

    public void setCameraFolderExternalSDCard(String cameraFolderExternalSDCard) {
        this.cameraFolderExternalSDCard = cameraFolderExternalSDCard;
    }

    public String getPasscodeLockType() {
        return passcodeLockType;
    }

    public void setPasscodeLockType(String passcodeLockType) {
        this.passcodeLockType = passcodeLockType;
    }


    public String getPreferredSortCloud() {
        return preferredSortCloud;
    }

    public void setPreferredSortCloud(String preferredSortCloud) {
        this.preferredSortCloud = preferredSortCloud;
    }

    public String getPreferredSortOthers() {
        return preferredSortOthers;
    }

    public void setPreferredSortOthers(String preferredSortOthers) {
        this.preferredSortOthers = preferredSortOthers;
    }

    public String getFirstTimeChat() {
        return firstTimeChat;
    }

    public void setFirstTimeChat(String firstTimeChat) {
        this.firstTimeChat = firstTimeChat;
    }

    public String getUploadVideoQuality() {
        return uploadVideoQuality;
    }

    public void setUploadVideoQuality(String uploadVideoQuality) {
        this.uploadVideoQuality = uploadVideoQuality;
    }

    public String getConversionOnCharging() {
        return conversionOnCharging;
    }

    public void setConversionOnCharging(String conversionOnCharging) {
        this.conversionOnCharging = conversionOnCharging;
    }

    public String getSdCardUri() {
        return sdCardUri;
    }

    public void setSdCardUri(String sdCardUri) {
        this.sdCardUri = sdCardUri;
    }

    public String getChargingOnSize() {
        return chargingOnSize;
    }

    public void setChargingOnSize(String chargingOnSize) {
        this.chargingOnSize = chargingOnSize;
    }

    public String getCamVideoSyncTimeStamp() {
        return camVideoSyncTimeStamp;
    }

    public void setCamVideoSyncTimeStamp(String camVideoSyncTimeStamp) {
        this.camVideoSyncTimeStamp = camVideoSyncTimeStamp;
    }

    public String getSecVideoSyncTimeStamp() {
        return secVideoSyncTimeStamp;
    }

    public void setSecVideoSyncTimeStamp(String secVideoSyncTimeStamp) {
        this.secVideoSyncTimeStamp = secVideoSyncTimeStamp;
    }

    public String getRemoveGPS() {
        return removeGPS;
    }

    public void setRemoveGPS(String removeGPS) {
        this.removeGPS = removeGPS;
    }

    public String getAskForDisplayOver() {
        return askForDisplayOver;
    }

    public void setAskForDisplayOver(String askForDisplayOver) {
        this.askForDisplayOver = askForDisplayOver;
    }

    public String getAskForSetDownloadLocation() {
        return askForSetDownloadLocation;
    }

    public void setAskForSetDownloadLocation(String askForSetDownloadLocation) {
        this.askForSetDownloadLocation = askForSetDownloadLocation;
    }

    public String getMediaSDCardUri() {
        return mediaSDCardUri;
    }

    public void setMediaSDCardUri(String mediaSDCardUri) {
        this.mediaSDCardUri = mediaSDCardUri;
    }

    public String getIsMediaOnSDCard() {
        return isMediaOnSDCard;
    }

    public void setIsMediaOnSDCard(String isMediaOnSDCard) {
        this.isMediaOnSDCard = isMediaOnSDCard;
    }

    public String getIsAutoPlayEnabled() {
        return isAutoPlayEnabled;
    }

    public void setIsAutoPlayEnabled(String isAutoPlayEnabled) {
        this.isAutoPlayEnabled = isAutoPlayEnabled;
    }

    public void setShowInviteBanner(String showInviteBanner) {
        this.showInviteBanner = showInviteBanner;
    }

    public String getPasscodeLockRequireTime() {
        return passcodeLockRequireTime;
    }

    public void setPasscodeLockRequireTime(String passcodeLockRequireTime) {
        this.passcodeLockRequireTime = passcodeLockRequireTime;
    }

    public String getFingerprintLock() {
        return fingerprintLock;
    }

    @Override
    public String toString() {
        return "MegaPreferences{" +
                "camSyncTimeStamp='" + camSyncTimeStamp + '\'' +
                ", camVideoSyncTimeStamp='" + camVideoSyncTimeStamp + '\'' +
                ", secSyncTimeStamp='" + secSyncTimeStamp + '\'' +
                ", secVideoSyncTimeStamp='" + secVideoSyncTimeStamp + '\'' +
                '}';
    }

    public boolean isAutoPlayEnabled() {
        return Boolean.parseBoolean(isAutoPlayEnabled);
    }
}

