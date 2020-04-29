package mega.privacy.android.app.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.jobservices.SyncRecord.TYPE_ANY;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.JobUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

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

    private static MegaApplication app = MegaApplication.getInstance();
    private static DatabaseHandler dbH = app.getDbH();

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

    public static void clearCUBackUp() {
        app.getSharedPreferences(LAST_CAM_SYNC_TIMESTAMP_FILE, Context.MODE_PRIVATE).edit().clear().apply();
    }

    /**
     * If the handle matches the previous primary folder's handle, restore the time stamp from stamps
     * if not clean the sync record from previous primary folder
     */
    public static void restorePrimaryTimestampsAndSyncRecordProcess() {
        SharedPreferences sharedPreferences = app.getSharedPreferences(LAST_CAM_SYNC_TIMESTAMP_FILE, Context.MODE_PRIVATE);
        long backupedHandle = sharedPreferences.getLong(KEY_PRIMARY_HANDLE, -2);
        long detectedPrimaryKey = getPrimaryFolderHandle();
        logDebug("Primary handle in local is: " + detectedPrimaryKey + ", backuped handle is: " + backupedHandle);
        if (detectedPrimaryKey == backupedHandle) {
            // if the primary handle matches to previous deleted primary folder's handle, restore the time stamp
            String camSyncStamp = sharedPreferences.getString(KEY_CAM_SYNC_TIMESTAMP, "");
            if (!isTextEmpty(camSyncStamp)) {
                try {
                    dbH.setCamSyncTimeStamp(Long.parseLong(camSyncStamp));
                } catch (Exception ex) {
                    logError("Exception happens: " + ex.toString());
                }
            }

            String camVideoSyncStamp = sharedPreferences.getString(KEY_CAM_VIDEO_SYNC_TIMESTAMP, "");
            if (!isTextEmpty(camVideoSyncStamp)) {
                try {
                    dbH.setCamVideoSyncTimeStamp(Long.parseLong(camVideoSyncStamp));
                } catch (Exception ex) {
                    logError("Exception happens: " + ex.toString());
                }
            }
        } else {
            // when primary target folder has been changed, delete primary sync records.
            dbH.deleteAllPrimarySyncRecords(TYPE_ANY);
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
        logDebug("Secondary handle in local is: " + detectedSecondaryKey + ", backuped handle is: " + backupedHanlde);
        if (backupedHanlde == detectedSecondaryKey) {
            // if the secondary handle matches to previous deleted secondary folder's handle, restore the time stamp
            String secSyncStamp = sharedPreferences.getString(KEY_SEC_SYNC_TIMESTAMP, "");
            if (!isTextEmpty(secSyncStamp)) {
                try {
                    dbH.setSecSyncTimeStamp(Long.parseLong(secSyncStamp));
                } catch (Exception ex) {
                    logError("Exception happens: " + ex.toString());
                }
            }

            String secVideoSyncStamp = sharedPreferences.getString(KEY_SEC_VIDEO_SYNC_TIMESTAMP, "");
            if (!isTextEmpty(secVideoSyncStamp)) {
                try {
                    dbH.setSecVideoSyncTimeStamp(Long.parseLong(secVideoSyncStamp));
                } catch (Exception ex) {
                    logError("Exception happens: " + ex.toString());
                }
            }
        } else {
            // when secondary target folder has been changed, delete secondary sync records.
            dbH.deleteAllSecondarySyncRecords(TYPE_ANY);
        }
        clearSecondaryBackUp();
    }

    /**
     * The method is to backup time stamps, primary upload folder and secondary folder in share preference after
     * database records being cleaned
     */
    public static void backupTimestampsAndFolderHandle() {
        MegaPreferences prefs = dbH.getPreferences();
        if (prefs == null) {
            logError("Preference is null, while backup.");
            return;
        }
        app.getSharedPreferences(LAST_CAM_SYNC_TIMESTAMP_FILE, Context.MODE_PRIVATE)
           .edit()
           .putString(KEY_CAM_SYNC_TIMESTAMP, prefs.getCamSyncTimeStamp())
           .putString(KEY_CAM_VIDEO_SYNC_TIMESTAMP, prefs.getCamVideoSyncTimeStamp())
           .putString(KEY_SEC_SYNC_TIMESTAMP, prefs.getSecSyncTimeStamp())
           .putString(KEY_SEC_VIDEO_SYNC_TIMESTAMP, prefs.getSecVideoSyncTimeStamp())
           .putLong(KEY_PRIMARY_HANDLE, getUploadFolderHandle(true))
           .putLong(KEY_SECONDARY_HANDLE, getUploadFolderHandle(false))
           .apply();
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
        logDebug("Reset primary timeline");
        dbH.setCamSyncTimeStamp(0);
        dbH.setCamVideoSyncTimeStamp(0);
        dbH.deleteAllPrimarySyncRecords(TYPE_ANY);
    }

    public static void resetSecondaryTimeline() {
        logDebug("Reset secondary timeline");
        dbH.setSecSyncTimeStamp(0);
        dbH.setSecVideoSyncTimeStamp(0);
        dbH.deleteAllSecondarySyncRecords(TYPE_ANY);
    }

    public static long getPrimaryFolderHandle() {
        return getUploadFolderHandle(true);
    }

    public static long getSecondaryFolderHandle() {
        return getUploadFolderHandle(false);
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

    public static void disableMediaUploadProcess() {
        resetMUTimestampsAndCache();
        dbH.setSecondaryUploadEnabled(false);
        stopRunningCameraUploadService(app);
    }
    /**
     * This method is to disable the CU and MU settings in database
     *
     * @param clearCamsyncRecords the boolean setting whether to clean the cam record
     */
    public static void disableCameraUploadSettingProcess(boolean clearCamsyncRecords) {
        resetCUTimestampsAndCache(clearCamsyncRecords);
        new Handler().postDelayed(() -> {
            if (dbH.shouldClearCamsyncRecords()) {
                dbH.deleteAllSyncRecords(TYPE_ANY);
                dbH.saveShouldClearCamsyncRecords(false);
            }
        }, 10 * 1000);

        // disable both primary and secondary.
        dbH.setCamSyncEnabled(false);
        dbH.setSecondaryUploadEnabled(false);
        stopRunningCameraUploadService(app);
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
        LocalBroadcastManager.getInstance(MegaApplication.getInstance()).sendBroadcast(intent);
    }
}
