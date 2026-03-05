package mega.privacy.android.app.textEditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.flow.distinctUntilChanged
import mega.privacy.android.core.nodecomponents.mapper.ViewTypeToNodeSourceTypeMapper
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetResult
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.feature.texteditor.presentation.TextEditorComposeViewModel
import mega.privacy.android.feature.texteditor.presentation.TextEditorScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyTextEditorNavKey

/**
 * Returns true when the node options bottom sheet result indicates the editor should close
 * (e.g. user navigated away or triggered a transfer — the current node may no longer be valid).
 */
internal fun shouldCloseTextEditorOnNodeOptionsResult(
    result: NodeOptionsBottomSheetResult?,
): Boolean =
    result is NodeOptionsBottomSheetResult.Navigation ||
            result is NodeOptionsBottomSheetResult.Transfer

/**
 * Legacy text editor destination. When [LegacyTextEditorNavKey.isTextEditorComposeEnabled] is true,
 * shows [TextEditorScreen]; otherwise starts [TextEditorActivity] and pops this destination.
 * Tapping More opens the Node Options Bottom Sheet. When the user deletes or moves the node
 * (result Navigation or Transfer), the editor is closed.
 */
fun EntryProviderScope<NavKey>.legacyTextEditorScreen(
    navigationHandler: NavigationHandler,
    viewTypeToNodeSourceTypeMapper: ViewTypeToNodeSourceTypeMapper,
    transferHandler: TransferHandler,
) {
    entry<LegacyTextEditorNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        TextEditorEntry(
            navKey = key,
            navigationHandler = navigationHandler,
            viewTypeToNodeSourceTypeMapper = viewTypeToNodeSourceTypeMapper,
            transferHandler = transferHandler,
        )
    }
}

@Composable
private fun TextEditorEntry(
    navKey: LegacyTextEditorNavKey,
    navigationHandler: NavigationHandler,
    viewTypeToNodeSourceTypeMapper: ViewTypeToNodeSourceTypeMapper,
    transferHandler: TransferHandler,
) {
    val context = LocalContext.current
    val removeDestination: () -> Unit = { navigationHandler.back() }

    // Close editor when user deletes/moves the node or navigates away from the sheet.
    LaunchedEffect(Unit) {
        navigationHandler.monitorResult<NodeOptionsBottomSheetResult>(NodeOptionsBottomSheetNavKey.RESULT)
            .distinctUntilChanged()
            .collect { result ->
                if (shouldCloseTextEditorOnNodeOptionsResult(result)) {
                    navigationHandler.clearResult(NodeOptionsBottomSheetNavKey.RESULT)
                    removeDestination()
                }
            }
    }

    if (navKey.isTextEditorComposeEnabled) {
        val mode = TextEditorMode.entries.find { it.value == navKey.mode } ?: TextEditorMode.View
        val topBarSlots = computeTextEditorTopBarSlots(navKey.nodeSourceType, mode)
        val viewModel =
            hiltViewModel<TextEditorComposeViewModel, TextEditorComposeViewModel.Factory> { factory ->
                factory.create(
                    TextEditorComposeViewModel.Args(
                        nodeHandle = navKey.nodeHandle,
                        mode = mode,
                        nodeSourceType = navKey.nodeSourceType,
                        fileName = navKey.fileName,
                        topBarSlots = topBarSlots,
                    )
                )
            }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        EventEffect(
            event = uiState.transferEvent,
            onConsumed = viewModel::consumeTransferEvent,
        ) { content ->
            transferHandler.setTransferEvent(content)
        }
        TextEditorScreen(
            viewModel = viewModel,
            onBack = removeDestination,
            onOpenNodeOptions = {
                navigationHandler.navigate(
                    NodeOptionsBottomSheetNavKey(
                        nodeHandle = navKey.nodeHandle,
                        nodeSourceType = viewTypeToNodeSourceTypeMapper(navKey.nodeSourceType),
                    )
                )
            },
        )
    } else {
        LaunchedEffect(Unit) {
            context.startActivity(
                TextEditorActivity.createIntent(
                    context = context,
                    nodeHandle = navKey.nodeHandle,
                    mode = navKey.mode,
                    nodeSourceType = navKey.nodeSourceType,
                    fileName = navKey.fileName,
                )
            )
            removeDestination()
        }
    }
}
