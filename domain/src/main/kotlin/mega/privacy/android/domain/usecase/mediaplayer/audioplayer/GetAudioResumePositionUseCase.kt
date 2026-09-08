package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import mega.privacy.android.domain.entity.continuewhereleftoff.AUDIO_RESUME_FINISHED_THRESHOLD_MS
import mega.privacy.android.domain.entity.continuewhereleftoff.AUDIO_RESUME_MINIMUM_DURATION_MS
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.domain.usecase.mediaplayer.GetMediaPlaybackInfoUseCase
import javax.inject.Inject

/**
 * Resolves the position an audio item should silently resume from when it is opened.
 *
 * Resume applies only to long-form audio: items whose recorded duration is not above
 * [AUDIO_RESUME_MINIMUM_DURATION_MS] (including records with an unknown duration) always
 * restart from the beginning. Saved positions within [AUDIO_RESUME_FINISHED_THRESHOLD_MS] of
 * the end count as played through, and positions past the recorded duration are stale records
 * (e.g. the file was replaced by a shorter version) — both restart from the beginning.
 *
 * The playback info store is shared with the video player, so records of any other media type
 * are ignored rather than resumed under the audio rules.
 */
class GetAudioResumePositionUseCase @Inject constructor(
    private val getMediaPlaybackInfoUseCase: GetMediaPlaybackInfoUseCase,
) {
    /**
     * @param handle the audio item's media handle
     * @return the position in milliseconds to resume from, or null to start from the beginning
     */
    suspend operator fun invoke(handle: Long): Long? {
        val info = getMediaPlaybackInfoUseCase(handle) ?: return null
        if (info.mediaType != MediaType.Audio) return null
        if (info.totalDuration <= AUDIO_RESUME_MINIMUM_DURATION_MS) return null
        return info.currentPosition.takeIf {
            it in 1 until info.totalDuration - AUDIO_RESUME_FINISHED_THRESHOLD_MS
        }
    }
}
