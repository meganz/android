package mega.privacy.android.shared.sync.folderpicker

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Contract used by the cloud explorer to apply the sync specific logic of the MEGA folder picker
 * when it is used to select the remote folder of a new sync or the destination of a stopped
 * backup.
 *
 * The implementation lives in the sync feature module, where all the sync domain logic
 * (sync repository, synced folders, backup connections, selected folder propagation) is defined.
 */
interface SyncFolderPickerHandler {

    /**
     * Monitors the children of [folderId] and emits which of them are restricted (cannot be
     * selected because they are already used by a sync or backup, or are reserved folders such
     * as Camera Uploads, Media Uploads or My Chat Files) together with whether the current
     * folder itself can be selected.
     *
     * @param folderId The folder whose children are being displayed
     * @param isStopBackup True when picking the destination to move a stopped backup folder to
     * @param stopBackupFolderName The name of the backup folder being moved, used to disable
     * selection when a node with the same name already exists in the current folder
     */
    fun monitorPickerNodes(
        folderId: NodeId,
        isStopBackup: Boolean,
        stopBackupFolderName: String?,
    ): Flow<SyncFolderPickerNodesResult>

    /**
     * Checks whether [folderId] is already used by a sync or backup on any device.
     *
     * @return A localized conflict message to be shown to the user, or null if the folder
     * is not in conflict
     */
    suspend fun getFolderUsageConflictMessage(folderId: NodeId): String?

    /**
     * Validates whether [folderId] can be synced through the SDK.
     *
     * @return Null when the folder is syncable, otherwise the string resource of the error
     * message to be shown to the user
     */
    @StringRes
    suspend fun validateNodeSyncability(folderId: NodeId): Int?

    /**
     * Checks whether a node named [folderName] already exists inside [parentId].
     */
    suspend fun folderNameExists(parentId: NodeId, folderName: String): Boolean

    /**
     * Propagates the picked folder to the sync feature, which monitors the selection to
     * continue the sync creation or stop backup flow.
     *
     * @return True if the folder was saved, false if the node no longer exists
     */
    suspend fun saveSelectedFolder(folderId: NodeId): Boolean

    /**
     * Removes the sync or backup connection of another device identified by [backupId].
     */
    suspend fun removeFolderConnection(backupId: Long)
}
