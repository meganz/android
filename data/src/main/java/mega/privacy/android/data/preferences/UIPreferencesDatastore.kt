package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import javax.inject.Inject

private const val USER_INTERFACE_PREFERENCES = "USER_INTERFACE_PREFERENCES"
private const val PREFERRED_START_SCREEN = "PREFERRED_START_SCREEN"
private const val HIDE_RECENT_ACTIVITY = "HIDE_RECENT_ACTIVITY"
private const val MEDIA_DISCOVERY_VIEW = "MEDIA_DISCOVERY_VIEW"
private const val SUBFOLDER_MEDIA_DISCOVERY = "SUBFOLDER_MEDIA_DISCOVERY"
private const val SHOW_OFFLINE_WARNING_VIEW = "SHOW_OFFLINE_WARNING_VIEW"
private const val PHOTOS_RECENT_QUERIES = "PHOTOS_RECENT_QUERIES"
private const val VIEW_TYPE = "VIEW_TYPE"
private const val ALMOST_FULL_STORAGE_BANNER_CLOSING_TIMESTAMP =
    "ALMOST_FULL_STORAGE_BANNER_CLOSING_TIMESTAMP"
private const val ADS_CLOSING_TIMESTAMP = "ADS_CLOSING_TIMESTAMP"
private const val GEO_TAGGING = "GEO_TAGGING"
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
    private val almostFullStorageBannerClosingTimestampKey =
        longPreferencesKey(ALMOST_FULL_STORAGE_BANNER_CLOSING_TIMESTAMP)
    private val photosRecentQueriesKey = stringPreferencesKey(PHOTOS_RECENT_QUERIES)

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

    override fun monitorAlmostFullStorageBannerClosingTimestamp(): Flow<Long?> =
        context.uiPreferenceDataStore.monitor(almostFullStorageBannerClosingTimestampKey)

    override suspend fun setAlmostFullStorageBannerClosingTimestamp(timestamp: Long) {
        context.uiPreferenceDataStore.edit {
            it[almostFullStorageBannerClosingTimestampKey] = timestamp
        }
    }

    override suspend fun setPhotosRecentQueries(queries: List<String>) {
        context.uiPreferenceDataStore.edit {
            it[photosRecentQueriesKey] = queries.joinToString("[SEP]")
        }
    }

    override fun monitorPhotosRecentQueries(): Flow<List<String>> {
        return context.uiPreferenceDataStore
            .monitor(photosRecentQueriesKey)
            .map { text ->
                if (text.isNullOrBlank()) listOf()
                else text.split("[SEP]")
            }
    }

    override suspend fun setAdsClosingTimestamp(timestamp: Long) {
        context.uiPreferenceDataStore.edit {
            it[longPreferencesKey(ADS_CLOSING_TIMESTAMP)] = timestamp
        }
    }

    override fun monitorAdsClosingTimestamp(): Flow<Long?> {
        return context.uiPreferenceDataStore.monitor(longPreferencesKey(ADS_CLOSING_TIMESTAMP))
    }

    override fun monitorGeoTaggingStatus(): Flow<Boolean?> =
        context.uiPreferenceDataStore.monitor(booleanPreferencesKey(GEO_TAGGING))

    override suspend fun enableGeoTagging(value: Boolean) {
        context.uiPreferenceDataStore.edit {
            it[booleanPreferencesKey(GEO_TAGGING)] = value
        }
    }
}