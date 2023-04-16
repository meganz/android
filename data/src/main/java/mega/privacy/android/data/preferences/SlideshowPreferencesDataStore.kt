package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.SlideshowPreferencesGateway
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.domain.qualifier.IoDispatcher
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SlideshowPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SlideshowPreferencesGateway {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "SLIDESHOW_PREFERENCES",
    )

    private lateinit var speedPreferenceKey: Preferences.Key<Int>
    private lateinit var orderPreferenceKey: Preferences.Key<Int>
    private lateinit var repeatPreferenceKey: Preferences.Key<Boolean>

    companion object {
        private const val speedPreferenceKeyString = "SLIDESHOW_SPEED"
        private const val orderPreferenceKeyString = "SLIDESHOW_ORDER"
        private const val repeatPreferenceKeyString = "SLIDESHOW_REPEAT"
    }

    override fun monitorSpeedSetting(userHandle: Long): Flow<SlideshowSpeed?> =
        context.dataStore.data.map { prefs ->
            speedPreferenceKey = intPreferencesKey(userHandle.toString() + speedPreferenceKeyString)
            prefs[speedPreferenceKey]?.let { id ->
                SlideshowSpeed.values().find { it.id == id }
            }
        }.flowOn(ioDispatcher)

    override suspend fun saveSpeedSetting(userHandle: Long, speed: SlideshowSpeed) {
        withContext(ioDispatcher) {
            speedPreferenceKey = intPreferencesKey(userHandle.toString() + speedPreferenceKeyString)
            context.dataStore.edit {
                it[speedPreferenceKey] = speed.id
            }
        }
    }

    override fun monitorOrderSetting(userHandle: Long): Flow<SlideshowOrder?> =
        context.dataStore.data.map { prefs ->
            orderPreferenceKey = intPreferencesKey(userHandle.toString() + orderPreferenceKeyString)
            prefs[orderPreferenceKey]?.let { id ->
                SlideshowOrder.values().find { it.id == id }
            }
        }.flowOn(ioDispatcher)

    override suspend fun saveOrderSetting(userHandle: Long, order: SlideshowOrder) {
        withContext(ioDispatcher) {
            orderPreferenceKey = intPreferencesKey(userHandle.toString() + orderPreferenceKeyString)
            context.dataStore.edit {
                it[orderPreferenceKey] = order.id
            }
        }
    }

    override fun monitorRepeatSetting(userHandle: Long): Flow<Boolean?> =
        context.dataStore.data.map { prefs ->
            repeatPreferenceKey =
                booleanPreferencesKey(userHandle.toString() + repeatPreferenceKeyString)
            prefs[repeatPreferenceKey]
        }.flowOn(ioDispatcher)

    override suspend fun saveRepeatSetting(userHandle: Long, isRepeat: Boolean) {
        withContext(ioDispatcher) {
            repeatPreferenceKey =
                booleanPreferencesKey(userHandle.toString() + repeatPreferenceKeyString)
            context.dataStore.edit {
                it[repeatPreferenceKey] = isRepeat
            }
        }
    }
}
