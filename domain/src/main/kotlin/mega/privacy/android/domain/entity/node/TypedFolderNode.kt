package mega.privacy.android.domain.entity.node

import mega.privacy.android.domain.entity.FolderType

/**
 * Typed folder node
 *
 */
interface TypedFolderNode : TypedNode, FolderNode{
    /**
     * Type
     */
    val type: FolderType
}
