package mega.privacy.android.feature.cloudexplorer.presentation.uploadscanneddocument

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.CLOUD_TAB_INDEX
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.INCOMING_TAB_INDEX
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.ShareToMegaUpload
import mega.privacy.android.navigation.destination.DiscardScanWarningDialogNavKey
import mega.privacy.android.navigation.destination.UploadScannedDocumentNavKey
import mega.privacy.android.shared.transfers.components.rememberUploadUrisEventState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UploadScannedDocumentScreen(
    uiState: UploadScannedDocumentsUiState,
    startNavKey: UploadScannedDocumentNavKey,
    onStartUpload: (TransferTriggerEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
) {
    if (uiState is UploadScannedDocumentsUiState.Data) {
        val uploadUrisEventState = rememberUploadUrisEventState()
        var folderPickedIdLong by rememberSaveable { mutableLongStateOf(-1L) }
        val folderPickedId = NodeId(folderPickedIdLong)
        var isProcessingAction by rememberSaveable { mutableStateOf(false) }
        val shareUris = listOf(uiState.uriPath)
        val tabIndex = if (startNavKey.nodeSourceType == NodeSourceType.INCOMING_SHARES) {
            INCOMING_TAB_INDEX
        } else {
            CLOUD_TAB_INDEX
        }
        val showDiscardScanWarning: () -> Unit = {
            onNavigate(
                DiscardScanWarningDialogNavKey(
                    hasMultipleScans = startNavKey.hasMultipleScans,
                    startNavKey = startNavKey,
                )
            )
        }

        BackHandler { showDiscardScanWarning() }

        ExplorerScreen(
            explorerMode = ExplorerMode.SaveScannedDocument,
            startNavKey = startNavKey,
            isInnerNavigation = false,
            nodeExplorerId = uiState.rootNodeId,
            nodeSourceType = startNavKey.nodeSourceType,
            shareUris = shareUris,
            tabIndex = tabIndex,
            onCloseExplorerScreen = showDiscardScanWarning,
            onNavigateBack = showDiscardScanWarning,
            onNavigate = onNavigate,
            isProcessingAction = isProcessingAction,
            onFolderPicked = { nodeId ->
                isProcessingAction = true
                folderPickedIdLong = nodeId.longValue
                uploadUrisEventState.trigger(shareUris.map { it.toUri() })
            },
        )

        ShareToMegaUpload(
            parentNodeId = folderPickedId,
            pitagTrigger = PitagTrigger.Scanner,
            uploadUrisEventState = uploadUrisEventState,
            onStartUpload = onStartUpload,
            onCloseExplorerScreen = onNavigateBack,
            onNavigate = onNavigate,
        )
    }
}
