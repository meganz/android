package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * Use case for setting the value of AudioContinuousPlaybackEnabled.
 *
 * Repeating the queue relies on playback continuing past the current item, so disabling
 * continuous playback also clears a [RepeatToggleMode.REPEAT_ALL] mode. The repeat mode is
 * written first, leaving every intermediate state an observer can see a supported one.
 *
 * Setting the value here is always the user's own choice, so it clears the record of continuous
 * playback having been turned on automatically by [SetAudioRepeatModeUseCase]. Without that,
 * leaving repeat-queue later would turn off a setting the user had since chosen themselves.
 */
class SetAudioContinuousPlaybackEnabledUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Invoke
     *
     * @param value true is playing the next item automatically, otherwise is false.
     */
    suspend operator fun invoke(value: Boolean) {
        mediaPlayerRepository.setAudioContinuousPlaybackAutoEnabled(false)
        if (!value &&
            mediaPlayerRepository.monitorAudioRepeatMode().first() == RepeatToggleMode.REPEAT_ALL
        ) {
            mediaPlayerRepository.setAudioRepeatMode(RepeatToggleMode.REPEAT_NONE.ordinal)
        }
        mediaPlayerRepository.setAudioContinuousPlaybackEnabled(value)
    }
}
