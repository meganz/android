package mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo

/**
 * UI state for the OfflineFileInfoComposeViewModel
 * @property title title of the screen, node's name
 * @property totalSize Size of the node, for folder it will be the total size of its content
 * @property folderInfo number of files in a folder
 * @property addedTime creation time of the node
 * @property thumbnail thumbnail of the node
 * @property isFolder true if the node is a folder, false if it's a file
 *
 */
internal data class OfflineFileInfoUiState(
    val title: String = "",
    val totalSize: Long = 0L,
    val folderInfo: OfflineFolderInfo? = null,
    val addedTime: Long? = null,
    val thumbnail: String? = null,
    val isFolder: Boolean = false,
    val errorEvent: StateEventWithContent<Boolean> = consumed()
)