package mega.privacy.android.app.utils.wrapper

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
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
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.security.PasscodeRepository
import mega.privacy.android.domain.usecase.MonitorPasscodeLockPreferenceUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Passcode preference wrapper
 *
 */
@Singleton
class PasscodePreferenceWrapper @Inject constructor(
    private val monitorPasscodeLockPreferenceUseCase: MonitorPasscodeLockPreferenceUseCase,
    private val passcodeRepository: PasscodeRepository,
    private val accountRepository: AccountRepository,
) {
    /**
     * Is passcode enabled
     *
     */
    suspend fun isPasscodeEnabled() =
        monitorPasscodeLockPreferenceUseCase().first()

    /**
     * Get passcode
     *
     * @return
     */
    suspend fun getPasscode(): String? =
        passcodeRepository.getPasscode()

    /**
     * Get passcode time out
     *
     */
    suspend fun getPasscodeTimeOut() =
        passcodeRepository.monitorPasscodeTimeOut().map {
            when (it) {
                PasscodeTimeout.Immediate -> REQUIRE_PASSCODE_IMMEDIATE
                is PasscodeTimeout.TimeSpan -> getTimeoutTime(it.milliseconds.toInt())
                null -> Constants.REQUIRE_PASSCODE_INVALID
            }
        }.first()

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
        val timeout = getTimeout(passcodeRequireTime)
        passcodeRepository.setPasscodeTimeOut(timeout)
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
        val current = getCurrentPasscodeTypeOrFallback()
        if (enabled) {
            passcodeRepository.setPasscodeType(PasscodeType.Biometric(current))
        } else {
            passcodeRepository.setPasscodeType(current)
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
        passcodeTypeIsBiometric()

    private suspend fun passcodeTypeIsBiometric() = passcodeRepository.monitorPasscodeType().map {
        it is PasscodeType.Biometric
    }.first()

    /**
     * Set passcode enabled
     *
     * @param enable
     */
    suspend fun setPasscodeEnabled(enable: Boolean) {
        passcodeRepository.setPasscodeEnabled(enable)
    }

    /**
     * Set passcode lock type
     *
     * @param type
     */
    suspend fun setPasscodeLockType(type: String) {
        val newType = getNewPasscodeType(type)
        passcodeRepository.setPasscodeType(newType)
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
        passcodeRepository.setPasscode(passcode)
    }

    /**
     * Get failed attempts count
     *
     * @return
     */
    suspend fun getFailedAttemptsCount() =
        passcodeRepository.monitorFailedAttempts().firstOrNull() ?: 0

    /**
     * Get passcode type
     *
     * @return
     */
    suspend fun getPasscodeType(): String =
        passcodeRepository.monitorPasscodeType().map {
            getPasscodeTypeString(it)
        }.first()

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
        passcodeRepository.setFailedAttempts(attempts)
    }

    /**
     * Check password
     *
     * @param password
     * @return
     */
    suspend fun checkPassword(password: String): Boolean =
        accountRepository.isCurrentPassword(password)
}