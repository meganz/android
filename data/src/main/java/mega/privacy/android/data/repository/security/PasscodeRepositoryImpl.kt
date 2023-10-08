package mega.privacy.android.data.repository.security

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.security.PasscodeStoreGateway
import mega.privacy.android.data.mapper.security.PasscodeTimeoutMapper
import mega.privacy.android.data.mapper.security.PasscodeTypeMapper
import mega.privacy.android.data.mapper.security.PasscodeTypeStringMapper
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

internal class PasscodeRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val passcodeStoreGateway: PasscodeStoreGateway,
    private val passcodeTimeoutMapper: PasscodeTimeoutMapper,
    private val passcodeTypeMapper: PasscodeTypeMapper,
    private val passcodeTypeStringMapper: PasscodeTypeStringMapper,
) : PasscodeRepository {
    override fun monitorFailedAttempts() = passcodeStoreGateway.monitorFailedAttempts()
        .flowOn(ioDispatcher)

    override suspend fun setFailedAttempts(attempts: Int) = withContext(ioDispatcher) {
        passcodeStoreGateway.setFailedAttempts(attempts)
    }

    override suspend fun setPasscode(passcode: String?) = withContext(ioDispatcher) {
        passcodeStoreGateway.setPasscode(passcode)
    }

    override suspend fun getPasscode() = withContext(ioDispatcher) {
        passcodeStoreGateway.getPasscode()
    }

    override suspend fun setLocked(locked: Boolean) = withContext(ioDispatcher) {
        passcodeStoreGateway.setLockedState(locked)
    }

    override fun monitorLockState() = passcodeStoreGateway.monitorLockState()
        .flowOn(ioDispatcher)

    override suspend fun setLastPausedTime(lastPausedUTCTimestamp: Long) =
        withContext(ioDispatcher) {
            passcodeStoreGateway.setLastBackgroundTime(
                lastPausedUTCTimestamp
            )
        }

    override suspend fun getLastPausedTime(): Long? = passcodeStoreGateway
        .monitorLastBackgroundTime()
        .flowOn(ioDispatcher)
        .firstOrNull()

    override fun monitorIsPasscodeEnabled() =
        passcodeStoreGateway.monitorPasscodeEnabledState()
            .flowOn(ioDispatcher)

    override suspend fun setPasscodeEnabled(enabled: Boolean) = withContext(ioDispatcher) {
        passcodeStoreGateway.setPasscodeEnabledState(enabled)
    }

    override fun monitorPasscodeTimeOut() =
        passcodeStoreGateway.monitorPasscodeTimeOut()
            .map { passcodeTimeoutMapper(it) }
            .flowOn(ioDispatcher)

    override suspend fun setPasscodeTimeOut(passcodeTimeout: PasscodeTimeout) =
        withContext(ioDispatcher) {
            passcodeStoreGateway.setPasscodeTimeout(
                when (passcodeTimeout) {
                    PasscodeTimeout.Immediate -> 0
                    is PasscodeTimeout.TimeSpan -> passcodeTimeout.milliseconds
                }
            )
        }

    override fun monitorPasscodeType() = combine(
        passcodeStoreGateway.monitorPasscodeType(),
        passcodeStoreGateway.monitorBiometricEnabledState()
    ) { type, biometricsEnabled ->
        passcodeTypeMapper(type, biometricsEnabled)
    }.flowOn(ioDispatcher)

    override suspend fun setPasscodeType(passcodeType: PasscodeType?) = withContext(ioDispatcher) {
        when (passcodeType) {
            null -> {
                passcodeStoreGateway.setBiometricsEnabled(false)
                passcodeStoreGateway.setPasscodeType(null)
            }

            is PasscodeType.Biometric -> {
                passcodeStoreGateway.setBiometricsEnabled(true)
                passcodeStoreGateway.setPasscodeType(passcodeTypeStringMapper(passcodeType.fallback))
            }

            else -> {
                passcodeStoreGateway.setBiometricsEnabled(false)
                passcodeStoreGateway.setPasscodeType(passcodeTypeStringMapper(passcodeType))
            }
        }
    }
}