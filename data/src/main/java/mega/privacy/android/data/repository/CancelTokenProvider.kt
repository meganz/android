package mega.privacy.android.data.repository

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

    /**
     * Get the current cancel token or creates a new one if it doesn't exist
     */
    fun getOrCreateCancelToken(): MegaCancelToken {
        return megaCancelToken ?: ((megaApiGateway.createCancelToken()).also {
            megaCancelToken = it
        })
    }

    /**
     * Cancel the current cancel token, if exists, and creates a new one
     */
    fun cancelAndCreateNewToken(): MegaCancelToken {
        cancelCurrentToken()
        return getOrCreateCancelToken()
    }

    /**
     * Cancel and invalidates the current cancel token, if exists
     */
    fun cancelCurrentToken() {
        megaCancelToken?.cancel()
        invalidateCurrentToken()
    }

    /**
     * Invalidates the current token, it won't be accessible anymore but won't be cancelled
     */
    fun invalidateCurrentToken() {
        megaCancelToken = null
    }
}