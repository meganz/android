package mega.privacy.android.domain.entity.node

import mega.privacy.android.domain.entity.FolderType

interface TypedFolderNode : TypedNode, FolderNode{
    val type: FolderType
}
