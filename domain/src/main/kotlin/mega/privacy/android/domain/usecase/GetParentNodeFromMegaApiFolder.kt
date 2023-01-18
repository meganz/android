package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * The use case for getting parent node by handle from MegaApiFolder
 */
fun interface GetParentNodeFromMegaApiFolder {

    /**
     * Get parent node by handle from MegaApiFolder
     *
     * @param parentHandle node handle
     * @return parent node
     */
    suspend operator fun invoke(parentHandle: Long): UnTypedNode?
}