package mega.privacy.android.app.sync

import mega.privacy.android.app.DatabaseHandler.Companion.KEY_BACKUP_DEL
import mega.privacy.android.app.DatabaseHandler.Companion.KEY_BACKUP_EX
import mega.privacy.android.app.DatabaseHandler.Companion.KEY_BACKUP_EXTRA_DATA
import mega.privacy.android.app.DatabaseHandler.Companion.KEY_BACKUP_ID
import mega.privacy.android.app.DatabaseHandler.Companion.KEY_BACKUP_LAST_TIME
import mega.privacy.android.app.DatabaseHandler.Companion.KEY_BACKUP_LOCAL_FOLDER
import mega.privacy.android.app.DatabaseHandler.Companion.KEY_BACKUP_NAME
import mega.privacy.android.app.DatabaseHandler.Companion.KEY_BACKUP_OUTDATED
import mega.privacy.android.app.DatabaseHandler.Companion.KEY_BACKUP_START_TIME
import mega.privacy.android.app.DatabaseHandler.Companion.KEY_BACKUP_STATE
import mega.privacy.android.app.DatabaseHandler.Companion.KEY_BACKUP_SUB_STATE
import mega.privacy.android.app.DatabaseHandler.Companion.KEY_BACKUP_TARGET_NODE
import mega.privacy.android.app.DatabaseHandler.Companion.KEY_BACKUP_TARGET_NODE_PATH
import mega.privacy.android.app.DatabaseHandler.Companion.KEY_BACKUP_TYPE
import mega.privacy.android.app.DatabaseHandler.Companion.TABLE_BACKUPS
import mega.privacy.android.app.DatabaseHandler.Companion.encrypt
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager
import mega.privacy.android.app.utils.CameraUploadUtil

/**
 * @return Name of the node with the handle. null if the node doesn't exist.
 */
fun Long.name(): String? = MegaApplication.getInstance().megaApi.getNodeByHandle(this)?.name

/**
 * @return A update SQL with a given backup object.
 */
fun updateSQL(backup: Backup) =
    "UPDATE $TABLE_BACKUPS SET " +
            "$KEY_BACKUP_NAME = '${encrypt(backup.backupName)}', " +
            "$KEY_BACKUP_TYPE = ${backup.backupType}, " +
            "$KEY_BACKUP_LOCAL_FOLDER = '${encrypt(backup.localFolder)}', " +
            "$KEY_BACKUP_TARGET_NODE_PATH = '${encrypt(backup.targetFolderPath)}', " +
            "$KEY_BACKUP_TARGET_NODE = '${encrypt(backup.targetNode.toString())}', " +
            "$KEY_BACKUP_EX = '${encrypt(backup.isExcludeSubFolders.toString())}', " +
            "$KEY_BACKUP_DEL = '${encrypt(backup.isDeleteEmptySubFolders.toString())}', " +
            "$KEY_BACKUP_START_TIME = '${encrypt(backup.startTimestamp.toString())}', " +
            "$KEY_BACKUP_LAST_TIME = '${encrypt(backup.lastFinishTimestamp.toString())}', " +
            "$KEY_BACKUP_STATE = ${backup.state.value}, " +
            "$KEY_BACKUP_SUB_STATE = ${backup.subState}, " +
            "$KEY_BACKUP_EXTRA_DATA = '${encrypt(backup.extraData)}', " +
            "$KEY_BACKUP_OUTDATED = '${encrypt(backup.outdated.toString())}'" +
            "WHERE $KEY_BACKUP_ID = '${encrypt(backup.backupId.toString())}'"

/**
 * @param id Backup id which is to be deleted.
 * @return A delete backup SQL.
 */
fun deleteSQL(id: Long) =
    "DELETE FROM $TABLE_BACKUPS WHERE $KEY_BACKUP_ID = '${encrypt(id.toString())}'"

/**
 * When user tries to logout, should delete backups first.
 * This should be called before megaApi.logout().
 */
fun removeBackupsBeforeLogout() {
    if (CameraUploadUtil.isPrimaryEnabled()) {
        CameraUploadSyncManager.removePrimaryBackup()
    }

    if (CameraUploadUtil.isSecondaryEnabled()) {
        CameraUploadSyncManager.removeSecondaryBackup()
    }
}