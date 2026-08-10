package mega.privacy.android.data.preferences.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.security.PasscodeStoreGateway
import javax.inject.Inject
import javax.inject.Named

internal const val passcodeDatastoreName = "passcodeDataStoreV2"

internal class PasscodeDataStore @Inject constructor(
    @Named(passcodeDatastoreName) private val dataStore: DataStore<Preferences>,
) : PasscodeStoreGateway {

    override fun monitorFailedAttempts() = dataStore.monitor(failedAttemptsKey)

    override suspend fun setFailedAttempts(attempts: Int) {
        dataStore.edit { it[failedAttemptsKey] = attempts }
    }

    override suspend fun setPasscode(passcode: String?) {
        dataStore.edit {
            if (passcode == null) it.remove(passcodeKey) else it[passcodeKey] = passcode
        }
    }

    override suspend fun getPasscode() = dataStore.data.map { it[passcodeKey] }.first()

    override suspend fun setLockedState(state: Boolean) {
        dataStore.edit { it[lockedStateKey] = state }
    }

    override fun monitorLockState() = dataStore.monitor(lockedStateKey)

    override suspend fun setPasscodeEnabledState(enabled: Boolean) {
        dataStore.edit { it[passcodeEnabledKey] = enabled }
    }

    override fun monitorPasscodeEnabledState() = dataStore.monitor(passcodeEnabledKey)

    override suspend fun setPasscodeTimeout(timeOutMilliseconds: Long?) {
        dataStore.edit {
            if (timeOutMilliseconds == null) it.remove(passcodeTimeOutKey)
            else it[passcodeTimeOutKey] = timeOutMilliseconds
        }
    }

    override fun monitorPasscodeTimeOut() = dataStore.monitor(passcodeTimeOutKey)

    override suspend fun setLastBackgroundTime(backgroundUTC: Long?) {
        dataStore.edit {
            if (backgroundUTC == null) it.remove(passcodeLastBackgroundKey)
            else it[passcodeLastBackgroundKey] = backgroundUTC
        }
    }

    override fun monitorLastBackgroundTime() = dataStore.monitor(passcodeLastBackgroundKey)

    override suspend fun setPasscodeType(passcodeType: String?) {
        dataStore.edit {
            if (passcodeType == null) it.remove(passcodeTypeKey)
            else it[passcodeTypeKey] = passcodeType
        }
    }

    override fun monitorPasscodeType() = dataStore.monitor(passcodeTypeKey)

    override suspend fun setBiometricsEnabled(enabled: Boolean?) {
        dataStore.edit {
            if (enabled == null) it.remove(biometricsEnabledKey)
            else it[biometricsEnabledKey] = enabled
        }
    }

    override fun monitorBiometricEnabledState() = dataStore.monitor(biometricsEnabledKey)

    override suspend fun setConfigurationChangedStatus(isConfigurationChanged: Boolean) {
        dataStore.edit { it[configurationChangeKey] = isConfigurationChanged }
    }

    override fun monitorConfigurationChangedStatus() =
        dataStore.monitor(configurationChangeKey).map { it ?: false }

    companion object {
        val failedAttemptsKey = intPreferencesKey("failedAttemptsKey")
        val passcodeKey = stringPreferencesKey("passcode")
        val lockedStateKey = booleanPreferencesKey("lockedState")
        val passcodeEnabledKey = booleanPreferencesKey("passcodeEnabled")
        val passcodeTimeOutKey = longPreferencesKey("passcodeTimeOutKey")
        val passcodeLastBackgroundKey = longPreferencesKey("passcodeLastBackgroundKey")
        val passcodeTypeKey = stringPreferencesKey("passcodeTypeKey")
        val biometricsEnabledKey = booleanPreferencesKey("biometricsEnabledKey")
        val configurationChangeKey = booleanPreferencesKey("configurationChangeKey")
    }
}
