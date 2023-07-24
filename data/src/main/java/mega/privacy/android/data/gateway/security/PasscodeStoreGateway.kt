package mega.privacy.android.data.gateway.security

import kotlinx.coroutines.flow.Flow

/**
 * Passcode store gateway
 *
 * @constructor Create empty Passcode store gateway
 */
interface PasscodeStoreGateway {
    /**
     * Get failed attempts flow
     *
     * @return flow of latest failed attempts count
     */
    fun monitorFailedAttempts(): Flow<Int?>

    /**
     * Set failed attempts
     *
     * @param attempts
     */
    suspend fun setFailedAttempts(attempts: Int)

    /**
     * Set passcode
     *
     * @param passcode
     */
    suspend fun setPasscode(passcode: String?)

    /**
     * Get passcode
     *
     * @return current passcode
     */
    suspend fun getPasscode(): String?

    /**
     * Set locked state
     *
     * @param state
     */
    suspend fun setLockedState(state: Boolean)

    /**
     * Get lock state flow
     *
     * @return flow of latest locked state
     */
    fun monitorLockState(): Flow<Boolean?>
}