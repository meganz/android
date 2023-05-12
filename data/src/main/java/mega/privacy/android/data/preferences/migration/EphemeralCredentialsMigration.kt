package mega.privacy.android.data.preferences.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.mapper.login.EphemeralCredentialsPreferenceMapper
import javax.inject.Inject

internal class EphemeralCredentialsMigration @Inject constructor(
    private val databaseHandler: DatabaseHandler,
    private val ephemeralCredentialsPreferenceMapper: EphemeralCredentialsPreferenceMapper,
) : DataMigration<Preferences> {
    val ephemeral = databaseHandler.ephemeral

    override suspend fun cleanUp() {
        databaseHandler.clearEphemeral()
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        ephemeral != null

    override suspend fun migrate(currentData: Preferences): Preferences {
        // it only run if shouldMigrate as true so ephemeral not null
        checkNotNull(ephemeral)
        return currentData.toMutablePreferences().apply {
            ephemeralCredentialsPreferenceMapper(this, ephemeral)
        }
    }
}