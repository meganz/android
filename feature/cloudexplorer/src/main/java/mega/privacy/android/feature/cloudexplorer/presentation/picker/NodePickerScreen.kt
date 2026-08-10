package mega.privacy.android.feature.cloudexplorer.presentation.picker

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.selectsyncfolder.SelectSyncFolderViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.selectsyncfolder.SyncFolderPickerEffects
import mega.privacy.android.feature.cloudexplorer.presentation.selectsyncfolder.rememberSyncFolderPicker
import mega.privacy.android.navigation.destination.ExplorerNavKey

/**
 * Shared entry screen for the explorer flows that open at the cloud-drive root and pick either a
 * destination folder ([onFolderPicked]) or a set of files ([onFilesPicked]); [ExplorerMode]
 * determines which action the explorer surfaces.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NodePickerScreen(
    uiState: NodePickerUiState,
    startNavKey: ExplorerNavKey,
    explorerMode: ExplorerMode,
    onNavigateBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    disabledNodeIds: Set<NodeId> = emptySet(),
    onFolderPicked: (NodeId) -> Unit = {},
    onFilesPicked: (List<NodeId>) -> Unit = {},
) {
    // Sync / stop-backup picker behaviour layered on top of the explorer (null for other modes).
    val syncFolderPicker = rememberSyncFolderPicker(
        explorerMode = explorerMode,
        startNavKey = startNavKey,
        folderHandle = SelectSyncFolderViewModel.INVALID_FOLDER_HANDLE,
    )

    if (uiState is NodePickerUiState.Data) {
        var isProcessingAction by rememberSaveable { mutableStateOf(false) }

        ExplorerScreen(
            explorerMode = explorerMode,
            startNavKey = startNavKey,
            isInnerNavigation = false,
            nodeExplorerId = uiState.rootNodeId,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            onCloseExplorerScreen = onNavigateBack,
            onNavigateBack = onNavigateBack,
            onNavigate = onNavigate,
            isProcessingAction = syncFolderPicker?.isProcessingAction ?: isProcessingAction,
            disabledNodeIds = disabledNodeIds,
            pickerRestrictions = syncFolderPicker?.restrictions,
            onFolderPicked = { nodeId ->
                if (syncFolderPicker != null) {
                    syncFolderPicker.onCurrentFolderSelected()
                } else {
                    isProcessingAction = true
                    onFolderPicked(nodeId)
                }
            },
            onFilesPicked = { nodeIds ->
                isProcessingAction = true
                onFilesPicked(nodeIds)
            },
        )
    }

    syncFolderPicker?.let {
        SyncFolderPickerEffects(
            uiState = it.uiState,
            syncPermissionsManager = it.syncPermissionsManager,
            onAction = it.onAction,
            onFolderConfirmed = onNavigateBack,
        )
    }
}
