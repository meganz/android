package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.domain.entity.photos.ImageNodeInfo
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Lists a page of lightweight [ImageNodeInfo] refs for the viewer's ordering, keeping images and
 * videos and dropping SVGs.
 */
class ListTimelineImageNodeInfoByOffsetUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
    private val fileSystemRepository: FileSystemRepository,
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
        ).filter { fileSystemRepository.getFileTypeInfoByName(it.name).isValidMediaNode() }

    private fun FileTypeInfo.isValidMediaNode(): Boolean =
        (this is ImageFileTypeInfo && this !is SvgFileTypeInfo) || this is VideoFileTypeInfo
}
