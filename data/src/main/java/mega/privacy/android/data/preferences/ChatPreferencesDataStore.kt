package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.qualifier.IoDispatcher
import java.io.IOException
import javax.inject.Inject

private val Context.chatDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "CHAT_PREFERENCES"
)

/**
 * Chat preferences data store implementation of the [ChatPreferencesGateway]
 *
 * @property context
 * @property ioDispatcher
 * @constructor Create empty chat preferences data store.
 **/
internal class ChatPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ChatPreferencesGateway {
    private val chatImageQualityPreferenceKey = stringPreferencesKey("CHAT_IMAGE_QUALITY")
    private val lastContactPermissionRequestedTimePreferenceKey =
        longPreferencesKey("LAST_CONTACT_PERMISSION_REQUESTED_TIME")

    override fun getChatImageQualityPreference(): Flow<ChatImageQuality> =
        context.chatDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                ChatImageQuality.valueOf(
                    preferences[chatImageQualityPreferenceKey] ?: ChatImageQuality.DEFAULT.name
                )
            }


    override suspend fun setChatImageQualityPreference(quality: ChatImageQuality) {
        withContext(ioDispatcher) {
            context.chatDataStore.edit {
                it[chatImageQualityPreferenceKey] = quality.name
            }
        }
    }

    override fun getLastContactPermissionRequestedTime(): Flow<Long> =
        context.chatDataStore.data.map { preferences ->
            preferences[lastContactPermissionRequestedTimePreferenceKey] ?: 0L
        }

    override suspend fun setLastContactPermissionRequestedTime(time: Long) {
        withContext(ioDispatcher) {
            context.chatDataStore.edit {
                it[lastContactPermissionRequestedTimePreferenceKey] = time
            }
        }
    }

    override suspend fun clearPreferences() {
        withContext(ioDispatcher) {
            context.chatDataStore.edit {
                it.clear()
            }
        }
    }
}