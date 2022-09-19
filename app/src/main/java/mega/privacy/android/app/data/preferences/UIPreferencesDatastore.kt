package mega.privacy.android.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.app.utils.SharedPreferenceConstants.PREFERRED_START_SCREEN
import mega.privacy.android.app.utils.SharedPreferenceConstants.USER_INTERFACE_PREFERENCES
import java.io.IOException
import javax.inject.Inject

private const val uiPreferenceFileName = USER_INTERFACE_PREFERENCES
private val Context.uiPreferenceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = uiPreferenceFileName,
    produceMigrations = {
        listOf(
            SharedPreferencesMigration(
                context = it,
                sharedPreferencesName = uiPreferenceFileName,
                keysToMigrate = setOf(
                    PREFERRED_START_SCREEN,
                )
            ),
        )
    })

class UIPreferencesDatastore @Inject constructor(
    @ApplicationContext private val context: Context,
) : UIPreferencesGateway {
    private val preferredStartScreenKey = intPreferencesKey(PREFERRED_START_SCREEN)

    override fun monitorPreferredStartScreen() = context.uiPreferenceDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map {
            it[preferredStartScreenKey]
        }

    override suspend fun setPreferredStartScreen(value: Int) {
        context.uiPreferenceDataStore.edit {
            it[preferredStartScreenKey] = value
        }
    }

    override fun monitorStartScreenLoginTimestamp(): Flow<Long?> {
        TODO("Not yet implemented")
    }

    override suspend fun setStartScreenLoginTimestamp(value: Long) {
        TODO("Not yet implemented")
    }

    override fun monitorDoNotAlertAboutStartScreen(): Flow<Boolean?> {
        TODO("Not yet implemented")
    }

    override suspend fun setDoNotAlertAboutStartScreen(value: Boolean) {
        TODO("Not yet implemented")
    }

    override fun monitorHideRecentActivity(): Flow<Boolean?> {
        TODO("Not yet implemented")
    }

    override suspend fun setHideRecentActivity(value: Boolean) {
        TODO("Not yet implemented")
    }
}