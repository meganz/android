package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * Repository related to video operations
 */
interface VideoRepository {

    /**
     * Compress a Video given his file path
     * @param root
     * @param original
     * @param newFilePath
     * @param quality
     * @return flow of [VideoCompressionState]
     */
    fun compressVideo(
        root: String,
        original: UriPath,
        newFilePath: String,
        quality: VideoQuality,
    ): Flow<VideoCompressionState>
}
