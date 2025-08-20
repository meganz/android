package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.model.SnackBarAttributes
import mega.android.core.ui.model.SnackBarDuration
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberNodeActionHandler
import mega.privacy.android.core.nodecomponents.list.NodeListViewItem
import mega.privacy.android.core.nodecomponents.mapper.NodeBottomSheetState
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeActionModeMenuItem
import mega.privacy.android.core.nodecomponents.model.text
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeOptionsBottomSheetRoute(
    navigationHandler: NavigationHandler,
    onDismiss: () -> Unit,
    nodeId: Long,
    nodeSourceType: NodeSourceType,
    onTransfer: (TransferTriggerEvent) -> Unit,
    nodeOptionsActionViewModel: NodeOptionsActionViewModel = hiltViewModel(),
    actionHandler: NodeActionHandler = rememberNodeActionHandler(nodeOptionsActionViewModel),
    viewModel: NodeOptionsBottomSheetViewModel = hiltViewModel(),
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nodeOptionActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        keyboardController?.hide()
        viewModel.getBottomSheetOptions(nodeId, nodeSourceType)
    }

    EventEffect(
        event = nodeOptionActionState.downloadEvent,
        onConsumed = nodeOptionsActionViewModel::markDownloadEventConsumed,
        action = onTransfer
    )

    NodeOptionsBottomSheetContent(
        uiState = uiState,
        navigationHandler = navigationHandler,
        actionHandler = actionHandler,
        onDismiss = onDismiss,
        onConsumeErrorState = viewModel::onConsumeErrorState,
    )
}

/**
 * Node options bottom sheet content for the node-components module
 * Uses NodeActionLauncher interface to launch activities from app module
 * while handling callbacks through NodeOptionsActionViewModel
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NodeOptionsBottomSheetContent(
    uiState: NodeBottomSheetState,
    navigationHandler: NavigationHandler,
    actionHandler: NodeActionHandler,
    onDismiss: () -> Unit,
    onConsumeErrorState: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackBarHostState.current
    val context = LocalContext.current

    fun showSnackbar(attributes: SnackBarAttributes) {
        coroutineScope.launch {
            attributes.message?.let { message ->
                snackbarHostState?.showSnackbar(
                    message = message,
                    actionLabel = attributes.action,
                    duration = when (attributes.duration) {
                        SnackBarDuration.Long -> SnackbarDuration.Long
                        SnackBarDuration.Short -> SnackbarDuration.Short
                        SnackBarDuration.Indefinite -> SnackbarDuration.Indefinite
                    },
                )
            }
        }
    }

    EventEffect(
        event = uiState.error,
        onConsumed = onConsumeErrorState,
        action = {
            Timber.e(it)
            onDismiss()
        },
    )

    if (uiState.node != null) {
        NodeListViewItem(
            modifier = Modifier.semantics { testTagsAsResourceId = true },
            title = uiState.node.name,
            titleColor = if (uiState.node.isTakenDown) TextColor.Error else TextColor.Primary,
            subtitle = uiState.node.subtitle.text(),
            showVersion = uiState.node.hasVersion,
            icon = uiState.node.iconRes,
            thumbnailData = uiState.node.thumbnailData,
            accessPermissionIcon = uiState.node.accessPermissionIcon,
            onItemClicked = {}
        )
    }

    LazyColumn(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
        items(uiState.actions) { item: NodeActionModeMenuItem ->
            item.control(
                BottomSheetClickHandler(
                    onDismiss = onDismiss,
                    actionHandler = actionHandler,
                    navigationHandler = navigationHandler,
                    coroutineScope = coroutineScope,
                    context = context,
                    snackbarHandler = ::showSnackbar
                )
            )
        }
    }
}