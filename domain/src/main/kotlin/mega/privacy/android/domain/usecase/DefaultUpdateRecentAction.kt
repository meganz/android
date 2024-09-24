package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.recentactions.GetRecentActionsUseCase
import javax.inject.Inject

/**
 * Default get nodes from the recent action bucket
 */
class DefaultUpdateRecentAction @Inject constructor(
    private val getRecentActionsUseCase: GetRecentActionsUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UpdateRecentAction {

    override suspend fun invoke(
        currentBucket: RecentActionBucket,
        cachedActionList: List<RecentActionBucket>?,
        excludeSensitives: Boolean,
    ): RecentActionBucket? = withContext(ioDispatcher) {

        val recentActions = getRecentActionsUseCase(
            excludeSensitives = excludeSensitives,
        )

        // Update the current bucket
        recentActions.firstOrNull { currentBucket.identifier == it.identifier }?.let {
            return@withContext it
        }

        if (cachedActionList == null) return@withContext null

        // Compare the previous list of actions with the new list of recent actions
        // and only keep the actions from the updated list that differs from the previous cached one
        val filteredActions = recentActions.filter { bucket ->
            cachedActionList.none { it.identifier == bucket.identifier }
        }

        // We return if only one bucket is changed, cause if more than one is updated or created,
        // we can't know which one to return as SDK API doesn't provide any unique id/key for a bucket
        return@withContext if (filteredActions.size == 1) filteredActions.first() else null
    }

}
