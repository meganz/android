package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.continuewhereleftoff.AUDIO_PLAYBACK_INFO_SAVE_INTERVAL_MS
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.usecase.GetTickerUseCase
import javax.inject.Inject

/**
 * Tracks the playing audio and persists its playback position to storage every
 * [AUDIO_PLAYBACK_INFO_SAVE_INTERVAL_MS], so the saved position survives a process kill,
 * crash, or device restart. Each snapshot goes through [PersistAudioPlaybackInfoUseCase],
 * which also decides when the item counts as played through.
 */
class TrackAndSaveAudioPlaybackInfoUseCase @Inject constructor(
    private val persistAudioPlaybackInfoUseCase: PersistAudioPlaybackInfoUseCase,
    private val getTickerUseCase: GetTickerUseCase,
) {
    /**
     * Track and persist audio playback info
     *
     * @param getCurrentPlaybackInfo get current playback info
     */
    suspend operator fun invoke(getCurrentPlaybackInfo: () -> MediaPlaybackInfo) {
        getTickerUseCase(AUDIO_PLAYBACK_INFO_SAVE_INTERVAL_MS)
            .map { getCurrentPlaybackInfo() }
            .collect { persistAudioPlaybackInfoUseCase(it) }
    }
}
