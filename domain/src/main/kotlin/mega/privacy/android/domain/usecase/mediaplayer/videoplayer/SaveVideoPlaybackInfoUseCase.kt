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
        if (info.currentPosition <= CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS) return

        if (info.totalDuration - info.currentPosition < CWLO_NEAR_COMPLETION_THRESHOLD_MS) {
            markWatchedThrough(info)
        } else {
            mediaPlayerRepository.updatePlaybackInformation(
                PlaybackInformation(
                    mediaId = info.mediaHandle,
                    totalDuration = info.totalDuration,
                    currentPosition = info.currentPosition,
                )
            )
            persistVideoPlaybackInfoUseCase(info)
        }
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

        val failure = deleted.exceptionOrNull()
            ?: removed.exceptionOrNull()
            ?: persisted.exceptionOrNull()
            ?: return
        listOfNotNull(
            deleted.exceptionOrNull(),
            removed.exceptionOrNull(),
            persisted.exceptionOrNull(),
        ).filterNot { it === failure }.forEach(failure::addSuppressed)
        throw failure
    }
}
