package mega.privacy.android.data.preferences.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.gateway.security.PasscodeStoreGateway
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

internal const val passcodeDatastoreName = "passcodeDataStore"

internal class PasscodeDataStore(
    private val getPreferenceFlow: () -> Flow<Preferences>,
    private val editPreferences: suspend (suspend (MutablePreferences) -> Unit) -> Preferences,
    private val encryptData: EncryptData,
    private val decryptData: DecryptData,
) : PasscodeStoreGateway {
    @Inject
    constructor(
        @Named(passcodeDatastoreName) dataStore: DataStore<Preferences>,
        encryptData: EncryptData,
        decryptData: DecryptData,
    ) : this(
        getPreferenceFlow = dataStore::data,
        editPreferences = dataStore::edit,
        encryptData = encryptData,
        decryptData = decryptData,
    )

    constructor(
        preferences: Preferences,
        encryptData: EncryptData,
        decryptData: DecryptData,
    ) : this(
        getPreferenceFlow = { flowOf(preferences) },
        editPreferences = { preferences.toMutablePreferences().apply { it(this) } },
        encryptData = encryptData,
        decryptData = decryptData,
    )

    private val failedAttemptsKey = stringPreferencesKey("failedAttemptsKey")
    private val passcodeKey = stringPreferencesKey("passcode")
    private val lockedStateKey = stringPreferencesKey("lockedState")
    private val passcodeEnabledKey = stringPreferencesKey("passcodeEnabled")
    private val passcodeTimeOutKey = stringPreferencesKey("passcodeTimeOutKey")
    private val passcodeLastBackgroundKey = stringPreferencesKey("passcodeLastBackgroundKey")

    override fun monitorFailedAttempts() =
        getPreferenceFlow().monitor(failedAttemptsKey)
            .map { decryptData(it)?.toIntOrNull() }

    override suspend fun setFailedAttempts(attempts: Int) {
        val encryptedValue = encryptData(attempts.toString()) ?: return
        editPreferences {
            it[failedAttemptsKey] = encryptedValue
        }
    }

    override suspend fun setPasscode(passcode: String?) {
        val encryptedValue = encryptData(passcode)
        editPreferences {
            if (encryptedValue == null) {
                it.remove(passcodeKey)
            } else {
                it[passcodeKey] = encryptedValue
            }
        }
    }

    override suspend fun getPasscode() = getPreferenceFlow().monitor(passcodeKey)
        .map { decryptData(it) }
        .first()

    override suspend fun setLockedState(state: Boolean) {
        val encryptedValue = encryptData(state.toString()) ?: return
        editPreferences {
            it[lockedStateKey] = encryptedValue
        }
    }

    override fun monitorLockState() =
        getPreferenceFlow().monitor(lockedStateKey)
            .map { decryptData(it)?.toBooleanStrictOrNull() }

    override suspend fun setPasscodeEnabledState(enabled: Boolean) {
        val encryptedValue = encryptData(enabled.toString()) ?: return
        editPreferences {
            it[passcodeEnabledKey] = encryptedValue
        }
    }

    override fun monitorPasscodeEnabledState() =
        getPreferenceFlow().monitor(passcodeEnabledKey)
            .map { decryptData(it)?.toBooleanStrictOrNull() }

    override suspend fun setPasscodeTimeout(timeOutMilliseconds: Long?) {
        val encryptedValue = encryptData(timeOutMilliseconds?.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(passcodeTimeOutKey)
            } else {
                it[passcodeTimeOutKey] = encryptedValue
            }
        }
    }

    override fun monitorPasscodeTimeOut() = getPreferenceFlow().monitor(passcodeTimeOutKey)
        .map { decryptData(it)?.toLongOrNull() }

    override suspend fun setLastBackgroundTime(backgroundUTC: Long?) {
        val encryptedValue = encryptData(backgroundUTC?.toString())
        editPreferences {
            if (encryptedValue == null) {
                it.remove(passcodeLastBackgroundKey)
            } else {
                it[passcodeLastBackgroundKey] = encryptedValue
            }
        }
    }

    override fun monitorLastBackgroundTime() =
        getPreferenceFlow().monitor(passcodeLastBackgroundKey)
            .map { decryptData(it)?.toLongOrNull() }

}

@Throws(Exception::class)
private fun <T> Flow<Preferences>.monitor(key: Preferences.Key<T>): Flow<T?> {
    return this
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map {
            it[key]
        }
}

