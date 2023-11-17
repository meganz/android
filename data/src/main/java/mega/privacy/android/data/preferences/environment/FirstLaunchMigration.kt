package mega.privacy.android.data.preferences.environment

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.data.database.DatabaseHandler
import javax.inject.Inject

internal class FirstLaunchMigration @Inject constructor(
    private val databaseHandler: DatabaseHandler,
) : DataMigration<Preferences> {
    override suspend fun cleanUp() {
        // no-op
    }

    override suspend fun shouldMigrate(currentData: Preferences) =
        currentData.asMap().keys.map { it.name }.contains(IS_FIRST_LAUNCH_KEY).not()

    override suspend fun migrate(currentData: Preferences): Preferences {
        val firstLaunch = databaseHandler.preferences?.firstTime?.toBooleanStrictOrNull() ?: true
        val mutablePreferences = currentData.toMutablePreferences()
        mutablePreferences.let {
            AppInfoPreferencesDatastore(
                getPreferenceFlow = { flowOf(it) },
                editPreferences = { editFunction -> it.apply { editFunction(this) } },
            ).setIsFirstLaunch(firstLaunch)
        }
        return mutablePreferences
    }
}