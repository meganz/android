package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.RecentActionsMapper
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRequest
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [RecentActionsRepository]
 */
internal class DefaultRecentActionsRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val recentActionsMapper: RecentActionsMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RecentActionsRepository {

    override suspend fun getRecentActions() =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.getRecentActionsAsync(DAYS, MAX_NODES,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = onGetRecentActionsAsyncFinish(continuation)
                    ))
            }
        }

    private fun onGetRecentActionsAsyncFinish(continuation: Continuation<List<MegaRecentActionBucket>>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaChatError.ERROR_OK) {
                val recentActionsList = recentActionsMapper(
                    request.recentActions,
                    ::provideRecentActionBucket)

                continuation.resumeWith(Result.success(recentActionsList))
            } else {
                continuation.failWithError(error)
            }
        }

    /**
     * Provide the [MegaRecentActionBucket] required copy.
     */
    private fun provideRecentActionBucket(recentActionBucket: MegaRecentActionBucket) =
        megaApiGateway.copyBucket(recentActionBucket)

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