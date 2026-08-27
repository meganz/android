package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.continuewhereleftoff.AUDIO_PLAYBACK_INFO_SAVE_INTERVAL_MS
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.logging.Log
import mega.privacy.android.domain.usecase.GetTickerUseCase
import javax.inject.Inject

/**
 * Tracks the playing audio and persists its playback position to storage every
 * [AUDIO_PLAYBACK_INFO_SAVE_INTERVAL_MS], so the saved position survives a process kill,
 * crash, or device restart. Each snapshot goes through [PersistAudioPlaybackInfoUseCase],
 * which also decides when the item counts as played through.
 *
 * The ticker keeps firing while playback is paused, where every snapshot is identical, so
 * a snapshot equal to the last successfully persisted one is skipped instead of rewriting the
 * same row indefinitely. Deduplicating on what was persisted rather than on what was emitted
 * is what lets a failed snapshot be retried while paused, where every emission is identical.
 *
 * A failed tick is contained rather than propagated: this is the safety net that bounds
 * position loss on a hard kill, and nothing restarts it, so one transient storage error must
 * not disable tracking for the rest of the session.
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
        var lastPersisted: MediaPlaybackInfo? = null
        getTickerUseCase(AUDIO_PLAYBACK_INFO_SAVE_INTERVAL_MS)
            .map { getCurrentPlaybackInfo() }
            .collect { info ->
                if (info == lastPersisted) return@collect
                runCatching { persistAudioPlaybackInfoUseCase(info) }
                    .onSuccess { lastPersisted = info }
                    .onFailure {
                        if (it is CancellationException) throw it
                        Log.e("Failed to persist audio playback snapshot", it)
                    }
            }
    }
}
