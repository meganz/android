package mega.privacy.android.feature.cloudexplorer.presentation.selectsyncfolder

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.sync.folderpicker.SyncFolderPickerRestrictedNode

/**
 * UI state for the sync MEGA folder picker built on top of the cloud explorer.
 */
@Stable
internal sealed interface SelectSyncFolderUiState {

    /**
     * Initial state, shown while the displayed folder is being resolved.
     */
    data object Loading : SelectSyncFolderUiState

    /**
     * Loaded state.
     *
     * @property currentFolderId The folder whose children are displayed (the cloud drive root for
     * the entry screen).
     * @property restrictedNodes The children that cannot be selected because they are already used
     * by a sync or backup, keyed by node id.
     * @property isSelectEnabled Whether the current folder itself can be selected as the sync
     * target. False at the root, when a child is used by a sync/backup, or when the current folder
     * is an ancestor of an existing sync/backup.
     * @property removeConnectionNode The restricted node whose folder connection the user is asked
     * to remove, or null when no removal is in progress. Holds the backup id used by the action.
     * @property isProcessing Whether a folder selection is being validated and saved, used to block
     * further interaction while it runs.
     * @property disableBatteryOptimizationsEvent Emitted when the disable battery optimisation
     * dialog should be shown.
     * @property warningEvent Emitted with a message to surface to the user (conflict / error).
     * @property folderConfirmedEvent Emitted when the selected folder has been validated and saved,
     * so the picker can be closed.
     */
    data class Data(
        val currentFolderId: NodeId,
        val restrictedNodes: Map<NodeId, SyncFolderPickerRestrictedNode>,
        val isSelectEnabled: Boolean,
        val removeConnectionNode: SyncFolderPickerRestrictedNode?,
        val isProcessing: Boolean,
        val disableBatteryOptimizationsEvent: StateEvent,
        val warningEvent: StateEventWithContent<LocalizedText>,
        val folderConfirmedEvent: StateEvent,
    ) : SelectSyncFolderUiState
}

/**
 * Internal holder for the folder being displayed and the restrictions monitored for its children.
 */
internal data class SyncFolderPickerNodes(
    val currentFolderId: NodeId,
    val restrictedNodes: Map<NodeId, SyncFolderPickerRestrictedNode> = emptyMap(),
    val isSelectEnabled: Boolean = false,
)
