package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.HasBackupsChildren
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import javax.inject.Inject

/**
 * Default Use Case implementation of [GetBackupsChildrenNodes]
 *
 * @property getChildrenNode [GetChildrenNode]
 * @property getCloudSortOrder [GetCloudSortOrder]
 * @property getBackupsNode [GetBackupsNode]
 * @property monitorNodeUpdatesUseCase [MonitorNodeUpdatesUseCase]
 */
class DefaultGetBackupsChildrenNodes @Inject constructor(
    private val getChildrenNode: GetChildrenNode,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getBackupsNode: GetBackupsNode,
    private val hasBackupsChildren: HasBackupsChildren,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
) : GetBackupsChildrenNodes {

    /**
     * Invocation function
     *
     * @return the list of Backups Children Nodes, or an empty list if the Backups Node does not exist
     */
    override fun invoke() = flow {
        emit(getBackupsChildrenNodes())
        emitAll(monitorNodeUpdatesUseCase().map { getBackupsChildrenNodes() })
    }

    /**
     * Returns the list of Backups Children Nodes
     *
     * @return the list of Backups Children Nodes, or an empty list if the parent Backups Node
     * is null or has no Children Nodes
     */
    private suspend fun getBackupsChildrenNodes() = takeIf { hasBackupsChildren() }
        ?.run { getBackupsNode() }
        ?.let { backupsNode ->
            getChildrenNode(
                parent = backupsNode,
                order = getCloudSortOrder(),
            )
        }.orEmpty()
}
