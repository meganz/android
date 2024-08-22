package mega.privacy.android.app.presentation.videosection.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.node.TypedVideoNode
import java.io.File
import javax.inject.Inject

/**
 * The mapper class to convert the TypedVideoNode to VideoUIEntity
 */
class VideoUIEntityMapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
) {
    /**
     * Convert to VideoNode to VideoUIEntity
     */
    operator fun invoke(
        typedVideoNode: TypedVideoNode,
    ) = VideoUIEntity(
        id = typedVideoNode.id,
        parentId = typedVideoNode.parentId,
        name = typedVideoNode.name,
        size = typedVideoNode.size,
        fileTypeInfo = typedVideoNode.type,
        durationString = durationInSecondsTextMapper(typedVideoNode.duration),
        duration = typedVideoNode.duration,
        thumbnail = typedVideoNode.thumbnailPath?.let { File(it) },
        isFavourite = typedVideoNode.isFavourite,
        isSharedItems = typedVideoNode.exportedData != null || typedVideoNode.isOutShared,
        nodeAvailableOffline = typedVideoNode.isAvailableOffline,
        label = typedVideoNode.label,
        elementID = typedVideoNode.elementID,
        isMarkedSensitive = typedVideoNode.isMarkedSensitive,
        isSensitiveInherited = typedVideoNode.isSensitiveInherited,
        watchedDate = TimeUtils.formatRecentlyWatchedDate(typedVideoNode.watchedTimestamp, context)
    )
}
