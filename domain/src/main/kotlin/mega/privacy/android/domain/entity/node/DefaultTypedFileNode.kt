package mega.privacy.android.domain.entity.node

internal data class DefaultTypedFileNode(
    private val fileNode: FileNode,
) : TypedFileNode, FileNode by fileNode