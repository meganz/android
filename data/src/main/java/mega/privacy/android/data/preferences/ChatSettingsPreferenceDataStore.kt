package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.gateway.preferences.ChatSettingsPreferenceGateway
import mega.privacy.android.domain.entity.settings.ChatSettings
import javax.inject.Inject
import javax.inject.Named

internal const val chatSettingsPreferenceDataStoreName = "chatSettingsDataStore"

internal class ChatSettingsPreferenceDataStore @Inject constructor(
    @Named(chatSettingsPreferenceDataStoreName) private val dataStore: DataStore<Preferences>,
) : ChatSettingsPreferenceGateway {

    override suspend fun setChatSettings(chatSettings: ChatSettings) {
        dataStore.edit {
            it[notificationsSoundKey] = chatSettings.notificationsSound
            it[vibrationEnabledKey] = chatSettings.vibrationEnabled
            it[videoQualityKey] = chatSettings.videoQuality
        }
    }

    override suspend fun setNotificationSound(sound: String?) {
        dataStore.edit { it[notificationsSoundKey] = sound.orEmpty() }
    }

    override suspend fun setVibrationEnabled(enabled: String?) {
        dataStore.edit { it[vibrationEnabledKey] = enabled.orEmpty() }
    }

    override suspend fun setVideoQuality(quality: String?) {
        dataStore.edit { it[videoQualityKey] = quality.orEmpty() }
    }

    override suspend fun clearPreferences() {
        dataStore.edit { it.clear() }
    }

    override suspend fun getChatSettings(): ChatSettings? = monitorChatSettings().firstOrNull()

    override fun monitorChatSettings(): Flow<ChatSettings?> = dataStore.data.map {
        val notificationsSound = it[notificationsSoundKey]
        val vibrationEnabled = it[vibrationEnabledKey]
        val videoQuality = it[videoQualityKey]
        if (notificationsSound == null && vibrationEnabled == null && videoQuality == null) {
            return@map null
        }
        val defaults = ChatSettings()
        ChatSettings(
            notificationsSound = notificationsSound ?: defaults.notificationsSound,
            vibrationEnabled = vibrationEnabled ?: defaults.vibrationEnabled,
            videoQuality = videoQuality ?: defaults.videoQuality,
        )
    }

    companion object {
        private val notificationsSoundKey = stringPreferencesKey("notificationsSound")
        private val vibrationEnabledKey = stringPreferencesKey("vibrationEnabled")
        private val videoQualityKey = stringPreferencesKey("videoQuality")

        fun migrate(preferences: MutablePreferences, chatSettings: ChatSettings) =
            preferences.apply {
                this[notificationsSoundKey] = chatSettings.notificationsSound
                this[vibrationEnabledKey] = chatSettings.vibrationEnabled
                this[videoQualityKey] = chatSettings.videoQuality
            }
    }
}
