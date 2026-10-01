package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * Use case to save video recently watched
 */
class SaveVideoRecentlyWatchedUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository,
) {

    /**
     * Save video recently watched
     *
     * @param handle the video handle
     * @param timestamp saved timestamp
     * @param collectionId collection id of the video
     */
    suspend operator fun invoke(
        handle: Long,
        timestamp: Long,
        collectionId: Long = 0,
        collectionTitle: String? = null,
    ) =
        videoSectionRepository.saveVideoRecentlyWatched(
            handle,
            timestamp,
            collectionId,
            collectionTitle
        )
}