package mega.privacy.android.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.data.gateway.api.MegaApiGateway
import nz.mega.sdk.MegaCancelToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Common source to get cancel token. Only one cancel token related query should exist at a given time, so this is the common source for it.
 */
@Singleton
internal class CancelTokenProvider @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
) {

    private var megaCancelToken: MegaCancelToken? = null
    private val mutex = Mutex()

    /**
     * Get the current cancel token or creates a new one if it doesn't exist or it's already cancelled
     * @return [MegaCancelToken] to be used for sdk calls, please don't cancel it directly, use [cancelCurrentToken]
     */
    suspend fun getOrCreateCancelToken(): MegaCancelToken =
        mutex.withLock {
            megaCancelToken?.takeIf {
                !it.isCancelled //should not be necessary, but we can't guarantee any direct cancellation of [MegaCancelToken]
            } ?: ((megaApiGateway.createCancelToken()).also {
                megaCancelToken = it
            })
        }

    /**
     * Cancel the current cancel token, if exists, and creates a new one
     * @return [MegaCancelToken] to be used for sdk calls, please don't cancel it directly, use [cancelCurrentToken]
     */
    suspend fun cancelAndCreateNewToken(): MegaCancelToken {
        cancelCurrentToken()
        return getOrCreateCancelToken()
    }

    /**
     * Cancel and invalidates the current cancel token, if exists
     */
    suspend fun cancelCurrentToken() {
        megaCancelToken?.cancel()
        invalidateCurrentToken()
    }

    /**
     * Invalidates the current token, it won't be accessible anymore but won't be cancelled
     */
    suspend fun invalidateCurrentToken() {
        mutex.withLock {
            megaCancelToken = null
        }
    }
}