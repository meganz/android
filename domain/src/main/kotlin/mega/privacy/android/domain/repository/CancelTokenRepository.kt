package mega.privacy.android.domain.repository

/**
 * Cancel token repository
 */
interface CancelTokenRepository {

    /**
     * Cancel and invalidates the current cancel token, if exists
     */
    suspend fun cancelCurrentToken()

    /**
     * Invalidates the current token, it won't be accessible anymore but won't be cancelled
     */
    suspend fun invalidateCurrentToken()
}