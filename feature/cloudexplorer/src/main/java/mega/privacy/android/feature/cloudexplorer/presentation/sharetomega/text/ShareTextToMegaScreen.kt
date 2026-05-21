package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text

import android.webkit.URLUtil
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
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
import mega.privacy.android.navigation.destination.NewTextFileDialogNavKey
import mega.privacy.android.navigation.destination.NewURLFileDialogNavKey
import mega.privacy.android.navigation.destination.ShareTextToMegaNavKey
import mega.privacy.android.shared.transfers.components.rememberUploadUrisEventState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareTextToMegaScreen(
    uiState: ShareTextToMegaUiState,
    startNavKey: ShareTextToMegaNavKey,
    isProcessingAction: Boolean,
    onStartUpload: (TransferTriggerEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    onChatsSelected: () -> Unit,
    onFileUriConsumed: () -> Unit,
    monitorResult: (String) -> Flow<Any?> = { emptyFlow() },
    clearResult: (String) -> Unit = {},
) {
    when (uiState) {
        is ShareTextToMegaUiState.Loading -> {
            //See if we need a loading view
        }

        is ShareTextToMegaUiState.Data -> {
            val isURL = URLUtil.isHttpUrl(startNavKey.text) || URLUtil.isHttpsUrl(startNavKey.text)
            val uploadUrisEventState = rememberUploadUrisEventState()
            var folderPickedIdLong by rememberSaveable { mutableLongStateOf(-1L) }
            val folderPickedId = NodeId(folderPickedIdLong)
            var prepareChatsEvent: StateEvent by remember { mutableStateOf(consumed) }

            EventEffect(
                event = uiState.fileUri,
                onConsumed = onFileUriConsumed,
            ) { uri ->
                uploadUrisEventState.trigger(listOf(uri.toUri()))
            }

            ExplorerScreen(
                explorerMode = if (isURL) ExplorerMode.ShareURLToMega else ExplorerMode.ShareTextToMega,
                startNavKey = startNavKey,
                isInnerNavigation = false,
                nodeExplorerId = uiState.rootNodeId,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                onCloseExplorerScreen = onNavigateBack,
                onNavigateBack = onNavigateBack,
                onNavigate = onNavigate,
                isProcessingAction = isProcessingAction,
                onFolderPicked = { nodeId ->
                    folderPickedIdLong = nodeId.longValue
                    onNavigate(
                        if (isURL) {
                            NewURLFileDialogNavKey(parentNodeId = nodeId)
                        } else {
                            NewTextFileDialogNavKey(
                                parentNodeId = nodeId,
                                returnFileName = true,
                            )
                        }
                    )
                },
                onChatsSelected = {
                    onChatsSelected()
                    prepareChatsEvent = triggered
                },
                prepareChatsEvent = prepareChatsEvent,
                onPrepareChatsConsumed = { prepareChatsEvent = consumed },
                monitorResult = monitorResult,
                clearResult = clearResult,
            )

            ShareToMegaUpload(
                parentNodeId = folderPickedId,
                uploadUrisEventState = uploadUrisEventState,
                onStartUpload = onStartUpload,
                onCloseExplorerScreen = onNavigateBack,
                onNavigate = onNavigate,
            )
        }
    }
}
