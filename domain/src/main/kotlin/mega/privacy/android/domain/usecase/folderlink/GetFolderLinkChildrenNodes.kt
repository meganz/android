package mega.privacy.android.domain.usecase.folderlink

import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Get children nodes of a parent node
 */
fun interface GetFolderLinkChildrenNodes {
    /**
     * Get children nodes of a parent node
     *
     * @param parentHandle  Parent handle
     * @param order         Order for the returned list
     * @return Children nodes of the parent node
     */
    suspend operator fun invoke(parentHandle: Long, order: Int?): List<TypedNode>
}