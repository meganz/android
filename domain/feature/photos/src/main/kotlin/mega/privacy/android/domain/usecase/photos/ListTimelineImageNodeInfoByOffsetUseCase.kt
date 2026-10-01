package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.domain.entity.photos.ImageNodeInfo
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Lists a page of lightweight [ImageNodeInfo] refs for the viewer's ordering.
 *
 * The page is returned as the SDK reports it, SVGs included, so the viewer's ordering stays
 * index-aligned with the grid it was opened from — see [ListMediaNodesByOffsetUseCase].
 */
class ListTimelineImageNodeInfoByOffsetUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {
    suspend operator fun invoke(
        filter: MediaTimelineFilter,
        section: MediaTimelineSection?,
        order: SortOrder,
        maxElements: Int,
        offset: Long,
    ): List<ImageNodeInfo> =
        photosRepository.listImageNodeInfoByPage(
            filter = filter,
            section = section,
            order = order,
            maxElements = maxElements,
            offset = offset,
        )
}
