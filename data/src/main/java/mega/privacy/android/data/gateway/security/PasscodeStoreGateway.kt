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

    /**
     * Set passcode enabled
     *
     * @param enabled
     */
    suspend fun setPasscodeEnabledState(enabled: Boolean)

    /**
     * Monitor passcode enabled state
     *
     * @return
     */
    fun monitorPasscodeEnabledState(): Flow<Boolean?>

    /**
     * Set passcode timeout
     *
     * @param timeOutMilliseconds
     */
    suspend fun setPasscodeTimeout(timeOutMilliseconds: Long?)

    /**
     * Monitor passcode time out
     *
     * @return passcode timeout
     */
    fun monitorPasscodeTimeOut(): Flow<Long?>

    /**
     * Set last background time
     *
     * @param backgroundUTC
     */
    suspend fun setLastBackgroundTime(backgroundUTC: Long?)

    /**
     * Monitor last background time
     *
     * @return last background time
     */
    fun monitorLastBackgroundTime(): Flow<Long?>

    /**
     * Set passcode type
     *
     * @param passcodeType
     */
    suspend fun setPasscodeType(passcodeType: String?)

    /**
     * Monitor passcode type
     *
     * @return
     */
    fun monitorPasscodeType(): Flow<String?>

    /**
     * Set enable biometrics
     *
     * @param enabled
     */
    suspend fun setBiometricsEnabled(enabled: Boolean?)

    /**
     * Monitor biometric enabled state
     *
     * @return biometrics enabled state as flow
     */
    fun monitorBiometricEnabledState(): Flow<Boolean?>

    /**
     * Set last orientation
     *
     * @param orientation
     */
    suspend fun setOrientation(orientation: Int?)

    /**
     * Monitor orientation
     *
     * @return orientation as a flow
     */
    fun monitorOrientation(): Flow<String?>
}