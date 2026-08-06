package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.indicators.LargeInfiniteSpinnerIndicator
import mega.android.core.ui.extensions.delayedTrue
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.ShareToMegaUpload
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.transfers.components.rememberUploadUrisEventState
import mega.privacy.android.shared.transfers.model.UploadFileViewModel
import kotlin.time.Duration.Companion.milliseconds

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
    uploadFileViewModel: UploadFileViewModel = hiltViewModel(),
) {
    if (uiState is ShareFilesToMegaUiState.Loading) {
        // Grace delay so fast loads never flash the processing state.
        val shouldShowProcessing by delayedTrue(200.milliseconds)
        if (shouldShowProcessing) {
            ProcessingFilesView()
        }
    } else {
        val dataUiState = uiState as ShareFilesToMegaUiState.Data
        val uploadUiState by uploadFileViewModel.uiState.collectAsStateWithLifecycle()
        val uploadUrisEventState = rememberUploadUrisEventState()
        var folderPickedIdLong by rememberSaveable { mutableLongStateOf(-1L) }
        val folderPickedId = NodeId(folderPickedIdLong)
        var isProcessingAction by rememberSaveable { mutableStateOf(false) }
        var prepareChatsEvent: StateEvent by remember { mutableStateOf(consumed) }
        var chatUploadIds by rememberSaveable { mutableStateOf<List<Long>?>(null) }
        val snackbarHostState = LocalSnackBarHostState.current
        val resources = LocalResources.current

        LaunchedEffect(dataUiState.hasNoFilesToUpload) {
            if (dataUiState.hasNoFilesToUpload) {
                isProcessingAction = true
                snackbarHostState?.showAutoDurationSnackbar(
                    resources.getString(sharedR.string.unable_to_open_selected_file_message)
                )
                onNavigateBack()
            }
        }

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
            isProcessingAction = isProcessingAction || uploadUiState.isProcessing,
            onFolderPicked = { nodeId ->
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
            pitagTrigger = PitagTrigger.ShareFromApp,
            chatIds = chatUploadIds,
            uploadUrisEventState = uploadUrisEventState,
            onStartUpload = onStartUpload,
            onCloseExplorerScreen = onNavigateBack,
            onNavigate = onNavigate,
            viewModel = uploadFileViewModel,
        )
    }
}

@Composable
private fun ProcessingFilesView(modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.x24)
            .testTag(SHARE_FILES_TO_MEGA_PROCESSING_VIEW_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LargeInfiniteSpinnerIndicator(
            iconColor = IconColor.Primary
        )
        Spacer(Modifier.height(spacing.x8))
        MegaText(
            text = stringResource(sharedR.string.album_get_link_requesting_link_placeholder),
            textColor = TextColor.Primary,
            style = AppTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
@Preview
private fun ProcessingFilesViewPreview() {
    AndroidThemeForPreviews {
        ProcessingFilesView()
    }
}

internal const val SHARE_FILES_TO_MEGA_PROCESSING_VIEW_TAG =
    "share_files_to_mega:processing_view"
