package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * Use case to remove recently watched item
 */
class RemoveRecentlyWatchedItemUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository,
) {
    /**
     * Remove video recently watched item
     *
     * @param handle removed item handle
     */
    suspend operator fun invoke(handle: Long) =
        videoSectionRepository.removeRecentlyWatchedItem(handle)
}