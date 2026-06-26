package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Use case to list media timeline nodes using offset-based pagination.
 */
class ListMediaNodesByOffsetUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {

    /**
     * Invoke
     *
     * Pagination happens independently within the given [section]: its query is scoped to the
     * section's date range, so any position can be loaded directly by [offset] without paging
     * through the items before it.
     *
     * @param filter the [MediaTimelineFilter] describing the scope of the media to list
     * @param section the [MediaTimelineSection] whose media is being paginated
     * @param order the [SortOrder] to apply to the results
     * @param maxElements the maximum number of nodes to return for the page (0 = no limit)
     * @param offset the zero-based index, within the section, of the first node to return
     * @return the page of media nodes as a list of [TypedFileNode], keeping only images and videos
     * (SVGs are excluded)
     */
    suspend operator fun invoke(
        filter: MediaTimelineFilter,
        section: MediaTimelineSection,
        order: SortOrder,
        maxElements: Int,
        offset: Long,
    ): List<TypedFileNode> =
        photosRepository
            .listMediaNodesByPage(
                filter = filter,
                section = section,
                order = order,
                maxElements = maxElements,
                offset = offset,
            )
            .filter { it.isValidMediaNode() }

    private fun TypedFileNode.isValidMediaNode(): Boolean =
        (type is ImageFileTypeInfo && type !is SvgFileTypeInfo) || type is VideoFileTypeInfo
}
