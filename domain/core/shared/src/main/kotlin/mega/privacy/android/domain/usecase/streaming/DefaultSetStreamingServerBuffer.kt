package mega.privacy.android.domain.usecase.streaming

import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.repository.StreamingServerRepository
import javax.inject.Inject

/**
 * Default set streaming server buffer
 *
 *
 * @param environmentRepository
 * @param streamingServerRepository
 */
class DefaultSetStreamingServerBuffer @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
    private val streamingServerRepository: StreamingServerRepository,
) : SetStreamingServerBuffer {
    override suspend fun invoke() {
        if ((environmentRepository.getDeviceMemorySizeInBytes() ?: 0) > LARGE_BUFFER_THRESHOLD) {
            streamingServerRepository.setMaxBufferSize(BUFFER_SIZE_32MB)
        } else {
            streamingServerRepository.setMaxBufferSize(BUFFER_SIZE_16MB)
        }
    }

    companion object {
        const val LARGE_BUFFER_THRESHOLD = 1_073_741_824L
        const val BUFFER_SIZE_16MB = 16_777_216
        const val BUFFER_SIZE_32MB = 33_554_432
    }
}