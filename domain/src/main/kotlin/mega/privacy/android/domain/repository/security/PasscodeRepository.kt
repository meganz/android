package mega.privacy.android.domain.repository.security

import kotlinx.coroutines.flow.Flow

/**
 * Passcode repository
 */
interface PasscodeRepository {

    /**
     * Monitor failed attempts
     *
     * @return a count of failed attempts
     */
    fun monitorFailedAttempts(): Flow<Int>


    /**
     * Set failed attempts
     *
     * @param attempts
     */
    suspend fun setFailedAttempts(attempts: Int)

    /**
     * Check passcode
     *
     * @param passcode
     * @return true if correct, else false
     */
    suspend fun checkPasscode(passcode: String): Boolean

    /**
     * Check password
     *
     * @param password
     * @return true if correct, else false
     */
    suspend fun checkPassword(password: String): Boolean

    /**
     * Set locked
     *
     * @param locked
     */
    suspend fun setLocked(locked: Boolean)


    /**
     * Monitor lock state
     *
     * @return flow of locked state - true for locked, false for not locked
     */
    fun monitorLockState(): Flow<Boolean>

}