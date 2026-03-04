package mega.privacy.android.feature.sync.domain.entity.megapicker

import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Domain model for a node in the Mega Picker folder list with usage/connection info.
 *
 * @property node The folder or file node
 * @property isDisabled True if the folder is excluded or used by sync/backup (any device)
 * @property subtitle Device name to show in list for folders connected to another device
 * @property backupId Backup ID for remove-connection action (only set for other-device folders)
 * @property deviceName Device name for the remove-connection
 */
internal data class MegaPickerNodeInfo(
    val node: TypedNode,
    val isDisabled: Boolean = false,
    val subtitle: String? = null,
    val backupId: Long? = null,
    val deviceName: String? = null,
)
