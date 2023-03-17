package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.entity.node.Node


/**
 * Get children nodes of the rubbish bin parent handle
 */
interface GetRubbishBinChildren {
    /**
     * Get children nodes of the rubbish bin parent handle
     *
     * @param parentHandle
     * @return Children nodes of the parent handle, null if cannot be retrieved
     */
    suspend operator fun invoke(parentHandle: Long): List<Node>
}