package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * The use case for getting root node from MegaApiFolder
 */
fun interface GetRootNodeFromMegaApiFolder {

    /**
     * Get root node from MegaApiFolder
     *
     * @return root node
     */
    suspend operator fun invoke(): UnTypedNode?
}