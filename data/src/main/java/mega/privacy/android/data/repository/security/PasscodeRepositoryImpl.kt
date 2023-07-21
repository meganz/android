package mega.privacy.android.data.repository.security

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.security.PasscodeStoreGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

internal class PasscodeRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val passcodeStoreGateway: PasscodeStoreGateway,
) : PasscodeRepository {
    override fun monitorFailedAttempts() = passcodeStoreGateway.getFailedAttemptsFlow()
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

    override fun monitorLockState() = passcodeStoreGateway.getLockStateFlow()
        .flowOn(ioDispatcher)


}