package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import kotlin.time.Duration

/**
 * Map file info to duration
 */
typealias FileDurationMapper = (@JvmSuppressWildcards FileTypeInfo) -> @JvmSuppressWildcards Duration?

/**
 * Map [FileTypeInfo] to [Duration]
 */

internal fun toDuration(fileInfo: FileTypeInfo): Duration? {
    return (fileInfo as? AudioFileTypeInfo)?.duration
        ?: (fileInfo as? VideoFileTypeInfo)?.duration
}
