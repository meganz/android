package mega.privacy.android.app.textEditor

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import java.io.File
import kotlinx.coroutines.flow.distinctUntilChanged
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_HOME_PAGE
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.core.nodecomponents.mapper.ViewTypeToNodeSourceTypeMapper
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.INCOMING_SHARES_ADAPTER
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.LINKS_ADAPTER
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.RUBBISH_BIN_ADAPTER
import mega.privacy.android.core.nodecomponents.dialog.removelink.RemoveNodeLinkDialogNavKey
import mega.privacy.android.core.nodecomponents.sheet.changelabel.ChangeLabelBottomSheet
import mega.privacy.android.core.nodecomponents.sheet.changelabel.ChangeLabelBottomSheetMultiple
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.texteditor.textEditorModeFromValue
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.feature.texteditor.presentation.TextEditorComposeViewModel
import mega.privacy.android.feature.texteditor.presentation.TextEditorScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.navigation.destination.LegacyTextEditorNavKey
import nz.mega.sdk.MegaApiJava

/**
 * Returns true when the node options bottom sheet result indicates the editor should close
 * (e.g. user navigated away or triggered a transfer where the current node may no longer be valid).
 * Preview / Open-with downloads ([TransferTriggerEvent.StartDownloadForPreview]) do not close the editor.
 */
internal fun shouldCloseTextEditorOnNodeOptionsResult(
    result: NodeOptionsBottomSheetResult?,
): Boolean = when (result) {
    is NodeOptionsBottomSheetResult.Transfer ->
        result.event !is TransferTriggerEvent.StartDownloadForPreview

    is NodeOptionsBottomSheetResult.Navigation ->
        result.navKey !is ChangeLabelBottomSheet &&
                result.navKey !is ChangeLabelBottomSheetMultiple &&
                result.navKey !is RemoveNodeLinkDialogNavKey

    else -> false
}

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

/**
 * True when Share should be shown for this source type.
 * Matches [TextEditorActivity.refreshMenuOptionsVisibility]: rubbish bin hides Share (only Remove + line numbers);
 * also hidden for folder link, versions, incoming shares, and chat.
 */
internal fun shouldShowShare(nodeSourceType: Int?): Boolean {
    if (nodeSourceType == null) return true
    return nodeSourceType !in setOf(
        RUBBISH_BIN_ADAPTER,
        FOLDER_LINK_ADAPTER,
        VERSIONS_ADAPTER,
        INCOMING_SHARES_ADAPTER,
        FROM_CHAT,
    )
}

/** True when Send to chat should be shown (cloud opens only; hidden for rubbish, versions, links, chat, offline). */
internal fun shouldShowSendToChat(nodeSourceType: Int?): Boolean {
    if (nodeSourceType == null) return true
    return nodeSourceType !in setOf(
        RUBBISH_BIN_ADAPTER,
        VERSIONS_ADAPTER,
        FOLDER_LINK_ADAPTER,
        FILE_LINK_ADAPTER,
        FROM_CHAT,
        OFFLINE_ADAPTER,
    )
}

/**
 * Builds the legacy [Intent] for [TextEditorActivity] from [navKey].
 * Used when [ApiFeatures.TextEditorCompose] is disabled.
 */
private fun buildTextEditorIntent(context: Context, navKey: LegacyTextEditorNavKey): Intent {
    return when {
        navKey.chatId != null && navKey.messageId != null ->
            Intent(context, TextEditorActivity::class.java).apply {
                putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.FROM_CHAT)
                putExtra(ChatNavKey.LEGACY_MESSAGE_ID, navKey.messageId)
                putExtra(ChatNavKey.LEGACY_CHAT_ID, navKey.chatId)
            }
        navKey.localPath != null -> {
            val nodeSourceType = navKey.nodeSourceType ?: OFFLINE_ADAPTER
            Intent(context, TextEditorActivity::class.java).apply {
                putExtra(Constants.INTENT_EXTRA_KEY_PATH, navKey.localPath)
                putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, nodeSourceType)
                putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, navKey.fileName)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
        else ->
            TextEditorActivity.createIntent(
                context = context,
                nodeHandle = navKey.nodeHandle ?: MegaApiJava.INVALID_HANDLE,
                mode = navKey.mode,
                nodeSourceType = navKey.nodeSourceType,
                fileName = navKey.fileName,
            ).apply {
                putExtra(FROM_HOME_PAGE, navKey.fromHome)
            }
    }
}

/**
 * Builds [TextEditorComposeViewModel.Args] from [navKey].
 */
private fun buildTextEditorViewModelArgs(
    navKey: LegacyTextEditorNavKey,
): TextEditorComposeViewModel.Args {
    val nodeSourceType = navKey.nodeSourceType
    return when {
        navKey.chatId != null && navKey.messageId != null ->
            TextEditorComposeViewModel.Args(
                nodeHandle = MegaApiJava.INVALID_HANDLE,
                mode = TextEditorMode.View,
                fileName = null,
                inExcludedAdapterForGetLinkAndEdit = true,
                showDownload = true,
                showShare = false,
                showSendToChat = false,
                chatId = navKey.chatId,
                messageId = navKey.messageId,
            )
        navKey.localPath != null -> {
            val sourceType = nodeSourceType ?: OFFLINE_ADAPTER
            TextEditorComposeViewModel.Args(
                nodeHandle = MegaApiJava.INVALID_HANDLE,
                mode = TextEditorMode.View,
                fileName = navKey.fileName ?: "",
                inExcludedAdapterForGetLinkAndEdit = inExcludedAdapterForGetLinkAndEdit(sourceType),
                showDownload = shouldShowDownload(sourceType),
                showShare = shouldShowShare(sourceType),
                showSendToChat = false,
                localPath = navKey.localPath,
            )
        }
        else -> {
            val nodeHandle = navKey.nodeHandle ?: MegaApiJava.INVALID_HANDLE
            val mode = textEditorModeFromValue(navKey.mode)
            TextEditorComposeViewModel.Args(
                nodeHandle = nodeHandle,
                mode = mode,
                fileName = navKey.fileName,
                inExcludedAdapterForGetLinkAndEdit = inExcludedAdapterForGetLinkAndEdit(nodeSourceType),
                showDownload = shouldShowDownload(nodeSourceType),
                showShare = shouldShowShare(nodeSourceType),
                showSendToChat = shouldShowSendToChat(nodeSourceType),
                isFromSharedFolder = isFromSharedFolder(nodeSourceType),
                fromHome = navKey.fromHome,
            )
        }
    }
}

/**
 * Legacy text editor destination. Uses [FeatureFlagGate] with [ApiFeatures.TextEditorCompose]:
 * when enabled shows [TextEditorScreen]; when disabled starts [TextEditorActivity] and pops.
 * Tapping More opens the Node Options Bottom Sheet (cloud node only). When the user deletes or
 * moves the node (Navigation or most Transfer results), the editor is closed; preview/Open-with
 * downloads ([TransferTriggerEvent.StartDownloadForPreview]) stay on the editor and forward the transfer.
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
    val legacyIntent = buildTextEditorIntent(context, navKey)

    if (navKey.chatId == null && navKey.localPath == null) {
        LaunchedEffect(Unit) {
            navigationHandler.monitorResult<NodeOptionsBottomSheetResult>(NodeOptionsBottomSheetNavKey.RESULT)
                .distinctUntilChanged()
                .collect { result ->
                    if (result is NodeOptionsBottomSheetResult.Transfer &&
                        result.event is TransferTriggerEvent.StartDownloadForPreview
                    ) {
                        transferHandler.setTransferEvent(result.event)
                        navigationHandler.clearResult(NodeOptionsBottomSheetNavKey.RESULT)
                        return@collect
                    }
                    if (shouldCloseTextEditorOnNodeOptionsResult(result)) {
                        navigationHandler.clearResult(NodeOptionsBottomSheetNavKey.RESULT)
                        removeDestination()
                    }
                }
        }
    }

    FeatureFlagGate(
        feature = ApiFeatures.TextEditorCompose,
        disabled = {
            LaunchedEffect(Unit) {
                context.startActivity(legacyIntent)
                removeDestination()
            }
        },
        enabled = {
            TextEditorComposeContent(
                navKey = navKey,
                navigationHandler = navigationHandler,
                removeDestination = removeDestination,
                transferHandler = transferHandler,
                viewTypeToNodeSourceTypeMapper = viewTypeToNodeSourceTypeMapper,
            )
        },
    )
}

@Composable
private fun TextEditorComposeContent(
    navKey: LegacyTextEditorNavKey,
    navigationHandler: NavigationHandler,
    removeDestination: () -> Unit,
    transferHandler: TransferHandler,
    viewTypeToNodeSourceTypeMapper: ViewTypeToNodeSourceTypeMapper,
) {
    val args = remember(navKey) {
        buildTextEditorViewModelArgs(navKey)
    }
    val showNodeOptions = navKey.chatId == null && navKey.localPath == null
        && textEditorModeFromValue(navKey.mode) != TextEditorMode.Create
    val onOpenNodeOptions: (() -> Unit)? = if (showNodeOptions) {
        remember(
            navKey.nodeHandle,
            navKey.nodeSourceType,
            navigationHandler,
            viewTypeToNodeSourceTypeMapper,
        ) {
            val nodeHandle = navKey.nodeHandle ?: MegaApiJava.INVALID_HANDLE
            val sourceType = navKey.nodeSourceType
            {
                navigationHandler.navigate(
                    NodeOptionsBottomSheetNavKey(
                        nodeHandle = nodeHandle,
                        nodeSourceType = viewTypeToNodeSourceTypeMapper(sourceType),
                    )
                )
            }
        }
    } else {
        null
    }

    val context = LocalContext.current
    val onShare: (String?, String?) -> Unit = remember {
        { localPath, fileName ->
            if (!localPath.isNullOrBlank()) {
                val file = File(localPath)
                if (file.exists()) {
                    val name = fileName?.ifBlank { null } ?: file.name
                    FileUtil.shareUri(context, name, FileUtil.getUriForFile(context, file))
                }
            }
        }
    }

    val viewModel =
        hiltViewModel<TextEditorComposeViewModel, TextEditorComposeViewModel.Factory> { factory ->
            factory.create(args)
        }
    TextEditorScreen(
        viewModel = viewModel,
        onBack = removeDestination,
        onOpenNodeOptions = onOpenNodeOptions,
        onTransfer = { transferHandler.setTransferEvent(it) },
        onShare = onShare,
    )
}
