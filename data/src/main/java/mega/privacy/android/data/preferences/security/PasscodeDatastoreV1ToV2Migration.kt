package mega.privacy.android.data.preferences.security

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.preferences.base.PreferencesSerializer
import timber.log.Timber
import java.io.FileNotFoundException
import javax.inject.Inject

internal const val passcodeDatastoreV1Name = "passcodeDataStore"

/**
 * Reads the legacy V1 passcode datastore (per-field encrypted with [DecryptData])
 * and writes plaintext typed values into the V2 [PreferenceDataStoreFactory.createEncrypted]
 * datastore. The V1 file is intentionally left in place after migration as a safety net
 * in case the V2 store needs to be regenerated; cleaning it up can happen in a later release.
 *
 * The V1 file is read directly via [PreferencesSerializer] rather than through a second
 * `DataStore<Preferences>` instance — that avoids leaking a coroutine scope and the
 * "multiple DataStores active for the same file" check.
 */
internal class PasscodeDatastoreV1ToV2Migration @Inject constructor(
    @ApplicationContext private val context: Context,
    private val decryptData: DecryptData,
) : DataMigration<Preferences> {

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData.asMap().keys.isEmpty()

    override suspend fun migrate(currentData: Preferences): Preferences {
        val v1 = readV1Preferences() ?: return currentData
        return currentData.toMutablePreferences().apply {
            runCatching {
                v1.copyString(FAILED_ATTEMPTS_V1)?.toIntOrNull()
                    ?.let { this[PasscodeDataStore.failedAttemptsKey] = it }
                v1.copyString(PASSCODE_V1)
                    ?.let { this[PasscodeDataStore.passcodeKey] = it }
                v1.copyString(LOCKED_STATE_V1)?.toBooleanStrictOrNull()
                    ?.let { this[PasscodeDataStore.lockedStateKey] = it }
                v1.copyString(PASSCODE_ENABLED_V1)?.toBooleanStrictOrNull()
                    ?.let { this[PasscodeDataStore.passcodeEnabledKey] = it }
                v1.copyString(PASSCODE_TIMEOUT_V1)?.toLongOrNull()
                    ?.let { this[PasscodeDataStore.passcodeTimeOutKey] = it }
                v1.copyString(PASSCODE_LAST_BACKGROUND_V1)?.toLongOrNull()
                    ?.let { this[PasscodeDataStore.passcodeLastBackgroundKey] = it }
                v1.copyString(PASSCODE_TYPE_V1)
                    ?.let { this[PasscodeDataStore.passcodeTypeKey] = it }
                v1.copyString(BIOMETRICS_ENABLED_V1)?.toBooleanStrictOrNull()
                    ?.let { this[PasscodeDataStore.biometricsEnabledKey] = it }
                v1.copyString(CONFIGURATION_CHANGE_V1)?.toBooleanStrictOrNull()
                    ?.let { this[PasscodeDataStore.configurationChangeKey] = it }
            }.onFailure { Timber.e(it, "Failed to decrypt V1 passcode entry; partial migration") }
        }
    }

    override suspend fun cleanUp() = Unit

    private suspend fun readV1Preferences(): Preferences? {
        val v1File = context.preferencesDataStoreFile(passcodeDatastoreV1Name)
        return runCatching {
            v1File.inputStream().use { PreferencesSerializer.readFrom(it) }
        }.recoverCatching { error ->
            // Fresh install: no V1 file → nothing to migrate. Not an error worth logging.
            if (error is FileNotFoundException) emptyPreferences()
            else throw error
        }.onFailure {
            Timber.e(it, "Failed to read V1 passcode datastore; skipping migration")
        }.getOrNull()
    }

    private suspend fun Preferences.copyString(key: Preferences.Key<String>): String? =
        decryptData(this[key])

    companion object {
        private val FAILED_ATTEMPTS_V1 = stringPreferencesKey("failedAttemptsKey")
        private val PASSCODE_V1 = stringPreferencesKey("passcode")
        private val LOCKED_STATE_V1 = stringPreferencesKey("lockedState")
        private val PASSCODE_ENABLED_V1 = stringPreferencesKey("passcodeEnabled")
        private val PASSCODE_TIMEOUT_V1 = stringPreferencesKey("passcodeTimeOutKey")
        private val PASSCODE_LAST_BACKGROUND_V1 = stringPreferencesKey("passcodeLastBackgroundKey")
        private val PASSCODE_TYPE_V1 = stringPreferencesKey("passcodeTypeKey")
        private val BIOMETRICS_ENABLED_V1 = stringPreferencesKey("biometricsEnabledKey")
        private val CONFIGURATION_CHANGE_V1 = stringPreferencesKey("configurationChangeKey")
    }
}
