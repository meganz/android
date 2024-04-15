package mega.privacy.android.app.mediaplayer.mapper

import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Mapper to convert [PlaylistItem]
 */
class PlaylistItemMapper @Inject constructor(
    private val fileTypeIconMapper: FileTypeIconMapper,
) {
    /**
     * Convert to PlaylistItem
     *
     * @param nodeHandle node handle
     * @param nodeName node name
     * @param thumbnailFile thumbnail file path, null if not available
     * @param index the index used for seek to this item
     * @param type item type
     * @param size size of the node
     * @param duration the duration of media item
     * @param fileExtension the file extension of the node
     */
    operator fun invoke(
        nodeHandle: Long,
        nodeName: String,
        thumbnailFile: File?,
        index: Int,
        type: Int,
        size: Long,
        duration: Duration,
        fileExtension: String?,
    ) = PlaylistItem(
        nodeHandle = nodeHandle,
        nodeName = nodeName,
        thumbnail = thumbnailFile,
        index = index,
        type = type,
        size = size,
        duration = duration,
        icon = fileTypeIconMapper(fileExtension ?: "")
    )
}