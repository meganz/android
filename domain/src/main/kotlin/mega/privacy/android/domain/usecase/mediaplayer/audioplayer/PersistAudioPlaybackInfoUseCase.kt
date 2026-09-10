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
 * Snapshots with an unknown duration are ignored: they are taken before the player is
 * prepared (e.g. right after a resume seek), and writing them would overwrite a good record's
 * real duration with 0 — breaking the next resume, which requires a known duration.
 * Snapshots at or below [CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS] mean the listener has moved back to
 * the start, so the stored position is dropped rather than left pointing somewhere they have moved
 * away from. The Continue Where Left Off carousel is deliberately left alone in that case: the
 * item was never played far enough to count as finished, and removing it here would add a way for
 * an item to leave the carousel that does not exist today. Within the last
 * [AUDIO_RESUME_FINISHED_THRESHOLD_MS] of the duration the item counts as finished: its
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
        if (info.totalDuration <= 0) return

        when {
            // Checked before the finished case on purpose: an item shorter than the two
            // thresholds combined sits inside the finished window while still at its start,
            // and treating that as played through would drop it from the carousel.
            info.currentPosition <= CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS ->
                mediaPlayerRepository.deleteMediaPlaybackInfo(info.mediaHandle)

            info.totalDuration - info.currentPosition <= AUDIO_RESUME_FINISHED_THRESHOLD_MS ->
                markFinished(info.mediaHandle)

            else -> mediaPlayerRepository.updateAndPersistPlaybackInfo(info)
        }
    }

    /**
     * Both cleanups run even when one fails. The last save of a session happens as the player is
     * torn down, with no further tick to retry, so a skipped step would strand the item in the
     * carousel with no playback record behind it.
     */
    private suspend fun markFinished(mediaHandle: Long) {
        val deleted = runCatching { mediaPlayerRepository.deleteMediaPlaybackInfo(mediaHandle) }
        val removed = runCatching { removeRecentlyUsedItemUseCase(mediaHandle) }
        deleted.exceptionOrNull()?.let { failure ->
            removed.exceptionOrNull()?.let(failure::addSuppressed)
            throw failure
        }
        removed.getOrThrow()
    }
}
