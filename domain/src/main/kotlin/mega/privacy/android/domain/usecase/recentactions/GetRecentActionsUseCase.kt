package mega.privacy.android.domain.usecase.recentactions

import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.RecentActionsRepository
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * Get a list of recent actions
 */
class GetRecentActionsUseCase @Inject constructor(
    private val recentActionsRepository: RecentActionsRepository,
    private val addNodeType: AddNodeType,
) {

    /**
     * Get a list of recent actions
     *
     * @return a list of recent actions
     */
    suspend operator fun invoke(): List<RecentActionBucket> {
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
