package mega.privacy.android.feature.sync.data.gateway

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Named

internal const val syncPromotionDataStoreName = "syncPromotionDataStore"

internal class SyncPromotionDataStoreImpl @Inject constructor(
    @Named(syncPromotionDataStoreName) private val dataStore: DataStore<Preferences>,
) : SyncPromotionDataStore {

    private val lastShownTimestampKey = longPreferencesKey(name = "lastShownTimestamp")
    private val numberOfTimesShownKey = intPreferencesKey(name = "numberOfTimesShown")

    override suspend fun getLastShownTimestamp(): Long =
        dataStore.data.firstOrNull()?.get(lastShownTimestampKey) ?: 0

    override suspend fun setLastShownTimestamp(timestamp: Long) {
        dataStore.edit {
            it[lastShownTimestampKey] = timestamp
        }
    }

    override suspend fun getNumberOfTimesShown(): Int =
        dataStore.data.firstOrNull()?.get(numberOfTimesShownKey) ?: 0

    override suspend fun setNumberOfTimesShown(numberOfTimes: Int) {
        dataStore.edit {
            it[numberOfTimesShownKey] = numberOfTimes
        }
    }

    override suspend fun increaseNumberOfTimesShown(currentTimestamp: Long) {
        dataStore.edit {
            it[lastShownTimestampKey] = currentTimestamp
            it[numberOfTimesShownKey] = getNumberOfTimesShown() + 1
        }
    }

}

internal val Context.syncPromotionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = syncPromotionDataStoreName,
)