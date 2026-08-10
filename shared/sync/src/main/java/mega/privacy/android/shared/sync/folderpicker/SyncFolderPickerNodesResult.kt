package mega.privacy.android.shared.sync.folderpicker

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Sync specific restrictions of the folder currently displayed by the MEGA folder picker.
 *
 * @property restrictedNodes The children that cannot be selected, keyed by node id
 * @property isSelectEnabled Whether the current folder itself can be selected
 */
data class SyncFolderPickerNodesResult(
    val restrictedNodes: Map<NodeId, SyncFolderPickerRestrictedNode> = emptyMap(),
    val isSelectEnabled: Boolean = false,
)

/**
 * A child node that cannot be selected in the MEGA folder picker.
 *
 * @property nodeId The node id
 * @property name The node name
 * @property isUsedBySyncOrBackup True when the node is already used by a sync or backup
 * @property backupId The backup id when the node is used by a backup of another device,
 * which allows the user to remove that connection
 * @property deviceName The name of the device the backup belongs to
 */
data class SyncFolderPickerRestrictedNode(
    val nodeId: NodeId,
    val name: String,
    val isUsedBySyncOrBackup: Boolean,
    val backupId: Long? = null,
    val deviceName: String? = null,
)
