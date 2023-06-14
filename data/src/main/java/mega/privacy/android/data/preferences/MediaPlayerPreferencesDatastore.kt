package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.preferences.MediaPlayerPreferencesGateway
import javax.inject.Inject

private const val MEDIA_PLAYER_PREFERENCES = "MEDIA_PLAYER_PREFERENCES"
private const val KEY_AUDIO_BACKGROUND_PLAY_ENABLED = "settings_audio_background_play_enabled"
private const val KEY_AUDIO_SHUFFLE_ENABLED = "settings_audio_shuffle_enabled"
private const val KEY_AUDIO_REPEAT_MODE = "settings_audio_repeat_mode"
private const val KEY_VIDEO_REPEAT_MODE = "settings_video_repeat_mode"
private const val mediaPlayerPreferenceFileName = MEDIA_PLAYER_PREFERENCES
private val Context.mediaPlayerPreferenceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = mediaPlayerPreferenceFileName,
    produceMigrations = {
        listOf(
            SharedPreferencesMigration(
                context = it,
                sharedPreferencesName = mediaPlayerPreferenceFileName,
                keysToMigrate = setOf(
                    KEY_AUDIO_BACKGROUND_PLAY_ENABLED,
                    KEY_AUDIO_SHUFFLE_ENABLED,
                    KEY_AUDIO_REPEAT_MODE,
                    KEY_VIDEO_REPEAT_MODE
                )
            )
        )
    }
)

internal class MediaPlayerPreferencesDatastore @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaPlayerPreferencesGateway {
    private val audioBackgroundPlayEnabledKey =
        booleanPreferencesKey(KEY_AUDIO_BACKGROUND_PLAY_ENABLED)
    private val audioShuffleEnabledKey = booleanPreferencesKey(KEY_AUDIO_SHUFFLE_ENABLED)
    private val audioRepeatModeKey = intPreferencesKey(KEY_AUDIO_REPEAT_MODE)
    private val videoRepeatModeKey = intPreferencesKey(KEY_VIDEO_REPEAT_MODE)

    override fun monitorAudioBackgroundPlayEnabled() =
        context.mediaPlayerPreferenceDataStore.monitor(audioBackgroundPlayEnabledKey)

    override suspend fun setAudioBackgroundPlayEnabled(value: Boolean) {
        context.mediaPlayerPreferenceDataStore.edit {
            it[audioBackgroundPlayEnabledKey] = value
        }
    }

    override fun monitorAudioShuffleEnabled() =
        context.mediaPlayerPreferenceDataStore.monitor(audioShuffleEnabledKey)

    override suspend fun setAudioShuffleEnabled(value: Boolean) {
        context.mediaPlayerPreferenceDataStore.edit {
            it[audioShuffleEnabledKey] = value
        }
    }

    override fun monitorAudioRepeatMode() =
        context.mediaPlayerPreferenceDataStore.monitor(audioRepeatModeKey)

    override suspend fun setAudioRepeatMode(value: Int) {
        context.mediaPlayerPreferenceDataStore.edit {
            it[audioRepeatModeKey] = value
        }
    }

    override fun monitorVideoRepeatMode() =
        context.mediaPlayerPreferenceDataStore.monitor(videoRepeatModeKey)

    override suspend fun setVideoRepeatMode(value: Int) {
        context.mediaPlayerPreferenceDataStore.edit {
            it[videoRepeatModeKey] = value
        }
    }
}