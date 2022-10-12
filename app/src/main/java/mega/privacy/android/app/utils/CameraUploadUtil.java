package mega.privacy.android.app.utils;

import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_CU_ATTR_CHANGE;
import static mega.privacy.android.app.constants.BroadcastConstants.EXTRA_IS_CU_SECONDARY_FOLDER;
import static mega.privacy.android.app.jobservices.CameraUploadsService.CAMERA_UPLOADS_ENGLISH;
import static mega.privacy.android.app.jobservices.CameraUploadsService.SECONDARY_UPLOADS_ENGLISH;
import static mega.privacy.android.app.utils.Constants.EXTRA_NODE_HANDLE;
import static mega.privacy.android.app.utils.Constants.SEPARATOR;
import static mega.privacy.android.app.utils.FileUtil.purgeDirectory;
import static mega.privacy.android.app.utils.JobUtil.fireStopCameraUploadJob;
import static mega.privacy.android.app.utils.StringResourcesUtils.getString;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.io.File;

import mega.privacy.android.data.database.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.data.model.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.listeners.RenameListener;
import mega.privacy.android.app.listeners.SetAttrUserListener;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;

public class CameraUploadUtil {

    /**
     * Keys for backing up time stamps
     */
    private static final String KEY_CAM_SYNC_TIMESTAMP = "KEY_CAM_SYNC_TIMESTAMP";
    private static final String KEY_CAM_VIDEO_SYNC_TIMESTAMP = "KEY_CAM_VIDEO_SYNC_TIMESTAMP";
    private static final String KEY_SEC_SYNC_TIMESTAMP = "KEY_SEC_SYNC_TIMESTAMP";
    private static final String KEY_SEC_VIDEO_SYNC_TIMESTAMP = "KEY_SEC_VIDEO_SYNC_TIMESTAMP";
    private static final String KEY_PRIMARY_HANDLE = "KEY_PRIMARY_HANDLE";
    private static final String KEY_SECONDARY_HANDLE = "KEY_SECONDARY_HANDLE";
    private static final String LAST_CAM_SYNC_TIMESTAMP_FILE = "LAST_CAM_SYNC_TIMESTAMP_FILE";

    private static final MegaApplication app = MegaApplication.getInstance();
    private static final DatabaseHandler dbH = app.getDbH();

    /**
     * The method is to delete the timestamps backup in share preference
     */
    private static void clearPrimaryBackUp() {
        app.getSharedPreferences(LAST_CAM_SYNC_TIMESTAMP_FILE, Context.MODE_PRIVATE).edit()
                .putString(KEY_CAM_SYNC_TIMESTAMP, "")
                .putString(KEY_CAM_VIDEO_SYNC_TIMESTAMP, "")
                .putLong(KEY_PRIMARY_HANDLE, -2L)
                .apply();
    }

    private static void clearSecondaryBackUp() {
        app.getSharedPreferences(LAST_CAM_SYNC_TIMESTAMP_FILE, Context.MODE_PRIVATE).edit()
                .putString(KEY_SEC_SYNC_TIMESTAMP, "")
                .putString(KEY_SEC_VIDEO_SYNC_TIMESTAMP, "")
                .putLong(KEY_SECONDARY_HANDLE, -2L)
                .apply();
    }

    /**
     * If the handle matches the previous primary folder's handle, restore the time stamp from stamps
     * if not clean the sync record from previous primary folder
     */
    public static void restorePrimaryTimestampsAndSyncRecordProcess() {
        SharedPreferences sharedPreferences = app.getSharedPreferences(LAST_CAM_SYNC_TIMESTAMP_FILE, Context.MODE_PRIVATE);
        long backupedHandle = sharedPreferences.getLong(KEY_PRIMARY_HANDLE, -2);
        long detectedPrimaryKey = getPrimaryFolderHandle();
        Timber.d("Primary handle in local is: %d, backuped handle is: %d", detectedPrimaryKey, backupedHandle);
        if (detectedPrimaryKey == backupedHandle) {
            // if the primary handle matches to previous deleted primary folder's handle, restore the time stamp
            String camSyncStamp = sharedPreferences.getString(KEY_CAM_SYNC_TIMESTAMP, "");
            if (!isTextEmpty(camSyncStamp)) {
                try {
                    dbH.setCamSyncTimeStamp(Long.parseLong(camSyncStamp));
                } catch (Exception ex) {
                    Timber.e(ex);
                }
            }

            String camVideoSyncStamp = sharedPreferences.getString(KEY_CAM_VIDEO_SYNC_TIMESTAMP, "");
            if (!isTextEmpty(camVideoSyncStamp)) {
                try {
                    dbH.setCamVideoSyncTimeStamp(Long.parseLong(camVideoSyncStamp));
                } catch (Exception ex) {
                    Timber.e(ex);
                }
            }
        } else {
            // when primary target folder has been changed, delete primary sync records.
            dbH.deleteAllPrimarySyncRecords();
        }
        clearPrimaryBackUp();
    }

    /**
     * If the handle matches the previous secondary folder's handle, restore the time stamp from stamps
     * if not clean the sync record from previous primary folder
     */
    public static void restoreSecondaryTimestampsAndSyncRecordProcess() {
        SharedPreferences sharedPreferences = app.getSharedPreferences(LAST_CAM_SYNC_TIMESTAMP_FILE, Context.MODE_PRIVATE);
        long backupedHanlde = sharedPreferences.getLong(KEY_SECONDARY_HANDLE, -2);
        long detectedSecondaryKey = getSecondaryFolderHandle();
        Timber.d("Secondary handle in local is: %d, backuped handle is: %d", detectedSecondaryKey, backupedHanlde);
        if (backupedHanlde == detectedSecondaryKey) {
            // if the secondary handle matches to previous deleted secondary folder's handle, restore the time stamp
            String secSyncStamp = sharedPreferences.getString(KEY_SEC_SYNC_TIMESTAMP, "");
            if (!isTextEmpty(secSyncStamp)) {
                try {
                    dbH.setSecSyncTimeStamp(Long.parseLong(secSyncStamp));
                } catch (Exception ex) {
                    Timber.e(ex);
                }
            }

            String secVideoSyncStamp = sharedPreferences.getString(KEY_SEC_VIDEO_SYNC_TIMESTAMP, "");
            if (!isTextEmpty(secVideoSyncStamp)) {
                try {
                    dbH.setSecVideoSyncTimeStamp(Long.parseLong(secVideoSyncStamp));
                } catch (Exception ex) {
                    Timber.e(ex);
                }
            }
        } else {
            // when secondary target folder has been changed, delete secondary sync records.
            dbH.deleteAllSecondarySyncRecords();
        }
        clearSecondaryBackUp();
    }

    /**
     * set all the time stamps to 0 for uploading, clean the cache directory for gps process
     * and clean the sync record by default
     */
    public static void resetCUTimestampsAndCache() {
        resetCUTimestampsAndCache(true);
    }

    public static void resetMUTimestampsAndCache() {
        dbH.setSecSyncTimeStamp(0);
        dbH.setSecVideoSyncTimeStamp(0);
    }

    public static void resetCUTimestampsAndCache(boolean clearCamsynRecords) {
        dbH.setCamSyncTimeStamp(0);
        dbH.setCamVideoSyncTimeStamp(0);
        dbH.setSecSyncTimeStamp(0);
        dbH.setSecVideoSyncTimeStamp(0);
        dbH.saveShouldClearCamsyncRecords(clearCamsynRecords);
        purgeDirectory(new File(app.getCacheDir().toString() + SEPARATOR));
    }

    public static void resetPrimaryTimeline() {
        Timber.d("Reset primary timeline");
        dbH.setCamSyncTimeStamp(0);
        dbH.setCamVideoSyncTimeStamp(0);
        dbH.deleteAllPrimarySyncRecords();
    }

    public static void resetSecondaryTimeline() {
        Timber.d("Reset secondary timeline");
        dbH.setSecSyncTimeStamp(0);
        dbH.setSecVideoSyncTimeStamp(0);
        dbH.deleteAllSecondarySyncRecords();
    }

    public static long getPrimaryFolderHandle() {
        return getUploadFolderHandle(true);
    }

    public static long getSecondaryFolderHandle() {
        return getUploadFolderHandle(false);
    }

    public static boolean isPrimaryEnabled() {
        MegaPreferences prefs = dbH.getPreferences();
        return prefs != null && Boolean.parseBoolean(prefs.getCamSyncEnabled());
    }

    public static boolean isSecondaryEnabled() {
        MegaPreferences prefs = dbH.getPreferences();
        return prefs != null && Boolean.parseBoolean(prefs.getSecondaryMediaFolderEnabled());
    }

    /**
     * @param isPrimary whether the primary upload's folder is returned
     * @return the primary or secondary upload folder's handle
     */
    private static long getUploadFolderHandle(boolean isPrimary) {
        MegaPreferences prefs = dbH.getPreferences();
        if (prefs == null) {
            return INVALID_HANDLE;
        }

        String handle = isPrimary ? prefs.getCamSyncHandle() : prefs.getMegaHandleSecondaryFolder();

        return isTextEmpty(handle) ? INVALID_HANDLE : Long.parseLong(handle);
    }

    /**
     * This method is to disable the CU and MU settings in database
     *
     * @param clearCamsyncRecords the boolean setting whether to clean the cam record
     */
    public static void disableCameraUploadSettingProcess(boolean clearCamsyncRecords) {
        resetCUTimestampsAndCache(clearCamsyncRecords);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dbH.shouldClearCamsyncRecords()) {
                dbH.deleteAllSyncRecordsTypeAny();
                dbH.saveShouldClearCamsyncRecords(false);
            }
        }, 10 * 1000);

        // disable both primary and secondary.
        dbH.setCamSyncEnabled(false);
        dbH.setSecondaryUploadEnabled(false);
        fireStopCameraUploadJob(app);
    }


    public static void disableMediaUploadProcess() {
        resetMUTimestampsAndCache();
        dbH.setSecondaryUploadEnabled(false);
        fireStopCameraUploadJob(app);
    }

    /**
     * This method is to disable the settings in database, clean the sync records by default
     */
    public static void disableCameraUploadSettingProcess() {
        disableCameraUploadSettingProcess(true);
    }

    public static long findDefaultFolder(String folderName) {
        MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();
        MegaNode node = megaApi.getNodeByPath(folderName, megaApi.getRootNode());
        return node != null && node.isFolder() && !megaApi.isInRubbish(node) ? node.getHandle() : INVALID_HANDLE;
    }

    /**
     * Force update node list's icon
     *
     * @param isSecondary whether the updated node is secondary folder
     * @param handle      the updated node handle
     */
    public static void forceUpdateCameraUploadFolderIcon(boolean isSecondary, long handle) {
        Intent intent = new Intent(BROADCAST_ACTION_INTENT_CU_ATTR_CHANGE);
        intent.putExtra(EXTRA_IS_CU_SECONDARY_FOLDER, isSecondary);
        intent.putExtra(EXTRA_NODE_HANDLE, handle);
        MegaApplication.getInstance().sendBroadcast(intent);
    }

    /**
     * This method is executed when the user has never set Cu attribute
     * Or the original cu folder has been deleted or put into rubbish bin
     *
     * @param context     the context where init process is executed
     * @param isSecondary determine whether it is camera upload or secondary upload
     */
    public static void initCUFolderFromScratch(Context context, boolean isSecondary) {
        if (isSecondary) {
            initSecondaryFolderFromScratch(context);
        } else {
            initPrimaryFolderFromScratch(context);
        }
    }

    private static void initPrimaryFolderFromScratch(Context context) {
        MegaApiAndroid api = MegaApplication.getInstance().getMegaApi();
        // Find previous camera upload folder, whose name is "Camera Uploads" in English
        long primaryHandle = findDefaultFolder(CAMERA_UPLOADS_ENGLISH);
        if (primaryHandle != INVALID_HANDLE) {
            Timber.d("Set CU primary attribute: %s", primaryHandle);
            api.setCameraUploadsFolders(primaryHandle, INVALID_HANDLE, new SetAttrUserListener(context));
            // if current device language is not English, rename this folder as "Camera Uploads" in other language
            if (!context.getString(R.string.section_photo_sync).equals(CAMERA_UPLOADS_ENGLISH)) {
                api.renameNode(api.getNodeByHandle(primaryHandle),
                        getString(R.string.section_photo_sync), new RenameListener());
            }
        }
    }

    private static void initSecondaryFolderFromScratch(Context context) {
        MegaApiAndroid api = MegaApplication.getInstance().getMegaApi();
        // Find previous camera upload folder, whose name is "Media Uploads" in English
        long secondaryHandle = findDefaultFolder(SECONDARY_UPLOADS_ENGLISH);
        if (secondaryHandle != INVALID_HANDLE) {
            // if current device language is not English, rename this folder as "Media Uploads" in other language
            Timber.d("Set CU secondary attribute: %s", secondaryHandle);
            api.setCameraUploadsFolders(INVALID_HANDLE, secondaryHandle, new SetAttrUserListener(context));
            if (!context.getString(R.string.section_secondary_media_uploads).equals(SECONDARY_UPLOADS_ENGLISH)) {
                api.renameNode(api.getNodeByHandle(secondaryHandle),
                        getString(R.string.section_secondary_media_uploads),
                        new RenameListener());
            }
        }
    }

    /**
     * The method is to update local cu attribute in database
     *
     * @param handleInUserAttr updated folder handle
     * @param isSecondary      whether this is about primary or secondary upload
     * @return whether camera upload services should stop since folder is changed
     */
    public static boolean compareAndUpdateLocalFolderAttribute(long handleInUserAttr, boolean isSecondary) {
        if (handleInUserAttr == INVALID_HANDLE) {
            return false;
        }

        boolean shouldCUStop = false;

        long primaryHandle = getPrimaryFolderHandle();
        long secondaryHandle = getSecondaryFolderHandle();

        //save changes to local DB
        if (isSecondary && handleInUserAttr != secondaryHandle) {
            dbH.setSecondaryFolderHandle(handleInUserAttr);
            resetSecondaryTimeline();
            shouldCUStop = true;
        } else if (!isSecondary && handleInUserAttr != primaryHandle) {
            dbH.setCamSyncHandle(handleInUserAttr);
            resetPrimaryTimeline();
            shouldCUStop = true;
        }
        return shouldCUStop;
    }
}
