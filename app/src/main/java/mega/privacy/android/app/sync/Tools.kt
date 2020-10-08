package mega.privacy.android.app.sync

import mega.privacy.android.app.DatabaseHandler.*
import mega.privacy.android.app.MegaApplication
import java.lang.Boolean
import kotlin.random.Random


fun Long.name(): String? = MegaApplication.getInstance().megaApi.getNodeByHandle(this)?.name

fun randomResult() = Random.nextBoolean()

fun updateSQL(syncPair: SyncPair) =
    "UPDATE $TABLE_SYNC_PAIRS SET " +
            "$KEY_SYNC_NAME = '${encrypt(syncPair.name)}', " +
            "$KEY_SYNC_PAIR_TYPE = ${syncPair.syncType}, " +
            "$KEY_SYNC_LOCAL_PATH = '${encrypt(syncPair.localFolderPath)}', " +
            "$KEY_SYNC_CLOUD_PATH = '${encrypt(syncPair.targetFolderPath)}', " +
            "$KEY_SYNC_CLOUD_HANDLE = '${encrypt(syncPair.targetFodlerHanlde.toString())}', " +
            "$KEY_SYNC_EX = '${encrypt(Boolean.toString(syncPair.isExcludeSubFolders))}', " +
            "$KEY_SYNC_DEL = '${encrypt(Boolean.toString(syncPair.isDeleteEmptySubFolders))}', " +
            "$KEY_SYNC_START_TIME = '${encrypt(syncPair.startTimestamp.toString())}', " +
            "$KEY_SYNC_LAST_SYNC_TIME = '${encrypt(syncPair.lastFinishTimestamp.toString())}', " +
            "$KEY_SYNC_PAIR_STATE = ${syncPair.state}, " +
            "$KEY_SYNC_PAIR_SUB_STATE = ${syncPair.subState}, " +
            "$KEY_SYNC_EXTRA_DATA = '${encrypt(syncPair.extraData)}', " +
            "$KEY_SYNC_OUTDATED = '${encrypt(Boolean.toString(syncPair.outdated))}'" +
            "WHERE $KEY_SYNC_ID = '${encrypt(syncPair.syncId.toString())}'"

fun deleteSQL(id: Long) = "DELETE FROM $TABLE_SYNC_PAIRS WHERE $KEY_SYNC_ID = '${encrypt(id.toString())}'"