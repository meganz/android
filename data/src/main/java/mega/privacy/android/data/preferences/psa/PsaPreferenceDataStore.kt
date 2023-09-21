package mega.privacy.android.data.preferences.psa

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.psa.PsaPreferenceGateway
import javax.inject.Inject
import javax.inject.Named

internal const val psaPreferenceDataStoreName = "psaPreferenceDataStoreName"

class PsaPreferenceDataStore @Inject constructor(
    @Named(psaPreferenceDataStoreName) private val dataStore: DataStore<Preferences>,
) : PsaPreferenceGateway {
    private val lastRequestTimeKey = longPreferencesKey("lastRequestTimeKey")
    override suspend fun setLastRequestedTime(time: Long?) {
        dataStore.edit {
            if (time == null) {
                it.remove(lastRequestTimeKey)
            } else {
                it[lastRequestTimeKey] = time
            }
        }
    }

    override suspend fun getLastRequestedTime(): Long? =
        dataStore.monitor(lastRequestTimeKey).firstOrNull()
}