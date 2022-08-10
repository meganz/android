package mega.privacy.android.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.domain.entity.CallsSoundNotifications
import java.io.IOException
import javax.inject.Inject

private val Context.callsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "CALLS_PREFERENCES"
)

/**
 * Calls preferences data store implementation of the [CallsPreferencesGateway]
 *
 * @property context
 * @property ioDispatcher
 * @constructor Create empty calls preferences data store.
 **/
class CallsPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CallsPreferencesGateway {
    private val callsSoundNotificationsPreferenceKey =
        stringPreferencesKey("CALLS_SOUND_NOTIFICATIONS")

    override fun getCallSoundNotificationsPreference(): Flow<CallsSoundNotifications> =
        context.callsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                CallsSoundNotifications.valueOf(
                    preferences[callsSoundNotificationsPreferenceKey]
                        ?: CallsSoundNotifications.DEFAULT.name
                )
            }

    override suspend fun setCallSoundNotificationsPreference(soundNotifications: CallsSoundNotifications) {
        withContext(ioDispatcher) {
            context.callsDataStore.edit {
                it[callsSoundNotificationsPreferenceKey] = soundNotifications.name
            }
        }
    }

    override suspend fun clearPreferences() {
        withContext(ioDispatcher) {
            context.callsDataStore.edit {
                it.clear()
            }
        }
    }
}