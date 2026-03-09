package mega.privacy.android.feature.sync.ui.megapicker

import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Node with a flag to indicate if it's disabled
 * @property node The node
 * @property isDisabled Flag to indicate if the node is disabled
 * @property backupId The backup ID for removal (if disabled due to existing sync/backup)
 * @property deviceName The device name for the dialog message
 */
internal data class TypedNodeUiModel(
    val node: TypedNode,
    val isDisabled: Boolean = false,
    val backupId: Long? = null,
    val deviceName: String? = null,
)
