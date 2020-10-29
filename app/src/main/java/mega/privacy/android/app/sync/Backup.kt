package mega.privacy.android.app.sync

import mega.privacy.android.app.utils.Constants.INVALID_NON_NULL_VALUE
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError

data class Backup(
    var backupId: Long,
    var backupType: Int,
    var targetNode: Long,
    var localFolder: String,
    var backupName: String,
    var state: Int = MegaApiJava.CU_SYNC_STATE_ACTIVE,
    var subState: Int = MegaError.API_OK,
    var extraData: String = INVALID_NON_NULL_VALUE,
    @ClientOnly var startTimestamp: Long = 0L,
    @ClientOnly var lastFinishTimestamp: Long = 0L,
    @ClientOnly var targetFolderPath: String = INVALID_NON_NULL_VALUE,
    @ClientOnly var isExcludeSubFolders: Boolean = false,
    @ClientOnly var isDeleteEmptySubFolders: Boolean = false,
    @ClientOnly var outdated: Boolean = false
) {

    @Target(AnnotationTarget.PROPERTY)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ClientOnly

    override fun toString(): String {
        return "Backup(backupId=$backupId, backupType=$backupType, targetNode=$targetNode(${targetNode.name()}), localFolder='$localFolder', backupName='$backupName', outdated=$outdated)"
    }
}