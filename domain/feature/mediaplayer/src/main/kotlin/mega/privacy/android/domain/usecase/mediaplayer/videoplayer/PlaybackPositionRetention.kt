package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_NEAR_COMPLETION_THRESHOLD_MS
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo

/**
 * Whether a playback position is worth keeping in the shared playback info store.
 *
 * A position is dropped when the duration is unknown, when the user is back within
 * [CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS] of the start, or when the item has been watched to within
 * [CWLO_NEAR_COMPLETION_THRESHOLD_MS] of its end.
 *
 * Shared by [PersistVideoPlaybackInfoUseCase] and [MigrateVideoPlaybackInfosUseCase] so that a
 * record carried over from the legacy store is held to the same bar as one written today.
 */
internal fun MediaPlaybackInfo.isPlaybackPositionWorthKeeping(): Boolean {
    if (totalDuration <= 0) return false
    if (currentPosition <= CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS) return false
    return totalDuration - currentPosition >= CWLO_NEAR_COMPLETION_THRESHOLD_MS
}
