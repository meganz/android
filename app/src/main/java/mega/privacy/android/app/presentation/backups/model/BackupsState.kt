package mega.privacy.android.app.presentation.backups.model

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaNode

/**
 * The Backups UI State
 *
 * @property currentViewType The current [ViewType]
 * @property hideMultipleItemSelection Whether to hide the Multiple Item Selection or not
 * @property originalBackupsNodeId The Original Backups [NodeId] that was passed to open the Backups
 * page
 * @property rootBackupsFolderNodeId The User's Root Backups Folder [NodeId]
 * @property currentBackupsFolderNodeId The current Backups Folder [NodeId] (the Backups Folder that
 * is currently in view)
 * @property currentBackupsFolderName The Current Backups Folder name
 * @property nodes The list of Backups Nodes
 * @property shouldExitBackups Whether the User should leave the Backups page or not
 * @property triggerBackPress Whether the User has triggered a Back Press behavior or not
 * @property isPendingRefresh Whether a refresh of the Backup Contents is needed or not
 * @property isUserInRootBackupsFolderLevel Checks whether the User is currently in the Root Backups
 * Folder level. This is the highest Folder level in the User's Backups data hierarchy
 */
data class BackupsState(
    val currentViewType: ViewType = ViewType.LIST,
    val hideMultipleItemSelection: Boolean = false,
    val originalBackupsNodeId: NodeId = NodeId(-1L),
    val rootBackupsFolderNodeId: NodeId = NodeId(-1L),
    val currentBackupsFolderNodeId: NodeId = NodeId(-1L),
    val currentBackupsFolderName: String? = null,
    val nodes: List<MegaNode> = emptyList(),
    val shouldExitBackups: Boolean = false,
    val triggerBackPress: Boolean = false,
    val isPendingRefresh: Boolean = false,
) {
    val isUserInRootBackupsFolderLevel =
        currentBackupsFolderNodeId == rootBackupsFolderNodeId
}