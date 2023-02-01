package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import java.io.IOException
import javax.inject.Inject

private const val USER_INTERFACE_PREFERENCES = "USER_INTERFACE_PREFERENCES"
private const val PREFERRED_START_SCREEN = "PREFERRED_START_SCREEN"
private const val HIDE_RECENT_ACTIVITY = "HIDE_RECENT_ACTIVITY"
private const val MEDIA_DISCOVERY_VIEW = "MEDIA_DISCOVERY_VIEW"
private const val VIEW_TYPE = "VIEW_TYPE"
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
                    HIDE_RECENT_ACTIVITY,
                )
            ),
        )
    })

internal class UIPreferencesDatastore @Inject constructor(
    @ApplicationContext private val context: Context,
) : UIPreferencesGateway {
    private val preferredStartScreenKey = intPreferencesKey(PREFERRED_START_SCREEN)
    private val hideRecentActivityKey = booleanPreferencesKey(HIDE_RECENT_ACTIVITY)
    private val mediaDiscoveryViewKey = intPreferencesKey(MEDIA_DISCOVERY_VIEW)
    private val viewTypeKey = intPreferencesKey(VIEW_TYPE)

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

    override fun monitorViewType(): Flow<Int?> = context.uiPreferenceDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map {
            it[viewTypeKey]
        }

    override suspend fun setViewType(value: Int) {
        context.uiPreferenceDataStore.edit {
            it[viewTypeKey] = value
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

    override fun monitorHideRecentActivity(): Flow<Boolean?> = context.uiPreferenceDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map {
            it[hideRecentActivityKey]
        }

    override fun monitorMediaDiscoveryView(): Flow<Int?> = context.uiPreferenceDataStore.data
        .catch { exception ->
            if (exception is IOException)
                emit(emptyPreferences())
            else
                throw exception
        }.map {
            it[mediaDiscoveryViewKey]
        }

    override suspend fun setHideRecentActivity(value: Boolean) {
        context.uiPreferenceDataStore.edit {
            it[hideRecentActivityKey] = value
        }
    }

    override suspend fun setMediaDiscoveryView(value: Int) {
        context.uiPreferenceDataStore.edit {
            it[mediaDiscoveryViewKey] = value
        }
    }
}