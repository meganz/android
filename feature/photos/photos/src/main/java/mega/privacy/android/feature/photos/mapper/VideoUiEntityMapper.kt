package mega.privacy.android.feature.photos.mapper

import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import javax.inject.Inject

/**
 * The mapper class to convert the TypedVideoNode to VideoUIEntity
 */
class VideoUiEntityMapper @Inject constructor() {
    /**
     * Convert to VideoNode to VideoUIEntity
     */
    operator fun invoke(
        typedVideoNode: TypedVideoNode,
    ) = VideoUiEntity(
        id = typedVideoNode.id,
        parentId = typedVideoNode.parentId,
        name = typedVideoNode.name,
        description = typedVideoNode.description,
        tags = typedVideoNode.tags,
        size = typedVideoNode.size,
        fileTypeInfo = typedVideoNode.type,
        duration = typedVideoNode.duration,
        isFavourite = typedVideoNode.isFavourite,
        isSharedItems = typedVideoNode.exportedData != null || typedVideoNode.isOutShared,
        nodeAvailableOffline = typedVideoNode.isAvailableOffline,
        nodeLabel = typedVideoNode.nodeLabel,
        elementID = typedVideoNode.elementID ?: typedVideoNode.id.longValue,
        isMarkedSensitive = typedVideoNode.isMarkedSensitive,
        isSensitiveInherited = typedVideoNode.isSensitiveInherited,
        watchedDate = typedVideoNode.watchedTimestamp,
        collectionTitle = typedVideoNode.collectionTitle,
        hasThumbnail = typedVideoNode.hasThumbnail,
    )
}