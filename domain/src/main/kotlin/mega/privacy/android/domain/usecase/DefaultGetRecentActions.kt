package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.RecentActionsRepository
import javax.inject.Inject

/**
 * Default Implementation of [GetRecentActions]
 */
class DefaultGetRecentActions @Inject constructor(
    private val recentActionsRepository: RecentActionsRepository,
    private val addNodeType: AddNodeType,
) : GetRecentActions {
    override suspend fun invoke(): List<RecentActionBucket> {
        val result = recentActionsRepository.getRecentActions().map {
            val typedNodes = it.nodes.map { node ->
                addNodeType(node)
            }.filterIsInstance<TypedFileNode>()
            return@map RecentActionBucket(
                it.timestamp,
                it.userEmail,
                it.parentHandle,
                it.isUpdate,
                it.isMedia,
                typedNodes,
            )
        }
        return result
    }

}
