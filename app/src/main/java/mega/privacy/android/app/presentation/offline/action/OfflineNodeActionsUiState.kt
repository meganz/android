package mega.privacy.android.app.presentation.offline.action

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import java.io.File

/**
 * UI State for [OfflineNodeActionsViewModel]
 *
 * @property shareFilesEvent Event to share files
 * @property sharesNodeLinksEvent Event to share nodes, contains first node's name and links
 */
data class OfflineNodeActionsUiState(
    val shareFilesEvent: StateEventWithContent<List<File>> = consumed(),
    val sharesNodeLinksEvent: StateEventWithContent<Pair<String?, String>> = consumed(),
)