package mega.privacy.android.data.preferences.security

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.model.MegaPreferences
import javax.inject.Inject

internal class PasscodeDatastoreMigration @Inject constructor(
    private val databaseHandler: DatabaseHandler,
    private val passcodeDataStoreFactory: PasscodeDataStoreFactory,
) : DataMigration<Preferences> {

    override suspend fun cleanUp() {
        //No-op
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        return currentData.asMap().keys.isEmpty()
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val store = passcodeDataStoreFactory(currentData)

        val oldPreferences = databaseHandler.preferences
        if (oldPreferences == null) {
            setDefaults(store)
        } else {
            setExistingValues(store, oldPreferences)
        }

        return currentData
    }

    private suspend fun setDefaults(store: PasscodeDataStore) {
        store.setValues(
            enabled = false,
            attempts = 0,
            passcode = null,
            state = false,
            timeOutMilliseconds = null,
            backgroundUTC = null
        )
    }

    private suspend fun setExistingValues(
        store: PasscodeDataStore,
        oldPreferences: MegaPreferences,
    ) {
        store.setValues(
            enabled = oldPreferences.passcodeLockEnabled.toBoolean(),
            attempts = databaseHandler.attributes?.attempts ?: 0,
            passcode = oldPreferences.passcodeLockCode,
            state = oldPreferences.passcodeLockEnabled.toBoolean(),
            timeOutMilliseconds = oldPreferences.passcodeLockRequireTime.toLongOrNull(),
            backgroundUTC = null
        )
    }

    private suspend fun PasscodeDataStore.setValues(
        enabled: Boolean,
        attempts: Int,
        passcode: String?,
        state: Boolean,
        timeOutMilliseconds: Long?,
        backgroundUTC: Long?,
    ) {
        with(this) {
            setPasscodeEnabledState(enabled)
            setFailedAttempts(attempts)
            setPasscode(passcode)
            setLockedState(state)
            setPasscodeTimeout(timeOutMilliseconds)
            setLastBackgroundTime(backgroundUTC)
        }
    }


}