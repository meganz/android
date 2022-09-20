package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaRecentActionBucket
import javax.inject.Inject

/**
 * Default get nodes from the recent action bucket
 */
class DefaultUpdateRecentAction @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UpdateRecentAction {

    override suspend fun invoke(
        currentBucket: MegaRecentActionBucket,
        cachedActionList: List<MegaRecentActionBucket>?,
    ): MegaRecentActionBucket? = withContext(ioDispatcher) {

        val recentActions = megaApi.recentActions

        // Update the current bucket
        recentActions.firstOrNull { isSameBucket(it, currentBucket) }?.let {
            return@withContext it
        }

        // Compare the list of recentActions with the new list of recent actions
        // and only keep the actions that differs from each other
        cachedActionList?.forEach { b ->
            val iterator = recentActions.iterator()
            while (iterator.hasNext()) {
                if (isSameBucket(iterator.next(), b)) {
                    iterator.remove()
                }
            }
        }

        // The last one is the changed one
        if (recentActions.size == 1) {
            return@withContext recentActions[0]
        }

        return@withContext null
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