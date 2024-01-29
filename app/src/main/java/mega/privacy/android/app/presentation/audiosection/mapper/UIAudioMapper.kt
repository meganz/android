package mega.privacy.android.app.presentation.audiosection.mapper

import mega.privacy.android.app.presentation.audiosection.model.UIAudio
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.node.TypedAudioNode
import java.io.File
import javax.inject.Inject

/**
 * The mapper class to convert the AudioNode to UIAudio
 */
class UIAudioMapper @Inject constructor(
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
) {

    /**
     * Convert to AudioNode to UIAudio
     */
    operator fun invoke(
        typedAudioNode: TypedAudioNode,
    ) = UIAudio(
        id = typedAudioNode.id,
        name = typedAudioNode.name,
        size = typedAudioNode.size,
        duration = durationInSecondsTextMapper(typedAudioNode.duration),
        thumbnail = typedAudioNode.thumbnailPath?.let { File(it) },
        isFavourite = typedAudioNode.isFavourite,
        isExported = typedAudioNode.exportedData != null,
        isTakenDown = typedAudioNode.isTakenDown,
        hasVersions = typedAudioNode.hasVersion,
        modificationTime = typedAudioNode.modificationTime,
        label = typedAudioNode.label,
        nodeAvailableOffline = typedAudioNode.isAvailableOffline
    )
}