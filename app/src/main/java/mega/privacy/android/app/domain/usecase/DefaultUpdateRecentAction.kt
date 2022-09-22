package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaRecentActionBucket
import javax.inject.Inject

/**
 * Default get nodes from the recent action bucket
 */
class DefaultUpdateRecentAction @Inject constructor(
    private val getRecentActions: GetRecentActions,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UpdateRecentAction {

    override suspend fun invoke(
        currentBucket: MegaRecentActionBucket,
        cachedActionList: List<MegaRecentActionBucket>?,
    ): MegaRecentActionBucket? = withContext(ioDispatcher) {

        val recentActions = getRecentActions()

        // Update the current bucket
        recentActions.firstOrNull { isSameBucket(it, currentBucket) }?.let {
            return@withContext it
        }

        // Compare the previous list of actions with the new list of recent actions
        // and only keep the actions from the previous list that differs from the new one
        cachedActionList?.filter { it !in recentActions.filter { item -> isSameBucket(it, item) } }

        // The last one is the changed one
        return@withContext if (recentActions.size == 1) recentActions[0] else null
    }

    /**
     * Compare two MegaRecentActionBucket
     *
     * @param selected the first MegaRecentActionBucket to compare
     * @param other the second MegaRecentActionBucket to compare
     * @return true if the two MegaRecentActionBucket are the same
     */
    private fun isSameBucket(
        selected: MegaRecentActionBucket,
        other: MegaRecentActionBucket,
    ): Boolean {
        return selected.isMedia == other.isMedia &&
                selected.isUpdate == other.isUpdate &&
                selected.timestamp == other.timestamp &&
                selected.parentHandle == other.parentHandle &&
                selected.userEmail == other.userEmail
    }
}