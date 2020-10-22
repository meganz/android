package mega.privacy.android.app.sync

import nz.mega.sdk.MegaApiJava


data class Backup(
    var backupId: Long,
    var backupType: Int,
    var targetNode: Long,
    var localFolder: String,
    var deviceId: String,
    var state: Int = MegaApiJava.STATE_PENDING,
    var subState: Int = MegaApiJava.STATE_PENDING,
    var extraData: String = "",
    @ClientOnly var startTimestamp: Long = 0L,
    @ClientOnly var lastFinishTimestamp: Long = 0L,
    @ClientOnly var targetFolderPath: String? = null,
    @ClientOnly var isExcludeSubFolders: Boolean = false,
    @ClientOnly var isDeleteEmptySubFolders: Boolean = false,
    @ClientOnly var name: String = "",
    @ClientOnly var outdated: Boolean = false
) {

    @Target(AnnotationTarget.PROPERTY)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ClientOnly

    override fun toString(): String {
        return "Backup(backupId=$backupId, backupType=$backupType, targetNode=$targetNode(${targetNode.name()}), localFolder='$localFolder', name='$name', outdated=$outdated)"
    }
}