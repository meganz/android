package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo.Companion.INVALID_MEDIA_HANDLE
import mega.privacy.android.domain.logging.Log
import mega.privacy.android.domain.usecase.GetTickerUseCase
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Tracks the playing video and saves its position once per second through
 * [SaveVideoPlaybackInfoUseCase]. The periodic save only bounds how much position is lost on a
 * hard kill or crash: the player also saves immediately when playback is paused, when the app
 * goes to the background, and when the player is left, so this interval is not what the user's
 * resume accuracy depends on.
 *
 * A snapshot taken before the player is prepared carries no handle and no duration, and a
 * duration of zero would read as "watched through" further down: such snapshots are dropped
 * rather than acted on. Nothing is tracked until the position is past the resumable threshold.
 *
 * The ticker keeps firing while playback is paused, where every snapshot is identical, so a
 * snapshot equal to the last successfully saved one is skipped instead of rewriting the same
 * position every tick. Deduplicating on what was saved rather than on what was emitted lets a
 * failed snapshot be retried on the next tick. A failed save is logged rather than propagated,
 * because nothing restarts this collector and one transient storage error must not disable
 * tracking for the rest of the session.
 */
class TrackPlaybackPositionUseCase @Inject constructor(
    private val saveVideoPlaybackInfoUseCase: SaveVideoPlaybackInfoUseCase,
    private val getTickerUseCase: GetTickerUseCase,
) {

    /**
     * Track the playback information
     *
     * @param getCurrentPlaybackInfo get the current playback snapshot
     */
    suspend operator fun invoke(getCurrentPlaybackInfo: () -> MediaPlaybackInfo) {
        var lastSaved: MediaPlaybackInfo? = null
        getTickerUseCase(TimeUnit.SECONDS.toMillis(1)).map { getCurrentPlaybackInfo() }
            .filter {
                it.mediaHandle != INVALID_MEDIA_HANDLE
                        && it.totalDuration > 0
                        && it.currentPosition > CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
            }.collect { info ->
                if (info == lastSaved) return@collect
                runCatching { saveVideoPlaybackInfoUseCase(info) }
                    .onSuccess { lastSaved = info }
                    .onFailure {
                        if (it is CancellationException) throw it
                        Log.e("Failed to save the video playback info", it)
                    }
            }
    }
}
