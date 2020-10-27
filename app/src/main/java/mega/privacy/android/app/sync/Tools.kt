package mega.privacy.android.app.sync

import mega.privacy.android.app.DatabaseHandler.*
import mega.privacy.android.app.MegaApplication
import java.lang.Boolean
import kotlin.random.Random


fun Long.name(): String? = MegaApplication.getInstance().megaApi.getNodeByHandle(this)?.name

fun randomResult() = Random.nextBoolean()

fun updateSQL(backup: Backup) =
    "UPDATE $TABLE_BACKUPS SET " +
            "$KEY_BACKUP_NAME = '${encrypt(backup.backupName)}', " +
            "$KEY_BACKUP_TYPE = ${backup.backupType}, " +
            "$KEY_BACKUP_LOCAL_FOLDER = '${encrypt(backup.localFolder)}', " +
            "$KEY_BACKUP_TARGET_NODE_PATH = '${encrypt(backup.targetFolderPath)}', " +
            "$KEY_BACKUP_TARGET_NODE = '${encrypt(backup.targetNode.toString())}', " +
            "$KEY_BACKUP_EX = '${encrypt(Boolean.toString(backup.isExcludeSubFolders))}', " +
            "$KEY_BACKUP_DEL = '${encrypt(Boolean.toString(backup.isDeleteEmptySubFolders))}', " +
            "$KEY_BACKUP_START_TIME = '${encrypt(backup.startTimestamp.toString())}', " +
            "$KEY_BACKUP_LAST_TIME = '${encrypt(backup.lastFinishTimestamp.toString())}', " +
            "$KEY_BACKUP_STATE = ${backup.state}, " +
            "$KEY_BACKUP_SUB_STATE = ${backup.subState}, " +
            "$KEY_BACKUP_EXTRA_DATA = '${encrypt(backup.extraData)}', " +
            "$KEY_BACKUP_OUTDATED = '${encrypt(Boolean.toString(backup.outdated))}'" +
            "WHERE $KEY_BACKUP_ID = '${encrypt(backup.backupId.toString())}'"

fun deleteSQL(id: Long) = "DELETE FROM $TABLE_BACKUPS WHERE $KEY_BACKUP_ID = '${encrypt(id.toString())}'"