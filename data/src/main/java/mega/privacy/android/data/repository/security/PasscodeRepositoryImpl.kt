package mega.privacy.android.data.repository.security

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

internal class PasscodeRepositoryImpl @Inject constructor() : PasscodeRepository {
    override fun monitorFailedAttempts(): Flow<Int> {
        TODO("Not yet implemented")
    }

    override suspend fun setFailedAttempts(attempts: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun checkPasscode(passcode: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun checkPassword(password: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun setLocked(locked: Boolean) {
        TODO("Not yet implemented")
    }

    override fun monitorLockState(): Flow<Boolean> {
        TODO("Not yet implemented")
    }

}