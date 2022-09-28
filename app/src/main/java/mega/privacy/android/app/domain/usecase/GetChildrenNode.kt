package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import nz.mega.sdk.MegaNode

/**
 * Get children nodes of a parent node
 */
fun interface GetChildrenNode {
    /**
     * Get children nodes of a parent node
     *
     * @param parent Parent node
     * @param order Order for the returned list
     * @return Children nodes of the parent node
     */
    suspend operator fun invoke(parent: MegaNode, order: SortOrder?): List<MegaNode>
}