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
 * @property thumbnailData the header image request for files — upgraded to a full-resolution preview
 * for image/video nodes; null for folders
 * @property durationText the formatted playback duration for audio/video nodes, or null otherwise
 * @property fileTypeExtension the file extension for files (e.g. "pdf"), or null for folders
 * @property sizeInBytes the file size in bytes (0 for folders; folder size arrives with folder stats)
 * @property creationTime the node creation time in seconds, or null if unknown
 * @property modificationTime the file modification time in seconds, or null for folders
 * @property nodeSourceType where the node lives (Cloud Drive / Rubbish Bin / Incoming Shares); drives
 * the localized location root label built in the UI. Null while unresolved.
 * @property locationFolders the containing-folder names below the root (empty when the node is in the root)
 * @property locationDestinations the navigation back stack that opens the node's containing folder
 * @property coordinates the media GPS coordinates, or null when the node has no valid location
 * @property locationCaption the reverse-geocoded place name for [coordinates], or null when unresolved
 * @property sharedContactCount the number of contacts this node is shared with (0 when not an outgoing share)
 * @property ownerName the display name (or email) of the incoming-share owner, or null when not an incoming share
 * @property ownerEmail the email of the incoming-share owner, or null when not an incoming share
 * @property versionCount the number of versions of a file (0 when none / not a file)
 * @property numberOfVersions the number of versioned files inside a folder (0 when none / not a folder)
 * @property currentVersionsSizeInBytes the total size of the folder's current versions
 * @property previousVersionsSizeInBytes the total size of the folder's previous versions
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
    val durationText: String? = null,
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
    val coordinates: Coordinates? = null,
    val locationCaption: String? = null,
    val sharedContactCount: Int = 0,
    val ownerName: String? = null,
    val ownerEmail: String? = null,
    val versionCount: Int = 0,
    val numberOfVersions: Int = 0,
    val currentVersionsSizeInBytes: Long = 0L,
    val previousVersionsSizeInBytes: Long = 0L,
) {
    /**
     * Whether to show the file's "Versions" row (navigates to the version history). Only files with
     * at least one version have it.
     */
    val showFileVersions: Boolean
        get() = isFile && versionCount > 0

    /**
     * Whether the node is an outgoing share (an owned folder shared with at least one contact);
     * drives the "Shared with" section. Outgoing shares only apply to folders, never files.
     */
    val isOutgoingShare: Boolean
        get() = !isFile && sharedContactCount > 0

    /**
     * Whether to show the folder's version sections (Versions / Current versions / Previous
     * versions). Only folders containing versioned files have them.
     */
    val showFolderVersions: Boolean
        get() = !isFile && numberOfVersions > 0

    /**
     * Whether the node is an incoming share (a folder shared with the current user by its owner);
     * drives the "Owner" and "Permissions" sections. Determined by the presence of the owner.
     */
    val isIncomingShare: Boolean
        get() = ownerEmail != null

    /**
     * The coordinates whose location map should be shown, or null when there is nothing to show:
     * the node has no valid location, or the current user is not the owner.
     */
    val mapCoordinates: Coordinates?
        get() = coordinates?.takeIf { accessPermission == AccessPermission.OWNER }

    /**
     * The description is editable only outside the rubbish bin / Backups and with write access.
     */
    val canEditDescription: Boolean
        get() = !isNodeInRubbish && !isNodeInBackups &&
                (accessPermission == AccessPermission.FULL ||
                        accessPermission == AccessPermission.OWNER)

    /**
     * Tags can be edited only outside the rubbish bin / Backups and with write access.
     */
    val canEditTags: Boolean
        get() = !isNodeInRubbish && !isNodeInBackups &&
                (accessPermission == AccessPermission.FULL ||
                        accessPermission == AccessPermission.OWNER)

    /**
     * The tags section is shown for any accessible node outside the rubbish bin / Backups; whether it
     * is also editable is [canEditTags].
     */
    val canShowTags: Boolean
        get() = !isNodeInRubbish && !isNodeInBackups &&
                accessPermission != AccessPermission.UNKNOWN
}
