package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
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
 * @property ioDispatcher [CoroutineDispatcher]
 */
class DefaultGetInboxChildrenNodes @Inject constructor(
    private val getChildrenNode: GetChildrenNode,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getInboxNode: GetInboxNode,
    private val hasInboxChildren: HasInboxChildren,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GetInboxChildrenNodes {

    /**
     * Retrieve the list of Inbox Children Nodes
     *
     * @return the list of Inbox Children Nodes, or an empty list if the Inbox Node does not exist
     */
    override suspend operator fun invoke(): List<MegaNode> =
        withContext(ioDispatcher) {
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
}