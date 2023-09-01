package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * The use case for getting videos by search type api
 */
class GetVideosBySearchTypeUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) {

    /**
     * Getting videos by search type api
     *
     * @param handle parent handle
     * @param order SortOrder
     * @return video nodes
     */
    suspend operator fun invoke(handle: Long, order: SortOrder) =
        mediaPlayerRepository.getVideosBySearchType(handle, order)?.map { addNodeType(it) }
}