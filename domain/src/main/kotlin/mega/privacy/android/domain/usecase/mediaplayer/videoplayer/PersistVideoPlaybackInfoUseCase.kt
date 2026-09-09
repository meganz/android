package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_NEAR_COMPLETION_THRESHOLD_MS
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo.Companion.INVALID_MEDIA_HANDLE
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * Persists a video playback snapshot to the shared playback info store.
 *
 * Snapshots with an unknown duration are ignored: they are taken before the player is prepared,
 * and writing one would record a zero duration that later reads cannot interpret. Otherwise the
 * position is either kept or dropped:
 *
 * - within the last [CWLO_NEAR_COMPLETION_THRESHOLD_MS] the item counts as watched through;
 * - at or below [CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS] the user is back at the start, so the stored
 *   position is dropped rather than left pointing at a place they have moved away from;
 * - everything in between is written straight to storage.
 *
 * Removal from the Continue Where Left Off index is deliberately **not** done here. While the
 * video player dual-writes to DataStore, [SaveVideoPlaybackInfoUseCase] still owns that side
 * effect on the DataStore branch; doing it in both places would fire the same removal twice per
 * save. It moves here when the DataStore path is deleted.
 */
class PersistVideoPlaybackInfoUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Persist video playback info
     *
     * @param info the playback snapshot to persist
     */
    suspend operator fun invoke(info: MediaPlaybackInfo) {
        if (info.mediaHandle == INVALID_MEDIA_HANDLE) return
        if (info.totalDuration <= 0) return

        if (info.isPlaybackPositionWorthKeeping()) {
            mediaPlayerRepository.updateAndPersistPlaybackInfo(info)
        } else {
            mediaPlayerRepository.deleteMediaPlaybackInfo(info.mediaHandle)
        }
    }
}
