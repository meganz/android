package mega.privacy.android.app.presentation.offline.action.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import java.io.File

/**
 * UI State for [OfflineNodeActionsViewModel]
 *
 * @property shareFilesEvent Event to share files
 * @property openFileEvent Event to open file, contains offline information, file and file type info
 * @property sharesNodeLinksEvent Event to share nodes, contains first node's name and links
 */
data class OfflineNodeActionsUiState(
    val shareFilesEvent: StateEventWithContent<List<File>> = consumed(),
    val openFileEvent: StateEventWithContent<OfflineNodeActionUiEntity> = consumed(),
    val sharesNodeLinksEvent: StateEventWithContent<Pair<String?, String>> = consumed(),
)