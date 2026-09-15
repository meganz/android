package mega.privacy.android.data.model;

import timber.log.Timber;

public class MegaPreferences {

    String firstTime;
    String camSyncWifi;
    String camSyncEnabled;
    String camSyncHandle;
    String camSyncLocalPath;
    String camSyncFileUpload;
    String storageAskAlways;
    String storageDownloadLocation;
    String secondaryMediaFolderEnabled;
    String localPathSecondaryFolder;
    String megaHandleSecondaryFolder;
    String keepFileNames;
    String preferredViewList;
    String uriExternalSDCard;
    String cameraFolderExternalSDCard;
    String preferredSortCloud;
    private String preferredSortCameraUpload;
    String preferredSortOthers;
    String uploadVideoQuality;
    String conversionOnCharging;
    private String removeGPS;
    String chargingOnSize;
    String isAutoPlayEnabled;

    public final static int ONLY_PHOTOS = 1001;
    public final static int ONLY_VIDEOS = 1002;
    public final static int PHOTOS_AND_VIDEOS = 1003;
    public final static int CHARGING_ON_SIZE_DEFAULT = 200;

    public MegaPreferences(String firstTime, String camSyncWifi, String camSyncEnabled,
                    String camSyncHandle, String camSyncLocalPath, String camSyncFileUpload,
                    String storageAskAlways, String storageDownloadLocation,
                    String secondaryMediaFolderEnabled, String localPathSecondaryFolder,
                    String megaHandleSecondaryFolder, String keepFileNames,
                    String preferredViewList, String uriExternalSDCard,
                    String cameraFolderExternalSDCard, String preferredSortCloud,
                    String preferredSortOthers, String uploadVideoQuality,
                    String conversionOnCharging, String chargingOnSize,
                    String isAutoPlayEnabled, String removeGPS,
                    String preferredSortCameraUpload) {
        this.firstTime = firstTime;
        this.camSyncWifi = camSyncWifi;
        this.camSyncEnabled = camSyncEnabled;
        this.camSyncHandle = camSyncHandle;
        this.camSyncLocalPath = camSyncLocalPath;
        this.camSyncFileUpload = camSyncFileUpload;
        this.storageAskAlways = storageAskAlways;
        this.storageDownloadLocation = storageDownloadLocation;
        this.secondaryMediaFolderEnabled = secondaryMediaFolderEnabled;
        this.localPathSecondaryFolder = localPathSecondaryFolder;
        this.megaHandleSecondaryFolder = megaHandleSecondaryFolder;
        this.keepFileNames = keepFileNames;
        this.preferredViewList = preferredViewList;
        this.uriExternalSDCard = uriExternalSDCard;
        this.cameraFolderExternalSDCard = cameraFolderExternalSDCard;
        this.preferredSortCloud = preferredSortCloud;
        this.preferredSortOthers = preferredSortOthers;
        this.uploadVideoQuality = uploadVideoQuality;
        this.conversionOnCharging = conversionOnCharging;
        this.chargingOnSize = chargingOnSize;
        this.isAutoPlayEnabled = isAutoPlayEnabled;
        this.removeGPS = removeGPS;
        this.preferredSortCameraUpload = preferredSortCameraUpload;
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

    public String getCamSyncFileUpload() {
        return camSyncFileUpload;
    }

    public void setCamSyncFileUpload(String camSyncFileUpload) {
        this.camSyncFileUpload = camSyncFileUpload;
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

    public String getKeepFileNames() {
        return keepFileNames;
    }

    public void setKeepFileNames(String keepFileNames) {
        this.keepFileNames = keepFileNames;
    }

    public String getPreferredViewList() {
        return preferredViewList;
    }

    public void setPreferredViewList(String preferredViewList) {
        this.preferredViewList = preferredViewList;
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

    public String getChargingOnSize() {
        return chargingOnSize;
    }

    public void setChargingOnSize(String chargingOnSize) {
        this.chargingOnSize = chargingOnSize;
    }

    public String getRemoveGPS() {
        return removeGPS;
    }

    public void setRemoveGPS(String removeGPS) {
        this.removeGPS = removeGPS;
    }

    public String getIsAutoPlayEnabled() {
        return isAutoPlayEnabled;
    }

    public void setIsAutoPlayEnabled(String isAutoPlayEnabled) {
        this.isAutoPlayEnabled = isAutoPlayEnabled;
    }

    public boolean isAutoPlayEnabled() {
        return Boolean.parseBoolean(isAutoPlayEnabled);
    }
}
