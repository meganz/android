package mega.privacy.android.domain.usecase.video

import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.VideoRepository
import javax.inject.Inject

/**
 * Compress a video
 */
class CompressVideoUseCase @Inject constructor(
    private val videoRepository: VideoRepository,
) {
    /**
     * Compress a video
     *
     * @param rootPath
     * @param filePath
     * @param newFilePath
     * @param quality
     */
    operator fun invoke(
        rootPath: String,
        filePath: String,
        newFilePath: String,
        quality: VideoQuality,
    ) = videoRepository.compressVideo(
        root = rootPath,
        filePath = filePath,
        newFilePath = newFilePath,
        quality = quality,
    )

}
