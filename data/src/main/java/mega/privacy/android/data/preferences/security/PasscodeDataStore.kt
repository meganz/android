package mega.privacy.android.data.preferences.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.gateway.security.PasscodeStoreGateway
import javax.inject.Inject

private val Context.passcodeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "passcodeDataStore",
)

internal class PasscodeDataStore @Inject constructor() : PasscodeStoreGateway {
    override fun getFailedAttemptsFlow(): Flow<Int?> {
        TODO("Not yet implemented")
    }

    override suspend fun setFailedAttempts(attempts: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun setPasscode(passcode: String?) {
        TODO("Not yet implemented")
    }

    override suspend fun getPasscode(): String? {
        TODO("Not yet implemented")
    }

    override suspend fun setLockedState(state: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getLockStateFlow(): Flow<Boolean?> {
        TODO("Not yet implemented")
    }
}