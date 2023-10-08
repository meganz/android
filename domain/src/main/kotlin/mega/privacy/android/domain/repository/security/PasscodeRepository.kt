package mega.privacy.android.domain.repository.security

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.entity.passcode.PasscodeType

/**
 * Passcode repository
 */
interface PasscodeRepository {

    /**
     * Monitor failed attempts
     *
     * @return a count of failed attempts
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
     * @return The current passcode if it exists
     */
    suspend fun getPasscode(): String?

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
    fun monitorLockState(): Flow<Boolean?>


    /**
     * Set last paused time
     *
     * @param lastPausedUTCTimestamp
     */
    suspend fun setLastPausedTime(lastPausedUTCTimestamp: Long)

    /**
     * Get last paused time
     *
     * @return last paused time
     */
    suspend fun getLastPausedTime(): Long?

    /**
     * Is passcode enabled
     *
     * @return
     */
    fun monitorIsPasscodeEnabled(): Flow<Boolean?>

    /**
     * Set passcode enabled
     *
     * @param enabled
     */
    suspend fun setPasscodeEnabled(enabled: Boolean)

    /**
     * Monitor passcode time out
     *
     * @return
     */
    fun monitorPasscodeTimeOut(): Flow<PasscodeTimeout?>

    /**
     * Set passcode time out
     *
     * @param passcodeTimeout
     */
    suspend fun setPasscodeTimeOut(passcodeTimeout: PasscodeTimeout)

    /**
     * Monitor passcode type
     *
     * @return flow of passcode type
     */
    fun monitorPasscodeType(): Flow<PasscodeType?>

    /**
     * Set passcode type
     *
     * @param passcodeType
     */
    suspend fun setPasscodeType(passcodeType: PasscodeType?)
}