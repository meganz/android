package mega.privacy.android.app.presentation.videosection.mapper

import mega.privacy.android.app.presentation.meeting.chat.mapper.DurationTextMapper
import mega.privacy.android.app.presentation.videosection.model.UIVideo
import mega.privacy.android.domain.entity.node.TypedVideoNode
import java.io.File
import javax.inject.Inject
import kotlin.time.DurationUnit

/**
 * The mapper class to convert the TypedVideoNode to UIVideo
 */
class UIVideoMapper @Inject constructor(
    private val durationTextMapper: DurationTextMapper,
) {
    /**
     * Convert to VideoNode to UIVideo
     */
    operator fun invoke(
        typedVideoNode: TypedVideoNode,
    ) = UIVideo(
        id = typedVideoNode.id,
        name = typedVideoNode.name,
        size = typedVideoNode.size,
        duration = durationTextMapper(typedVideoNode.duration, DurationUnit.SECONDS),
        thumbnail = typedVideoNode.thumbnailPath?.let { File(it) },
        isFavourite = typedVideoNode.isFavourite,
        nodeAvailableOffline = typedVideoNode.isAvailableOffline
    )
}
