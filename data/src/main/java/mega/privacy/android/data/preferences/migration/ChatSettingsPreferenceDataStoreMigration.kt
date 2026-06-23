package mega.privacy.android.data.preferences.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import dagger.Lazy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.preferences.ChatSettingsPreferenceDataStore.Companion.migrate
import mega.privacy.android.domain.qualifier.DatabaseDispatcher
import javax.inject.Inject

/**
 * Handles the Preference migration from the legacy [DatabaseHandler.chatSettings] to the new
 * [mega.privacy.android.data.preferences.ChatSettingsPreferenceDataStore]
 */
internal class ChatSettingsPreferenceDataStoreMigration @Inject constructor(
    private val databaseHandler: Lazy<DatabaseHandler>,
    @DatabaseDispatcher private val databaseDispatcher: CoroutineDispatcher,
) : DataMigration<Preferences> {

    override suspend fun cleanUp() = Unit

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData.asMap().keys.isEmpty()

    override suspend fun migrate(currentData: Preferences): Preferences {
        val chatSettings = withContext(databaseDispatcher) {
            databaseHandler.get().chatSettings
        } ?: return currentData
        return currentData.toMutablePreferences().apply {
            migrate(this, chatSettings)
        }
    }
}
