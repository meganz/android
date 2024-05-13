package mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo

/**
 * UI state for the OfflineFileInfoComposeViewModel
 * @property id Id of the node
 * @property handle handle of node
 * @property title title of the screen, node's name
 * @property totalSize Size of the node, for folder it will be the total size of its content
 * @property folderInfo number of files in a folder
 * @property addedTime creation time of the node
 * @property thumbnail thumbnail of the node
 * @property isFolder true if the node is a folder, false if it's a file
 * @property id id of node in database
 * @property parentId id of parent in database
 */
data class OfflineFileInfoUiState(
    val id: Int = 0,
    val handle: Long = 0,
    val parentId: Int = -1,
    val title: String = "",
    val totalSize: Long = 0L,
    val folderInfo: OfflineFolderInfo? = null,
    val addedTime: Long? = null,
    val thumbnail: String? = null,
    val isFolder: Boolean = false,
    val errorEvent: StateEventWithContent<Boolean> = consumed()
)