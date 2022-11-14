package mega.privacy.android.domain.entity.node

import mega.privacy.android.domain.entity.FolderType

internal class DefaultTypedFolderNode(
    private val folderNode: FolderNode,
    override val type: FolderType
) : TypedFolderNode, FolderNode by folderNode