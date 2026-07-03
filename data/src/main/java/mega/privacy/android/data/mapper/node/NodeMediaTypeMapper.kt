package mega.privacy.android.data.mapper.node

import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Classifies a node as media (image / video / audio) from the media-info attributes the SDK
 * computes from the file content at upload, used to identify files whose name has no usable
 * extension. Returns null when the node carries no media info (the SDK only computes it for
 * audio/video, so images/PDF/text will usually return null here and need content detection).
 *
 * All attributes are -1 when not set.
 */
internal class NodeMediaTypeMapper @Inject constructor() {

    operator fun invoke(
        videoCodecId: Int,
        shortFormat: Int,
        width: Int,
        height: Int,
        duration: Int,
    ): FileTypeInfo? {
        val hasVideoCodec = videoCodecId != NOT_SET
        val hasMediaInfo = shortFormat != NOT_SET
        val hasDimensions = width != NOT_SET && height != NOT_SET
        val hasDuration = duration != NOT_SET && duration > 0
        return when {
            hasVideoCodec || (hasMediaInfo && hasDimensions) -> VideoFileTypeInfo(
                mimeType = "video/mp4",
                extension = "",
                duration = duration.coerceAtLeast(0).seconds,
            )

            hasMediaInfo || hasDuration -> AudioFileTypeInfo(
                mimeType = "audio/mpeg",
                extension = "",
                duration = duration.coerceAtLeast(0).seconds,
            )

            hasDimensions -> StaticImageFileTypeInfo(
                mimeType = "image/jpeg",
                extension = "",
            )

            else -> null
        }
    }

    private companion object {
        const val NOT_SET = -1
    }
}
