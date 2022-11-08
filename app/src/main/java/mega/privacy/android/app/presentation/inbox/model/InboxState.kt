package mega.privacy.android.app.presentation.inbox.model

import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaNode

/**
 * Inbox UI State
 *
 * @property currentParentNodeId The current Parent Node ID
 * @property hideMultipleItemSelection Whether to hide the Multiple Item Selection or not
 * @property myBackupsFolderNodeId The My Backups Folder Node ID
 * @property nodes List of Inbox Nodes
 * @property shouldExitInbox Whether the User should leave the Inbox screen or not
 * @property triggerBackPress Whether the User has triggered a Back Press behavior or not
 */
data class InboxState(
    val currentParentNodeId: NodeId = NodeId(-1L),
    val hideMultipleItemSelection: Boolean = false,
    val myBackupsFolderNodeId: NodeId = NodeId(-1L),
    val nodes: List<MegaNode> = emptyList(),
    val shouldExitInbox: Boolean = false,
    val triggerBackPress: Boolean = false,
)