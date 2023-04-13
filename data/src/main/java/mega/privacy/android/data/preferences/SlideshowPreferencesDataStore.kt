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

    companion object {
        private const val speedPreferenceKeyString = "SLIDESHOW_SPEED"
        private const val orderPreferenceKeyString = "SLIDESHOW_ORDER"
        private const val repeatPreferenceKeyString = "SLIDESHOW_REPEAT"
    }

    override fun monitorSpeedSetting(userHandle: Long): Flow<SlideshowSpeed?> =
        context.dataStore.data.map { prefs ->
            try {
                val userKey = stringPreferencesKey("$userHandle")
                val userPref = prefs[userKey]
                userPref?.let {
                    SlideshowSpeed.values().find {
                        it.id == JSONObject(userPref)[speedPreferenceKeyString]
                    }
                }
            } catch (e: JSONException) {
                // Default value is normal
                SlideshowSpeed.Normal
            }

        }.flowOn(ioDispatcher)

    override suspend fun saveSpeedSetting(userHandle: Long, speed: SlideshowSpeed) {
        withContext(ioDispatcher) {
            context.dataStore.edit { prefs ->
                val userKey = stringPreferencesKey("$userHandle")
                val userPref = prefs[userKey]

                val json = JSONObject(userPref ?: "{}")
                json.put(speedPreferenceKeyString, speed.id)

                prefs[userKey] = json.toString()
            }
        }
    }

    override fun monitorOrderSetting(userHandle: Long): Flow<SlideshowOrder?> =
        context.dataStore.data.map { prefs ->
            try {
                val userKey = stringPreferencesKey("$userHandle")
                val userPref = prefs[userKey]
                userPref?.let {
                    SlideshowOrder.values().find {
                        it.id == JSONObject(userPref)[orderPreferenceKeyString]
                    }
                }
            } catch (e: JSONException) {
                // Default value is shuffle
                SlideshowOrder.Shuffle
            }
        }.flowOn(ioDispatcher)

    override suspend fun saveOrderSetting(userHandle: Long, order: SlideshowOrder) {
        withContext(ioDispatcher) {
            context.dataStore.edit { prefs ->
                val userKey = stringPreferencesKey("$userHandle")
                val userPref = prefs[userKey]

                val json = JSONObject(userPref ?: "{}")
                json.put(orderPreferenceKeyString, order.id)

                prefs[userKey] = json.toString()
            }
        }
    }

    override fun monitorRepeatSetting(userHandle: Long): Flow<Boolean?> =
        context.dataStore.data.map { prefs ->
            try {
                val userKey = stringPreferencesKey("$userHandle")
                val userPref = prefs[userKey]
                userPref?.let {
                    JSONObject(userPref)[repeatPreferenceKeyString] == true
                }
            } catch (e: JSONException) {
                // Default value is off
                false
            }
        }.flowOn(ioDispatcher)

    override suspend fun saveRepeatSetting(userHandle: Long, isRepeat: Boolean) {
        withContext(ioDispatcher) {
            context.dataStore.edit { prefs ->
                val userKey = stringPreferencesKey("$userHandle")
                val userPref = prefs[userKey]

                val json = JSONObject(userPref ?: "{}")
                json.put(repeatPreferenceKeyString, isRepeat)

                prefs[userKey] = json.toString()
            }
        }
    }
}
