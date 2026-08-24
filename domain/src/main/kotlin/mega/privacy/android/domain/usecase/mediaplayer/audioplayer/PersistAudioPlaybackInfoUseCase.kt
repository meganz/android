package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import mega.privacy.android.domain.entity.continuewhereleftoff.AUDIO_RESUME_FINISHED_THRESHOLD_MS
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.continuewhereleftoff.RemoveRecentlyUsedItemUseCase
import javax.inject.Inject

/**
 * Persists an audio playback snapshot to storage, or marks the item as played through.
 *
 * Snapshots at or below [CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS] are ignored so briefly opened
 * items are never surfaced back as resumable. Within the last
 * [AUDIO_RESUME_FINISHED_THRESHOLD_MS] of a known duration the item counts as finished: its
 * playback info is deleted and it is dropped from the Continue Where Left Off index.
 * Everything else is written straight to storage so the position survives a process kill,
 * crash, or device restart.
 */
class PersistAudioPlaybackInfoUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val removeRecentlyUsedItemUseCase: RemoveRecentlyUsedItemUseCase,
) {
    /**
     * Persist audio playback info
     *
     * @param info the playback snapshot to persist
     */
    suspend operator fun invoke(info: MediaPlaybackInfo) {
        if (info.mediaHandle == -1L) return
        if (info.currentPosition <= CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS) return
        val remaining = info.totalDuration - info.currentPosition
        if (info.totalDuration > 0 && remaining <= AUDIO_RESUME_FINISHED_THRESHOLD_MS) {
            mediaPlayerRepository.deleteMediaPlaybackInfo(info.mediaHandle)
            removeRecentlyUsedItemUseCase(info.mediaHandle)
        } else {
            mediaPlayerRepository.updateAndPersistAudioPlaybackInfo(info)
        }
    }
}
