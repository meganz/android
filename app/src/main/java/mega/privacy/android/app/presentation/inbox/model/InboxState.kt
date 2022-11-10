package mega.privacy.android.app.presentation.inbox.model

import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaNode

/**
 * Inbox UI State
 *
 * @property hideMultipleItemSelection Whether to hide the Multiple Item Selection or not
 * @property inboxNodeId The current Inbox Node ID
 * @property nodes List of Inbox Nodes
 * @property shouldExitInbox Whether the User should leave the Inbox screen or not
 * @property triggerBackPress Whether the User has triggered a Back Press behavior or not
 */
data class InboxState(
    val hideMultipleItemSelection: Boolean = false,
    val inboxNodeId: NodeId = NodeId(-1L),
    val nodes: List<MegaNode> = emptyList(),
    val shouldExitInbox: Boolean = false,
    val triggerBackPress: Boolean = false,
)