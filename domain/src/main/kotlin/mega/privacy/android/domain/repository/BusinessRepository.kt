package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus

/**
 * Business repository
 */
interface BusinessRepository {
    /**
     * Get business status
     *
     * @return current business account status
     */
    suspend fun getBusinessStatus(): BusinessAccountStatus

    /**
     * Checks whether the user's Business Account is currently active or not
     *
     * @return True if the user's Business Account is currently active, or
     * false if inactive or if the user is not under a Business Account
     */
    suspend fun isBusinessAccountActive(): Boolean

    /**
     * Checks whether the user's Account is a master business account
     *
     * @return True if the user's account is a master business account
     */
    suspend fun isMasterBusinessAccount(): Boolean

    /**
     * Broadcast business account expired
     */
    suspend fun broadcastBusinessAccountExpired()

    /**
     * Monitor business account expired events
     *
     * @return a flow that emits each time a new business account expired error is received
     */
    fun monitorBusinessAccountExpired(): Flow<Unit>
}