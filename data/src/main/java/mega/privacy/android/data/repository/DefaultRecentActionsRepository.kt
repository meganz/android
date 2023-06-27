package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.recentactions.RecentActionBucketMapper
import mega.privacy.android.data.mapper.recentactions.RecentActionsMapper
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.RecentActionsRepository
import nz.mega.sdk.MegaRecentActionBucket
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [RecentActionsRepository]
 */
internal class DefaultRecentActionsRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val recentActionsMapper: RecentActionsMapper,
    private val recentActionBucketMapper: RecentActionBucketMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RecentActionsRepository {

    override suspend fun getRecentActions() = withContext(ioDispatcher) {
        runCatching {
            val list = getMegaRecentAction().map {
                recentActionBucketMapper.invoke(
                    it,
                )
            }
            return@withContext list
        }.onFailure {
            Timber.e(it)
        }
        return@withContext emptyList<RecentActionBucketUnTyped>()
    }

    private suspend fun getMegaRecentAction(): List<MegaRecentActionBucket> =
        withContext(ioDispatcher) {
            val result = suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("getMegaRecentAction") {
                    it.recentActions
                }
                megaApiGateway.getRecentActionsAsync(DAYS, MAX_NODES, listener)
                continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
            }
            recentActionsMapper(
                result,
                ::copyRecentActionBucket
            )
        }


    /**
     * Provide the [MegaRecentActionBucket] required copy.
     */
    private suspend fun copyRecentActionBucket(recentActionBucket: MegaRecentActionBucket) =
        withContext(ioDispatcher) {
            megaApiGateway.copyBucket(recentActionBucket)
        }


    companion object {
        /**
         * Default and recommended value for getting recent actions in the last days.
         */
        private const val DAYS = 30L

        /**
         * Default and recommended value for getting recent actions for a maximum value of nodes.
         */
        private const val MAX_NODES = 500L
    }
}
