package mega.privacy.android.feature.sync.data.gateway

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

internal const val syncPrefsDataStoreName = "syncPrefsDataStore"

internal class SyncPreferencesDatastoreImpl @Inject constructor(
    @Named(syncPrefsDataStoreName) private val dataStore: DataStore<Preferences>,
) : SyncPreferencesDatastore {

    private val onboardingShownKey = booleanPreferencesKey("onboardingShown")
    private val syncOnlyByWiFiKey = booleanPreferencesKey("syncOnlyByWiFi")

    override suspend fun setOnboardingShown(shown: Boolean) {
        dataStore.edit {
            it[onboardingShownKey] = shown
        }
    }

    override suspend fun getOnboardingShown(): Boolean? =
        dataStore.data.first()[onboardingShownKey]

    override suspend fun setSyncOnlyByWiFi(checked: Boolean) {
        dataStore.edit {
            it[syncOnlyByWiFiKey] = checked
        }
    }

    override fun monitorSyncOnlyByWiFi(): Flow<Boolean?> =
        dataStore.data.map { it[syncOnlyByWiFiKey] }
}

internal val Context.syncPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = syncPrefsDataStoreName,
)
