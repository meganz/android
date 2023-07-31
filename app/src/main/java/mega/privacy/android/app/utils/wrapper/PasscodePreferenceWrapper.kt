package mega.privacy.android.app.utils.wrapper

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Passcode preference wrapper
 *
 * @property databaseHandler
 * @property ioDispatcher
 */
@Singleton
class PasscodePreferenceWrapper @Inject constructor(
    private val databaseHandler: DatabaseHandler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApi: MegaApiGateway,
) {
    /**
     * Is passcode enabled
     *
     */
    suspend fun isPasscodeEnabled() =
        withContext(ioDispatcher) { databaseHandler.preferences?.passcodeLockEnabled.toBoolean() }

    /**
     * Get passcode
     *
     * @return
     */
    suspend fun getPasscode(): String? =
        withContext(ioDispatcher) { databaseHandler.preferences?.passcodeLockCode.takeUnless { it.isNullOrEmpty() } }

    /**
     * Get passcode time out
     *
     */
    suspend fun getPasscodeTimeOut() =
        withContext(ioDispatcher) { databaseHandler.passcodeRequiredTime }

    /**
     * Set passcode time out
     *
     * @param passcodeRequireTime
     */
    suspend fun setPasscodeTimeOut(passcodeRequireTime: Int) {
        withContext(ioDispatcher) { databaseHandler.passcodeRequiredTime = passcodeRequireTime }
    }

    /**
     * Set fingerprint lock enabled
     *
     * @param enabled
     */
    suspend fun setFingerprintLockEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            databaseHandler.isFingerprintLockEnabled = enabled
        }
    }

    /**
     * Is finger print lock enabled
     *
     */
    suspend fun isFingerPrintLockEnabled() =
        withContext(ioDispatcher) { databaseHandler.isPasscodeLockEnabled }

    /**
     * Set passcode enabled
     *
     * @param enable
     */
    suspend fun setPasscodeEnabled(enable: Boolean) {
        withContext(ioDispatcher) { databaseHandler.isPasscodeLockEnabled = enable }
    }

    /**
     * Set passcode lock type
     *
     * @param type
     */
    suspend fun setPasscodeLockType(type: String) {
        withContext(ioDispatcher) { databaseHandler.passcodeLockType = type }
    }

    /**
     * Set passcode
     *
     * @param passcode
     */
    suspend fun setPasscode(passcode: String) {
        withContext(ioDispatcher) { databaseHandler.passcodeLockCode = passcode }
    }

    /**
     * Get failed attempts count
     *
     * @return
     */
    suspend fun getFailedAttemptsCount(): Int {
        return withContext(ioDispatcher) { databaseHandler.attributes?.attempts ?: 0 }
    }

    /**
     * Get passcode type
     *
     * @return
     */
    suspend fun getPasscodeType(): String =
        withContext(ioDispatcher) {
            databaseHandler.preferences?.passcodeLockType.takeUnless { it.isNullOrEmpty() } ?: "4"
        }

    /**
     * Set failed attempts count
     *
     * @param attempts
     */
    suspend fun setFailedAttemptsCount(attempts: Int) {
        withContext(ioDispatcher) { databaseHandler.setAttrAttempts(attempts) }
    }

    /**
     * Check password
     *
     * @param password
     * @return
     */
    suspend fun checkPassword(password: String): Boolean =
        withContext(ioDispatcher){ megaApi.isCurrentPassword(password) }


}