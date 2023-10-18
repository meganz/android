package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality

/**
 * Repository related to video operations
 */
interface VideoRepository {

    /**
     * Compress a Video given his file path
     * @param root
     * @param filePath
     * @param newFilePath
     * @param quality
     * @return flow of [VideoCompressionState]
     */
    fun compressVideo(
        root: String,
        filePath: String,
        newFilePath: String,
        quality: VideoQuality,
    ): Flow<VideoCompressionState>
}
