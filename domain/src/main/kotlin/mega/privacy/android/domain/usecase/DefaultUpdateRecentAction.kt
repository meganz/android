package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject

/**
 * Default get nodes from the recent action bucket
 */
class DefaultUpdateRecentAction @Inject constructor(
    private val getRecentActions: GetRecentActions,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UpdateRecentAction {

    override suspend fun invoke(
        currentBucket: RecentActionBucket,
        cachedActionList: List<RecentActionBucket>?,
    ): RecentActionBucket? = withContext(ioDispatcher) {

        val recentActions = getRecentActions()

        // Update the current bucket
        recentActions.firstOrNull { currentBucket.isSameBucket(it) }?.let {
            return@withContext it
        }

        // Compare the previous list of actions with the new list of recent actions
        // and only keep the actions from the updated list that differs from the previous cached one
        val filteredActions =
            cachedActionList?.let { cachedList ->
                recentActions.filter { it !in cachedList.filter { item -> item.isSameBucket(it) } }
            } ?: return@withContext null

        // The last one is the changed one
        return@withContext if (filteredActions.size == 1) filteredActions[0] else null
    }
}

/**
 * Compare two [RecentActionBucket]
 *
 * @param bucket the MegaRecentActionBucket to compare with the current object
 * @return true if the two MegaRecentActionBucket are the same
 */
fun RecentActionBucket.isSameBucket(bucket: RecentActionBucket): Boolean {
    return isMedia == bucket.isMedia &&
            isUpdate == bucket.isUpdate &&
            timestamp == bucket.timestamp &&
            parentHandle == bucket.parentHandle &&
            userEmail == bucket.userEmail
}
