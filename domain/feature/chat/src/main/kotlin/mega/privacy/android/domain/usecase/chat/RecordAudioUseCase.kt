package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.MediaRecorderRepository
import java.io.File
import javax.inject.Inject

/**
 * Use case to start recording audio from device microphone to the destination file
 */
class RecordAudioUseCase @Inject constructor(
    private val mediaRecorderRepository: MediaRecorderRepository,
) {
    /**
     * Creates a flow that starts recording when collecting its values begins and stops recording when cancelled. Meanwhile, it will emit the sampled current maximum amplitude.
     * @param destination where the recording will be stored
     * @return a flow with sampled current maximum amplitude.
     */
    operator fun invoke(destination: File) = mediaRecorderRepository.recordAudio(destination)
}