package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo

/**
 * Map file info to duration
 */
typealias FileDurationMapper = (@JvmSuppressWildcards FileTypeInfo) -> @JvmSuppressWildcards Int?

/**
 * Map [FileTypeInfo] to [Int]
 */

internal fun toDuration(fileInfo: FileTypeInfo): Int? {
    return (fileInfo as? AudioFileTypeInfo)?.duration
        ?: (fileInfo as? VideoFileTypeInfo)?.duration
}
