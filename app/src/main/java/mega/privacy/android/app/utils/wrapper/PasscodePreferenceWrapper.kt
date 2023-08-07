package mega.privacy.android.app.utils.wrapper

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.PIN_4
import mega.privacy.android.app.utils.Constants.PIN_6
import mega.privacy.android.app.utils.Constants.PIN_ALPHANUMERIC
import mega.privacy.android.app.utils.Constants.REQUIRE_PASSCODE_INVALID
import mega.privacy.android.app.utils.PasscodeUtil.Companion.REQUIRE_PASSCODE_AFTER_10S
import mega.privacy.android.app.utils.PasscodeUtil.Companion.REQUIRE_PASSCODE_AFTER_1M
import mega.privacy.android.app.utils.PasscodeUtil.Companion.REQUIRE_PASSCODE_AFTER_2M
import mega.privacy.android.app.utils.PasscodeUtil.Companion.REQUIRE_PASSCODE_AFTER_30S
import mega.privacy.android.app.utils.PasscodeUtil.Companion.REQUIRE_PASSCODE_AFTER_5M
import mega.privacy.android.app.utils.PasscodeUtil.Companion.REQUIRE_PASSCODE_AFTER_5S
import mega.privacy.android.app.utils.PasscodeUtil.Companion.REQUIRE_PASSCODE_IMMEDIATE
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.security.PasscodeRepository
import mega.privacy.android.domain.usecase.MonitorPasscodeLockPreferenceUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
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
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorPasscodeLockPreferenceUseCase: MonitorPasscodeLockPreferenceUseCase,
    private val passcodeRepository: PasscodeRepository,
    private val accountRepository: AccountRepository,
) {
    /**
     * Is passcode enabled
     *
     */
    suspend fun isPasscodeEnabled() =
        if (newImplementation()) {
            monitorPasscodeLockPreferenceUseCase().first()
        } else {
            withContext(ioDispatcher) { databaseHandler.preferences?.passcodeLockEnabled.toBoolean() }
        }

    /**
     * Get passcode
     *
     * @return
     */
    suspend fun getPasscode(): String? =
        if (newImplementation()) {
            passcodeRepository.getPasscode()
        } else {
            withContext(ioDispatcher) { databaseHandler.preferences?.passcodeLockCode.takeUnless { it.isNullOrEmpty() } }
        }

    /**
     * Get passcode time out
     *
     */
    suspend fun getPasscodeTimeOut() =
        if (newImplementation()) {
            passcodeRepository.monitorPasscodeTimeOut().map {
                when (it) {
                    PasscodeTimeout.Immediate -> REQUIRE_PASSCODE_IMMEDIATE
                    is PasscodeTimeout.TimeSpan -> getTimeoutTime(it.milliseconds.toInt())
                    null -> Constants.REQUIRE_PASSCODE_INVALID
                }
            }.first()
        } else {
            withContext(ioDispatcher) { databaseHandler.passcodeRequiredTime }
        }

    private fun getTimeoutTime(milliseconds: Int): Int {
        return when {
            milliseconds < REQUIRE_PASSCODE_AFTER_10S -> REQUIRE_PASSCODE_AFTER_5S
            milliseconds < REQUIRE_PASSCODE_AFTER_30S -> REQUIRE_PASSCODE_AFTER_10S
            milliseconds < REQUIRE_PASSCODE_AFTER_1M -> REQUIRE_PASSCODE_AFTER_30S
            milliseconds < REQUIRE_PASSCODE_AFTER_2M -> REQUIRE_PASSCODE_AFTER_1M
            milliseconds < REQUIRE_PASSCODE_AFTER_5M -> REQUIRE_PASSCODE_AFTER_2M
            else -> REQUIRE_PASSCODE_AFTER_5M
        }
    }


    /**
     * Set passcode time out
     *
     * @param passcodeRequireTime
     */
    suspend fun setPasscodeTimeOut(passcodeRequireTime: Int) {
        if (newImplementation()) {
            val timeout = getTimeout(passcodeRequireTime)
            passcodeRepository.setPasscodeTimeOut(timeout)
        } else {
            withContext(ioDispatcher) { databaseHandler.passcodeRequiredTime = passcodeRequireTime }
        }
    }

    private fun getTimeout(timeSpan: Int) = when (timeSpan) {
        REQUIRE_PASSCODE_IMMEDIATE -> PasscodeTimeout.Immediate
        REQUIRE_PASSCODE_INVALID -> PasscodeTimeout.TimeSpan(REQUIRE_PASSCODE_AFTER_30S.toLong())
        else -> PasscodeTimeout.TimeSpan(timeSpan.toLong())
    }

    /**
     * Set fingerprint lock enabled
     *
     * @param enabled
     */
    suspend fun setFingerprintLockEnabled(enabled: Boolean) {
        if (newImplementation()) {
            val current = getCurrentPasscodeTypeOrFallback()
            if (enabled) {
                passcodeRepository.setPasscodeType(PasscodeType.Biometric(current))
            } else {
                passcodeRepository.setPasscodeType(current)
            }
        } else {
            withContext(ioDispatcher) {
                databaseHandler.isFingerprintLockEnabled = enabled
            }
        }
    }

    private suspend fun getCurrentPasscodeTypeOrFallback() =
        (passcodeRepository.monitorPasscodeType().map {
            if (it is PasscodeType.Biometric) it.fallback else it
        }.first()
            ?: throw IllegalStateException("Cannot enable biometrics without a fallback passcode method"))

    /**
     * Is finger print lock enabled
     *
     */
    suspend fun isFingerPrintLockEnabled() =
        if (newImplementation()) {
            passcodeTypeIsBiometric()
        } else {
            withContext(ioDispatcher) { databaseHandler.isFingerprintLockEnabled }
        }

    private suspend fun passcodeTypeIsBiometric() = passcodeRepository.monitorPasscodeType().map {
        it is PasscodeType.Biometric
    }.first()

    /**
     * Set passcode enabled
     *
     * @param enable
     */
    suspend fun setPasscodeEnabled(enable: Boolean) {
        if (newImplementation()) {
            passcodeRepository.setPasscodeEnabled(enable)
        } else {
            withContext(ioDispatcher) { databaseHandler.isPasscodeLockEnabled = enable }
        }
    }

    /**
     * Set passcode lock type
     *
     * @param type
     */
    suspend fun setPasscodeLockType(type: String) {
        if (newImplementation()) {
            val newType = getNewPasscodeType(type)
            passcodeRepository.setPasscodeType(newType)
        } else {
            withContext(ioDispatcher) { databaseHandler.passcodeLockType = type }
        }
    }

    private suspend fun getNewPasscodeType(
        type: String,
    ): PasscodeType {
        val passcodeType = when (type) {
            PIN_6 -> PasscodeType.Pin(6)
            PIN_ALPHANUMERIC -> PasscodeType.Password
            else -> PasscodeType.Pin(4)
        }
        return if (passcodeTypeIsBiometric()) PasscodeType.Biometric(passcodeType) else passcodeType
    }

    /**
     * Set passcode
     *
     * @param passcode
     */
    suspend fun setPasscode(passcode: String) {
        if (newImplementation()) {
            passcodeRepository.setPasscode(passcode)
        } else {
            withContext(ioDispatcher) { databaseHandler.passcodeLockCode = passcode }
        }
    }

    /**
     * Get failed attempts count
     *
     * @return
     */
    suspend fun getFailedAttemptsCount() =
        if (newImplementation()) {
            passcodeRepository.monitorFailedAttempts().firstOrNull() ?: 0
        } else {
            withContext(ioDispatcher) { databaseHandler.attributes?.attempts ?: 0 }
        }

    /**
     * Get passcode type
     *
     * @return
     */
    suspend fun getPasscodeType(): String =
        if (newImplementation()) {
            passcodeRepository.monitorPasscodeType().map {
                getPasscodeTypeString(it)
            }.first()
        } else {
            withContext(ioDispatcher) {
                databaseHandler.preferences?.passcodeLockType.takeUnless { it.isNullOrEmpty() }
                    ?: PIN_4
            }
        }

    private fun getPasscodeTypeString(it: PasscodeType?): String = when {
        it is PasscodeType.Biometric -> getPasscodeTypeString(it.fallback)
        it == PasscodeType.Password -> PIN_ALPHANUMERIC
        it is PasscodeType.Pin && it.digits == 4 -> PIN_4
        it is PasscodeType.Pin && it.digits == 6 -> PIN_6
        else -> PIN_4
    }

    /**
     * Set failed attempts count
     *
     * @param attempts
     */
    suspend fun setFailedAttemptsCount(attempts: Int) {
        if (newImplementation()) {
            passcodeRepository.setFailedAttempts(attempts)
        } else {
            withContext(ioDispatcher) { databaseHandler.setAttrAttempts(attempts) }
        }
    }

    /**
     * Check password
     *
     * @param password
     * @return
     */
    suspend fun checkPassword(password: String): Boolean =
        if (newImplementation()) {
            accountRepository.isCurrentPassword(password)
        } else {
            withContext(ioDispatcher) { megaApi.isCurrentPassword(password) }
        }

    private suspend fun newImplementation() =
        getFeatureFlagValueUseCase(AppFeatures.PasscodeBackend)
}