package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.node.TypedFolderNode

/**
 * Creates a new share key for the folder if there is no share key already created.
 */
interface CreateShareKey {
    /**
     * Creates a new share key for the folder if there is no share key already created.
     * @param node : [TypedFolderNode]
     */
    suspend operator fun invoke(node: TypedFolderNode)
}