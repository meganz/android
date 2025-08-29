package mega.privacy.android.core.nodecomponents.dialog.sharefolder

import mega.privacy.android.domain.entity.node.TypedNode

/**
 * State for Share Folder Dialog
 */
data class ShareFolderDialogState(
    val dialogType: ShareFolderDialogType? = null,
)

sealed class ShareFolderDialogType(val nodes: List<TypedNode>) {
    data class Multiple(val nodeList: List<TypedNode>) : ShareFolderDialogType(nodeList)

    data class Single(val nodeList: List<TypedNode>) : ShareFolderDialogType(nodeList)
}
