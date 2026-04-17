package mega.privacy.android.data.preferences.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import dagger.Lazy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.preferences.CredentialsPreferencesDataStore.Companion.migrate
import mega.privacy.android.domain.qualifier.DatabaseDispatcher
import javax.inject.Inject

internal class CredentialsPreferencesMigration @Inject constructor(
    private val databaseHandler: Lazy<DatabaseHandler>,
    @DatabaseDispatcher private val databaseDispatcher: CoroutineDispatcher,
) : DataMigration<Preferences> {

    override suspend fun cleanUp() {
        // No-op
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData.asMap().keys.isEmpty()

    override suspend fun migrate(currentData: Preferences): Preferences {
        val credentials = withContext(databaseDispatcher) {
            databaseHandler.get().credentials
        }
        credentials?.let {
            return currentData.toMutablePreferences().apply {
                migrate(this, it)
            }.also {
                withContext(databaseDispatcher) {
                    databaseHandler.get().clearCredentials()
                }
            }
        }
        return currentData
    }
}
