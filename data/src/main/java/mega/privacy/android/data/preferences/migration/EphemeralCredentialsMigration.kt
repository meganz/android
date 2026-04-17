package mega.privacy.android.data.preferences.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import dagger.Lazy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.mapper.login.EphemeralCredentialsPreferenceMapper
import mega.privacy.android.domain.qualifier.DatabaseDispatcher
import javax.inject.Inject

internal class EphemeralCredentialsMigration @Inject constructor(
    private val databaseHandler: Lazy<DatabaseHandler>,
    private val ephemeralCredentialsPreferenceMapper: EphemeralCredentialsPreferenceMapper,
    @DatabaseDispatcher private val databaseDispatcher: CoroutineDispatcher,
) : DataMigration<Preferences> {

    override suspend fun cleanUp() {
        withContext(databaseDispatcher) {
            databaseHandler.get().clearEphemeral()
        }
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        withContext(databaseDispatcher) {
            databaseHandler.get().ephemeral != null
        }

    override suspend fun migrate(currentData: Preferences): Preferences {
        return withContext(databaseDispatcher) {
            val ephemeral = databaseHandler.get().ephemeral
            checkNotNull(ephemeral)
            currentData.toMutablePreferences().apply {
                ephemeralCredentialsPreferenceMapper(this, ephemeral)
            }
        }
    }
}
