package mega.privacy.android.app.mediaplayer.mapper

import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import java.io.File

/**
 * Mapper to convert [PlaylistItem]
 */
typealias PlaylistItemMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards File?,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Int,
) -> @JvmSuppressWildcards PlaylistItem

internal fun toPlaylistItemMapper(
    nodeHandle: Long,
    nodeName: String,
    thumbnailFile: File?,
    index: Int,
    type: Int,
    size: Long,
    duration: Int,
): PlaylistItem =
    PlaylistItem(
        nodeHandle = nodeHandle,
        nodeName = nodeName,
        thumbnail = thumbnailFile,
        index = index,
        type = type,
        size = size,
        duration = duration
    )