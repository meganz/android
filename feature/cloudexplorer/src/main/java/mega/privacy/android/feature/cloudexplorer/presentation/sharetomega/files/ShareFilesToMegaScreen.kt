package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.files

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.ShareToMegaUpload
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.shared.transfers.components.rememberUploadUrisEventState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareFilesToMegaScreen(
    uiState: ShareFilesToMegaUiState,
    startNavKey: ExplorerNavKey,
    onStartUpload: (TransferTriggerEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    monitorResult: (String) -> Flow<Any?> = { emptyFlow() },
    clearResult: (String) -> Unit = {},
) {
    if (uiState is ShareFilesToMegaUiState.Loading) {
        //See if we need a loading view
    } else {
        val dataUiState = uiState as ShareFilesToMegaUiState.Data
        val uploadUrisEventState = rememberUploadUrisEventState()
        var folderPickedIdLong by rememberSaveable { mutableLongStateOf(-1L) }
        val folderPickedId = NodeId(folderPickedIdLong)
        var isProcessingAction by rememberSaveable { mutableStateOf(false) }
        var prepareChatsEvent: StateEvent by remember { mutableStateOf(consumed) }
        var chatUploadIds by rememberSaveable { mutableStateOf<List<Long>?>(null) }

        ExplorerScreen(
            explorerMode = ExplorerMode.ShareFilesToMega,
            startNavKey = startNavKey,
            isInnerNavigation = false,
            nodeExplorerId = dataUiState.rootNodeId,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            shareUris = dataUiState.shareUris,
            onCloseExplorerScreen = onNavigateBack,
            onNavigateBack = onNavigateBack,
            onNavigate = onNavigate,
            isProcessingAction = isProcessingAction,
            onFolderPicked = { nodeId ->
                isProcessingAction = true
                folderPickedIdLong = nodeId.longValue
                uploadUrisEventState.trigger(dataUiState.shareUris.map { it.toUri() })
            },
            onChatsSelected = {
                isProcessingAction = true
                prepareChatsEvent = triggered
            },
            prepareChatsEvent = prepareChatsEvent,
            onPrepareChatsConsumed = { prepareChatsEvent = consumed },
            onChatsReadyToShare = { chatIds ->
                chatUploadIds = chatIds
                uploadUrisEventState.trigger(dataUiState.shareUris.map { it.toUri() })
            },
            monitorResult = monitorResult,
            clearResult = clearResult,
        )

        ShareToMegaUpload(
            parentNodeId = folderPickedId,
            chatIds = chatUploadIds,
            uploadUrisEventState = uploadUrisEventState,
            onStartUpload = onStartUpload,
            onCloseExplorerScreen = onNavigateBack,
            onNavigate = onNavigate,
        )
    }
}