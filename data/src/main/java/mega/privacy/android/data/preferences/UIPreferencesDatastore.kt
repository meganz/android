package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import javax.inject.Inject

private const val USER_INTERFACE_PREFERENCES = "USER_INTERFACE_PREFERENCES"
private const val PREFERRED_START_SCREEN = "PREFERRED_START_SCREEN"
private const val HIDE_RECENT_ACTIVITY = "HIDE_RECENT_ACTIVITY"
private const val MEDIA_DISCOVERY_VIEW = "MEDIA_DISCOVERY_VIEW"
private const val SUBFOLDER_MEDIA_DISCOVERY = "SUBFOLDER_MEDIA_DISCOVERY"
private const val SHOW_OFFLINE_WARNING_VIEW = "SHOW_OFFLINE_WARNING_VIEW"
private const val VIEW_TYPE = "VIEW_TYPE"
private val Context.uiPreferenceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_INTERFACE_PREFERENCES,
    produceMigrations = {
        listOf(
            SharedPreferencesMigration(
                context = it,
                sharedPreferencesName = USER_INTERFACE_PREFERENCES,
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
    private val subFolderMediaDiscoveryKey = booleanPreferencesKey(SUBFOLDER_MEDIA_DISCOVERY)
    private val viewTypeKey = intPreferencesKey(VIEW_TYPE)
    private val offlineWarningViewKey = booleanPreferencesKey(SHOW_OFFLINE_WARNING_VIEW)

    override fun monitorPreferredStartScreen() =
        context.uiPreferenceDataStore.monitor(preferredStartScreenKey)

    override suspend fun setPreferredStartScreen(value: Int) {
        context.uiPreferenceDataStore.edit {
            it[preferredStartScreenKey] = value
        }
    }

    override fun monitorViewType(): Flow<Int?> =
        context.uiPreferenceDataStore.monitor(viewTypeKey)

    override suspend fun setViewType(value: Int) {
        context.uiPreferenceDataStore.edit {
            it[viewTypeKey] = value
        }
    }

    override fun monitorHideRecentActivity(): Flow<Boolean?> =
        context.uiPreferenceDataStore.monitor(hideRecentActivityKey)

    override fun monitorMediaDiscoveryView(): Flow<Int?> =
        context.uiPreferenceDataStore.monitor(mediaDiscoveryViewKey)

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

    override fun monitorSubfolderMediaDiscoveryEnabled(): Flow<Boolean?> =
        context.uiPreferenceDataStore.monitor(subFolderMediaDiscoveryKey)


    override suspend fun setSubfolderMediaDiscoveryEnabled(enabled: Boolean) {
        context.uiPreferenceDataStore.edit {
            it[subFolderMediaDiscoveryKey] = enabled
        }
    }

    override suspend fun setOfflineWarningMessageVisibility(isVisible: Boolean) {
        context.uiPreferenceDataStore.edit {
            it[offlineWarningViewKey] = isVisible
        }
    }

    override fun monitorOfflineWarningMessageVisibility(): Flow<Boolean?> =
        context.uiPreferenceDataStore.monitor(offlineWarningViewKey)

}