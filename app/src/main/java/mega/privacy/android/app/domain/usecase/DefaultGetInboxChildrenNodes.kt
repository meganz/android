package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
 * @property monitorNodeUpdates [MonitorNodeUpdates]
 */
class DefaultGetInboxChildrenNodes @Inject constructor(
    private val getChildrenNode: GetChildrenNode,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getInboxNode: GetInboxNode,
    private val hasInboxChildren: HasInboxChildren,
    private val monitorNodeUpdates: MonitorNodeUpdates,
) : GetInboxChildrenNodes {

    /**
     * Retrieve the list of Inbox Children Nodes
     *
     * @return the list of Inbox Children Nodes, or an empty list if the Inbox Node does not exist
     */
    override fun invoke(): Flow<List<MegaNode>> {
        return flow {
            emit(getInboxChildrenNodes())
            emitAll(monitorNodeUpdates().map { getInboxChildrenNodes() })
        }
    }

    /**
     * Returns the list of Inbox Children Nodes
     *
     * @return the list of Inbox Children Nodes, or an empty list if the parent Inbox Node
     * is null or has no Children Nodes
     */
    private suspend fun getInboxChildrenNodes(): List<MegaNode> =
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