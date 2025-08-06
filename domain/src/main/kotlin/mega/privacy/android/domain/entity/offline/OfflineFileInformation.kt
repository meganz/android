package mega.privacy.android.domain.entity.offline

import mega.privacy.android.domain.entity.FileTypeInfo
import java.util.concurrent.TimeUnit

/**
 * OfflineFileInformation
 *
 * @property nodeInfo [OfflineNodeInformation] containing the file information
 * @property totalSize Total size of the file
 * @property folderInfo [OfflineFolderInfo] containing the folder information if the file is inside a folder
 * @property fileTypeInfo [FileTypeInfo] containing the file type information
 * @property thumbnail Path to the thumbnail of the file, if available
 * @property absolutePath Absolute path of the file in the offline storage
 * @property addedTime Time when the file was added, in milliseconds
 */
data class OfflineFileInformation(
    val nodeInfo: OfflineNodeInformation,
    val totalSize: Long = 0L,
    val folderInfo: OfflineFolderInfo? = null,
    val fileTypeInfo: FileTypeInfo? = null,
    val thumbnail: String? = null,
    val absolutePath: String = "",
) : OfflineNodeInformation by nodeInfo {
    val addedTime = lastModifiedTime?.let { TimeUnit.MICROSECONDS.toMillis(it) }
}