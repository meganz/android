package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_NEAR_COMPLETION_THRESHOLD_MS
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo.Companion.INVALID_MEDIA_HANDLE
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.continuewhereleftoff.RemoveRecentlyUsedItemUseCase
import javax.inject.Inject

/**
 * Writes one video playback snapshot to both stores of the dual-write phase: the DataStore path
 * the player still reads from, and the database through [PersistVideoPlaybackInfoUseCase].
 *
 * Snapshots that carry no handle, or whose duration is not known yet, are taken before the player
 * is prepared and are ignored: a zero duration reads as "watched through" and would drop a record
 * the user has not finished. Snapshots at or below [CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS] are
 * ignored too, so briefly opened items are never surfaced back as resumable.
 *
 * The DataStore write goes first because it is the store the player currently reads from: a
 * database failure must not cost the resume position that is actually used. Callers that need the
 * DataStore value to survive a process death must flush it with [SavePlaybackTimesUseCase]; this
 * use case only updates the in-memory map, matching the existing DataStore write path.
 */
class SaveVideoPlaybackInfoUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val removeRecentlyUsedItemUseCase: RemoveRecentlyUsedItemUseCase,
    private val persistVideoPlaybackInfoUseCase: PersistVideoPlaybackInfoUseCase,
) {
    /**
     * Save a video playback snapshot
     *
     * @param info the playback snapshot to save
     */
    suspend operator fun invoke(info: MediaPlaybackInfo) {
        if (info.mediaHandle == INVALID_MEDIA_HANDLE) return
        if (info.totalDuration <= 0) return

        when {
            // Checked before the near-completion case on purpose: an item shorter than the two
            // thresholds combined sits inside the near-completion window while still at its
            // start, and treating that as watched through would drop it from the carousel.
            info.currentPosition <= CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS -> discardPosition(info)

            info.totalDuration - info.currentPosition < CWLO_NEAR_COMPLETION_THRESHOLD_MS ->
                markWatchedThrough(info)

            else -> keepPosition(info)
        }
    }

    private suspend fun keepPosition(info: MediaPlaybackInfo) {
        mediaPlayerRepository.updatePlaybackInformation(
            PlaybackInformation(
                mediaId = info.mediaHandle,
                totalDuration = info.totalDuration,
                currentPosition = info.currentPosition,
            )
        )
        persistVideoPlaybackInfoUseCase(info)
    }

    /**
     * The user has moved back to the start, so the stored position no longer describes where they
     * were and is dropped from both stores.
     *
     * The Continue Where Left Off carousel is deliberately left alone: the item was never played
     * far enough to count as finished, and removing it here would add a way for an item to leave
     * the carousel that does not exist today.
     */
    private suspend fun discardPosition(info: MediaPlaybackInfo) {
        val discarded = runCatching {
            mediaPlayerRepository.deletePlaybackInformation(info.mediaHandle)
        }
        // Below the minimum playback threshold this deletes the database row rather than writing.
        val persisted = runCatching { persistVideoPlaybackInfoUseCase(info) }
        rethrowFirstFailure(discarded.exceptionOrNull(), persisted.exceptionOrNull())
    }

    /**
     * Every step runs even when an earlier one fails. The last save of a session happens as the
     * player is torn down, with no further tick to retry it, so a skipped step would leave the
     * two stores disagreeing about whether the item was watched through.
     */
    private suspend fun markWatchedThrough(info: MediaPlaybackInfo) {
        val deleted = runCatching {
            mediaPlayerRepository.deletePlaybackInformation(info.mediaHandle)
        }
        val removed = runCatching { removeRecentlyUsedItemUseCase(info.mediaHandle) }
        val persisted = runCatching { persistVideoPlaybackInfoUseCase(info) }
        rethrowFirstFailure(
            deleted.exceptionOrNull(),
            removed.exceptionOrNull(),
            persisted.exceptionOrNull(),
        )
    }

    private fun rethrowFirstFailure(vararg failures: Throwable?) {
        val raised = failures.filterNotNull()
        val failure = raised.firstOrNull() ?: return
        raised.filterNot { it === failure }.forEach(failure::addSuppressed)
        throw failure
    }
}
