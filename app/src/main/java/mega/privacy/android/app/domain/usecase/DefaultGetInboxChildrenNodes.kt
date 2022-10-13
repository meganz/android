package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.HasInboxChildren
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default Use Case implementation of [GetInboxChildrenNodes]
 *
 * @property getChildrenNode [GetChildrenNode]
 * @property getCloudSortOrder [GetCloudSortOrder]
 * @property getInboxNode [GetInboxNode]
 */
class DefaultGetInboxChildrenNodes @Inject constructor(
    private val getChildrenNode: GetChildrenNode,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getInboxNode: GetInboxNode,
    private val hasInboxChildren: HasInboxChildren,
) : GetInboxChildrenNodes {

    /**
     * Retrieve the list of Inbox Children Nodes
     *
     * @return the list of Inbox Children Nodes, or an empty list if the Inbox Node does not exist
     */
    override suspend operator fun invoke(): List<MegaNode> =
        if (hasInboxChildren()) {
            getInboxNode()?.let { inboxNode ->
                getChildrenNode(
                    parent = inboxNode,
                    order = getCloudSortOrder(),
                )
            } ?: emptyList()
        } else {
            emptyList()
        }
}