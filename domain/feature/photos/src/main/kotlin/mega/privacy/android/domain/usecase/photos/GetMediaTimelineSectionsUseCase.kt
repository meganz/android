package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * The use case to get media timeline sections
 */
class GetMediaTimelineSectionsUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {

    /**
     * Get media timeline sections
     *
     * @param filter the [MediaTimelineFilter] describing the scope and granularity of the sections
     * @param order the [SortOrder] determining the order of the returned sections
     * @return the list of [MediaTimelineSection] matching the [filter]
     */
    suspend operator fun invoke(
        filter: MediaTimelineFilter,
        order: SortOrder,
    ): List<MediaTimelineSection> =
        photosRepository.getMediaTimelineSections(filter, order)
}
