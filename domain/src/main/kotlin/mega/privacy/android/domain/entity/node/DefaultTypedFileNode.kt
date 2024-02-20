package mega.privacy.android.domain.entity.node

import kotlinx.serialization.Serializable

@Serializable
internal data class DefaultTypedFileNode(
    private val fileNode: FileNode,
) : TypedFileNode, FileNode by fileNode