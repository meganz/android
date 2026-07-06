package mega.privacy.android.feature.fileinfo.presentation.model

import androidx.annotation.DrawableRes
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.domain.entity.shares.AccessPermission

/**
 * UI state for the File Info screen.
 *
 * @property isLoading whether the node information is still being loaded
 * @property title the node name
 * @property isFile true if the node is a file, false if it is a folder
 * @property iconRes the header icon: the file-type icon for files, the folder icon for folders
 * @property thumbnailData the thumbnail request for files (image/video preview), or null for folders
 * @property fileTypeExtension the file extension for files (e.g. "pdf"), or null for folders
 * @property sizeInBytes the file size in bytes (0 for folders; folder size arrives with folder stats)
 * @property creationTime the node creation time in seconds, or null if unknown
 * @property modificationTime the file modification time in seconds, or null for folders
 * @property nodeSourceType where the node lives (Cloud Drive / Rubbish Bin / Incoming Shares); drives
 * the localized location root label built in the UI. Null while unresolved.
 * @property locationFolders the containing-folder names below the root (empty when the node is in the root)
 * @property locationDestinations the navigation back stack that opens the node's containing folder
 * @property descriptionText the node description, empty when none
 * @property tags the tags associated with the node
 * @property isTakenDown whether the node has been taken down
 * @property accessPermission the current user's access level for this node
 * @property isNodeInRubbish whether the node is in the rubbish bin
 * @property isNodeInBackups whether the node is in the Backups folder
 */
internal data class FileInfoUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val isFile: Boolean = true,
    @DrawableRes val iconRes: Int? = null,
    val thumbnailData: ThumbnailData? = null,
    val fileTypeExtension: String? = null,
    val sizeInBytes: Long = 0L,
    val creationTime: Long? = null,
    val modificationTime: Long? = null,
    val nodeSourceType: NodeSourceType? = null,
    val locationFolders: List<String> = emptyList(),
    val locationDestinations: List<NavKey>? = null,
    val descriptionText: String = "",
    val tags: List<String> = emptyList(),
    val isTakenDown: Boolean = false,
    val accessPermission: AccessPermission = AccessPermission.UNKNOWN,
    val isNodeInRubbish: Boolean = false,
    val isNodeInBackups: Boolean = false,
)
