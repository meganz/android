package mega.privacy.android.feature.sync.domain.entity.megapicker

import mega.privacy.android.domain.entity.node.Node

/**
 * Result of loading folder nodes for the Mega Picker.
 *
 * @property currentFolder The folder whose children are listed
 * @property nodes The list of node info (with disabled/connection state)
 * @property isSelectEnabled Whether the "Select folder" action is enabled for currentFolder
 */
internal data class MegaPickerFolderResult(
    val currentFolder: Node,
    val nodes: List<MegaPickerNodeInfo>,
    val isSelectEnabled: Boolean,
)
