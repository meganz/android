package mega.privacy.android.feature.photos.presentation.timeline.revamp.mapper

import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.feature.photos.model.MediaType
import mega.privacy.android.feature.photos.model.PhotosNodeContentItemV2
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import javax.inject.Inject

/**
 * Maps a [TypedFileNode] into a [PhotosNodeContentItemV2] for the timeline revamp grid.
 *
 * Only [PhotosNodeContentType.PhotoNode] items are produced; the revamp screen renders its own
 * section headers, so the date fields ([PhotosNodeContentItemV2.day]/[PhotosNodeContentItemV2.month]/
 * [PhotosNodeContentItemV2.year]) are unused and left at 0.
 */
internal class MediaTimelineNodeUiItemMapper @Inject constructor(
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
) {

    operator fun invoke(node: TypedFileNode): PhotosNodeContentItemV2 {
        val mediaType = if (node.type is ImageFileTypeInfo) MediaType.Image else MediaType.Video
        val duration = (node.type as? VideoFileTypeInfo)
            ?.let { durationInSecondsTextMapper(it.duration) }
            .orEmpty()
        return PhotosNodeContentItemV2(
            key = node.id.longValue,
            contentType = PhotosNodeContentType.PhotoNode,
            id = node.id.longValue,
            mediaType = mediaType,
            day = 0,
            month = 0,
            year = 0,
            fullModificationTime = node.modificationTime,
            thumbnailFilePath = node.thumbnailPath,
            previewFilePath = node.previewPath,
            extension = node.type.extension,
            isFavourite = node.isFavourite,
            isSensitive = node.isMarkedSensitive || node.isSensitiveInherited,
            duration = duration,
        )
    }
}
