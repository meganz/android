package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.qualifier.AudioPlaybackModeMutex
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * Use case for setting audio repeat mode.
 *
 * Repeating the queue relies on playback continuing past the current item, so selecting
 * [RepeatToggleMode.REPEAT_ALL] turns continuous playback on. That is remembered, and leaving
 * repeat-queue turns it back off again, so passing through repeat-queue on the way to another
 * mode does not silently leave the user's continuous-playback setting changed.
 *
 * The writes are ordered so that every intermediate state an observer can see is a supported
 * one: continuous playback is turned on before repeat-queue is set, and turned back off only
 * after repeat-queue has been left. That ordering only holds within a single invocation, so
 * the whole sequence runs under the [AudioPlaybackModeMutex] mutex.
 */
class SetAudioRepeatModeUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    @AudioPlaybackModeMutex private val audioPlaybackModeMutex: Mutex,
) {
    /**
     * Invoke
     *
     * @param value Int value of audio repeat mode
     */
    suspend operator fun invoke(value: Int) = audioPlaybackModeMutex.withLock {
        if (value == RepeatToggleMode.REPEAT_ALL.ordinal) {
            if (mediaPlayerRepository.monitorAudioContinuousPlaybackEnabled().first() == false) {
                mediaPlayerRepository.setAudioContinuousPlaybackEnabled(true)
                mediaPlayerRepository.setAudioContinuousPlaybackAutoEnabled(true)
            }
            mediaPlayerRepository.setAudioRepeatMode(value)
            return@withLock
        }

        mediaPlayerRepository.setAudioRepeatMode(value)
        if (mediaPlayerRepository.monitorAudioContinuousPlaybackAutoEnabled().first() == true) {
            mediaPlayerRepository.setAudioContinuousPlaybackAutoEnabled(false)
            mediaPlayerRepository.setAudioContinuousPlaybackEnabled(false)
        }
    }
}
