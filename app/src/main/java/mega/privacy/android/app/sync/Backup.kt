package mega.privacy.android.app.sync

import mega.privacy.android.app.sync.cusync.CuSyncManager
import mega.privacy.android.app.utils.Constants.INVALID_NON_NULL_VALUE
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError

/**
 * Backup data object.
 */
data class Backup(

    /**
     * ID of the backup, generate by server when set backup.
     */
    var backupId: Long,
    var backupType: Int,

    /**
     * Handle of the MegaNode where the backup targets to.
     */
    var targetNode: Long,
    /**
     * Path of the local folder where the backup uploads from.
     */
    var localFolder: String,
    var backupName: String,
    /**
     * Valid value definitions
     * @see MegaApiJava
     */
    var state: Int = CuSyncManager.State.CU_SYNC_STATE_ACTIVE,
    /**
     * Valid value definitions
     * @see MegaError
     */
    var subState: Int = MegaError.API_OK,
    var extraData: String = INVALID_NON_NULL_VALUE,
    @ClientOnly var startTimestamp: Long = 0L,
    @ClientOnly var lastFinishTimestamp: Long = 0L,
    @ClientOnly var targetFolderPath: String = INVALID_NON_NULL_VALUE,
    @ClientOnly var isExcludeSubFolders: Boolean = false,
    @ClientOnly var isDeleteEmptySubFolders: Boolean = false,
    @ClientOnly var outdated: Boolean = false
) {

    /**
     * Mark the field is only used for client, unrelated with server side.
     */
    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ClientOnly

    override fun toString(): String {
        return "Backup(backupId=$backupId, backupType=$backupType, targetNode=$targetNode(${targetNode.name()}), localFolder='$localFolder', backupName='$backupName', outdated=$outdated)"
    }
}