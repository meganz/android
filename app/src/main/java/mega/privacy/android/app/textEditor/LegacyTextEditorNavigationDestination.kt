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
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.core.nodecomponents.mapper.ViewTypeToNodeSourceTypeMapper
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.INCOMING_SHARES_ADAPTER
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.LINKS_ADAPTER
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.RUBBISH_BIN_ADAPTER
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

/** True when adapter is rubbish bin, offline, folder link, zip, file link, chat, or versions (hides Get Link and Edit). */
private fun inExcludedAdapterForGetLinkAndEdit(nodeSourceType: Int?): Boolean {
    if (nodeSourceType == null) return false
    return nodeSourceType in setOf(
        RUBBISH_BIN_ADAPTER,
        OFFLINE_ADAPTER,
        FOLDER_LINK_ADAPTER,
        ZIP_ADAPTER,
        FILE_LINK_ADAPTER,
        FROM_CHAT,
        VERSIONS_ADAPTER,
    )
}

/** True when Download should be shown for this source type (not offline, not rubbish bin). */
private fun shouldShowDownload(nodeSourceType: Int?): Boolean {
    if (nodeSourceType == null) return true
    return nodeSourceType != OFFLINE_ADAPTER && nodeSourceType != RUBBISH_BIN_ADAPTER
}

/** True when the editor was opened from a Shared folder (incoming/outgoing shares or links). Save will create a new file with (1)(2) naming instead of overwriting. */
private fun isFromSharedFolder(nodeSourceType: Int?): Boolean {
    if (nodeSourceType == null) return false
    return nodeSourceType in setOf(
        INCOMING_SHARES_ADAPTER,
        OUTGOING_SHARES_ADAPTER,
        LINKS_ADAPTER,
    )
}

/** True when Share should be shown for this source type (not folder link, versions, incoming shares, or chat). */
private fun shouldShowShare(nodeSourceType: Int?): Boolean {
    if (nodeSourceType == null) return true
    return nodeSourceType !in setOf(
        FOLDER_LINK_ADAPTER,
        VERSIONS_ADAPTER,
        INCOMING_SHARES_ADAPTER,
        FROM_CHAT,
    )
}

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
        val nodeSourceType = navKey.nodeSourceType
        TextEditorComposeContent(
            navKey = navKey,
            mode = mode,
            inExcludedAdapterForGetLinkAndEdit = inExcludedAdapterForGetLinkAndEdit(nodeSourceType),
            showDownload = shouldShowDownload(nodeSourceType),
            showShare = shouldShowShare(nodeSourceType),
            navigationHandler = navigationHandler,
            removeDestination = removeDestination,
            viewTypeToNodeSourceTypeMapper = viewTypeToNodeSourceTypeMapper,
            transferHandler = transferHandler,
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

@Composable
private fun TextEditorComposeContent(
    navKey: LegacyTextEditorNavKey,
    mode: TextEditorMode,
    inExcludedAdapterForGetLinkAndEdit: Boolean,
    showDownload: Boolean,
    showShare: Boolean,
    navigationHandler: NavigationHandler,
    removeDestination: () -> Unit,
    viewTypeToNodeSourceTypeMapper: ViewTypeToNodeSourceTypeMapper,
    transferHandler: TransferHandler,
) {
    val viewModel =
        hiltViewModel<TextEditorComposeViewModel, TextEditorComposeViewModel.Factory> { factory ->
            factory.create(
                TextEditorComposeViewModel.Args(
                    nodeHandle = navKey.nodeHandle,
                    mode = mode,
                    fileName = navKey.fileName,
                    inExcludedAdapterForGetLinkAndEdit = inExcludedAdapterForGetLinkAndEdit,
                    showDownload = showDownload,
                    showShare = showShare,
                    transferHandler = transferHandler,
                    isFromSharedFolder = isFromSharedFolder(navKey.nodeSourceType),
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
}
