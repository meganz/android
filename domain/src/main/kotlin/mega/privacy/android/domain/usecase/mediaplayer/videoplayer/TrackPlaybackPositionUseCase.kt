package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_NEAR_COMPLETION_THRESHOLD_MS
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo.Companion.INVALID_MEDIA_HANDLE
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.logging.Log
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.GetTickerUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.RemoveRecentlyUsedItemUseCase
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Tracks the playing video and records its position once per second.
 *
 * During the dual-write phase each snapshot is taken once and sent to both stores: the
 * [PersistVideoPlaybackInfoUseCase] database write, and the DataStore path that the player still
 * reads from. Only the database write is deduplicated and contained:
 *
 * - the ticker keeps firing while playback is paused, where every snapshot is identical, so a
 *   snapshot equal to the last successfully persisted one is skipped instead of rewriting the
 *   same row every second. Deduplicating on what was persisted rather than on what was emitted
 *   lets a failed snapshot be retried on the next tick;
 * - a failed write is logged rather than propagated, because nothing restarts this collector and
 *   one transient storage error must not disable tracking for the rest of the session.
 *
 * The DataStore path keeps its existing behaviour untouched, including its removal of finished
 * items from the Continue Where Left Off index. That responsibility moves to
 * [PersistVideoPlaybackInfoUseCase] when the DataStore path is deleted.
 */
class TrackPlaybackPositionUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val removeRecentlyUsedItemUseCase: RemoveRecentlyUsedItemUseCase,
    private val persistVideoPlaybackInfoUseCase: PersistVideoPlaybackInfoUseCase,
    private val getTickerUseCase: GetTickerUseCase,
) {

    /**
     * Track the playback information
     *
     * @param getCurrentPlaybackInfo get the current playback snapshot
     */
    suspend operator fun invoke(getCurrentPlaybackInfo: () -> MediaPlaybackInfo) {
        var lastPersisted: MediaPlaybackInfo? = null
        getTickerUseCase(TimeUnit.SECONDS.toMillis(1)).map { getCurrentPlaybackInfo() }
            .filter {
                // A snapshot taken before the player is prepared carries no handle, and nothing
                // is tracked until the position is past the resumable threshold.
                it.mediaHandle != INVALID_MEDIA_HANDLE
                        && it.currentPosition > CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
            }.collect { info ->
                if (info != lastPersisted) {
                    runCatching { persistVideoPlaybackInfoUseCase(info) }
                        .onSuccess { lastPersisted = info }
                        .onFailure {
                            if (it is CancellationException) throw it
                            Log.e("Failed to persist the video playback info", it)
                        }
                }

                // When the video is playing until last 3 seconds, remove playback information
                // and drop it from the Continue Where Left Off index so it is not surfaced
                // back to the user as resumable.
                if (info.totalDuration - info.currentPosition < CWLO_NEAR_COMPLETION_THRESHOLD_MS) {
                    mediaPlayerRepository.deletePlaybackInformation(info.mediaHandle)
                    removeRecentlyUsedItemUseCase(info.mediaHandle)
                } else {
                    mediaPlayerRepository.updatePlaybackInformation(
                        PlaybackInformation(
                            mediaId = info.mediaHandle,
                            totalDuration = info.totalDuration,
                            currentPosition = info.currentPosition,
                        )
                    )
                }
            }
    }
}
