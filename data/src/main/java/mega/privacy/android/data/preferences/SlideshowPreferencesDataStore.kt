package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.preferences.SlideshowPreferencesGateway
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.domain.qualifier.IoDispatcher
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

    private val speedPreferenceKey = intPreferencesKey("SLIDESHOW_SPEED")

    private val orderPreferenceKey = intPreferencesKey("SLIDESHOW_ORDER")

    private val repeatPreferenceKey = booleanPreferencesKey("SLIDESHOW_REPEAT")

    override fun monitorSpeedSetting(): Flow<SlideshowSpeed?> =
        context.dataStore.data.map { prefs ->
            prefs[speedPreferenceKey]?.let { id ->
                SlideshowSpeed.values().find { it.id == id }
            }
        }.flowOn(ioDispatcher)

    override suspend fun saveSpeedSetting(speed: SlideshowSpeed) {
        withContext(ioDispatcher) {
            context.dataStore.edit {
                it[speedPreferenceKey] = speed.id
            }
        }
    }

    override fun monitorOrderSetting(): Flow<SlideshowOrder?> =
        context.dataStore.data.map { prefs ->
            prefs[orderPreferenceKey]?.let { id ->
                SlideshowOrder.values().find { it.id == id }
            }
        }.flowOn(ioDispatcher)

    override suspend fun saveOrderSetting(order: SlideshowOrder) {
        withContext(ioDispatcher) {
            context.dataStore.edit {
                it[orderPreferenceKey] = order.id
            }
        }
    }

    override fun monitorRepeatSetting(): Flow<Boolean?> = context.dataStore.data.map { prefs ->
        prefs[repeatPreferenceKey]
    }.flowOn(ioDispatcher)

    override suspend fun saveRepeatSetting(isRepeat: Boolean) {
        withContext(ioDispatcher) {
            context.dataStore.edit {
                it[repeatPreferenceKey] = isRepeat
            }
        }
    }
}
