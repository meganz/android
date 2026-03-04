package mega.privacy.android.feature.texteditor.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorConditionalTopBarAction
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarAction
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarSlot
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarSlots
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Compose screen for viewing and editing text files.
 * Top bar: Back + Download, Line numbers, Get link, Send to chat, Share, More (opens Node Options Bottom Sheet).
 * Slots and order depend on node source and mode; node actions are also available via the bottom sheet when tapping More.
 */
@Composable
fun TextEditorScreen(
    viewModel: TextEditorComposeViewModel,
    onBack: () -> Unit,
    onOpenNodeOptions: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler {
        when {
            uiState.mode != TextEditorMode.View && uiState.isFileEdited -> onBack()
            uiState.mode == TextEditorMode.Edit -> viewModel.setViewMode()
            uiState.mode == TextEditorMode.Create -> {
                viewModel.saveFile(fromHome = false)
                onBack()
            }

            else -> onBack()
        }
    }

    MegaScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TextEditorTopAppBar(
                showLineNumbers = uiState.showLineNumbers,
                topBarSlots = uiState.topBarSlots,
                onBack = onBack,
                onMenuAction = viewModel::onMenuAction,
                onOpenNodeOptions = onOpenNodeOptions,
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            if (!uiState.isLoading) {
                MegaText(text = "Text editor (Compose)\n${uiState.fileName}")
            }
        }
    }
}

@Composable
private fun TextEditorTopAppBar(
    showLineNumbers: Boolean,
    topBarSlots: TextEditorTopBarSlots,
    onBack: () -> Unit,
    onMenuAction: (TextEditorTopBarAction) -> Unit,
    onOpenNodeOptions: () -> Unit,
) {
    MegaTopAppBar(
        title = "",
        navigationType = AppBarNavigationType.Back(onBack),
        trailingIcons = {
            Row {
                topBarSlots.forEach { slot ->
                    when (slot) {
                        TextEditorTopBarSlot.LineNumbers -> LineNumbersButton(
                            showLineNumbers,
                            onMenuAction,
                        )
                        TextEditorTopBarSlot.More -> MoreButton(onOpenNodeOptions)
                        is TextEditorTopBarSlot.Conditional -> ConditionalActionButton(
                            slot.action,
                            onMenuAction,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun LineNumbersButton(
    showLineNumbers: Boolean,
    onMenuAction: (TextEditorTopBarAction) -> Unit,
) {
    // TODO: Replace Menu01 with dedicated line-numbers icon when UI/UX provides it (IconPack).
    // Legacy uses action_show_line_numbers / action_hide_line_numbers (app); we use shared strings here because feature does not depend on app.
    IconButton(onClick = { onMenuAction(TextEditorTopBarAction.LineNumbers) }) {
        MegaIcon(
            imageVector = IconPack.Medium.Thin.Outline.Menu01,
            tint = IconColor.Primary,
            contentDescription = stringResource(
                if (showLineNumbers) sharedR.string.general_hide_node
                else sharedR.string.general_unhide_node
            ),
        )
    }
}

@Composable
private fun MoreButton(onOpenNodeOptions: () -> Unit) {
    IconButton(onClick = onOpenNodeOptions) {
        MegaIcon(
            imageVector = IconPack.Medium.Thin.Outline.MoreVertical,
            tint = IconColor.Primary,
            contentDescription = stringResource(sharedR.string.album_content_selection_action_more_description),
        )
    }
}

@Composable
private fun ConditionalActionButton(
    action: TextEditorConditionalTopBarAction,
    onMenuAction: (TextEditorTopBarAction) -> Unit,
) {
    when (action) {
        TextEditorConditionalTopBarAction.Download -> IconButton(onClick = {
            onMenuAction(
                TextEditorTopBarAction.Download
            )
        }) {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.Download,
                tint = IconColor.Primary,
                contentDescription = stringResource(sharedR.string.general_save_to_device),
            )
        }

        TextEditorConditionalTopBarAction.GetLink -> IconButton(onClick = {
            onMenuAction(
                TextEditorTopBarAction.GetLink
            )
        }) {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.Link01,
                tint = IconColor.Primary,
                contentDescription = stringResource(sharedR.string.meetings_share_link_bottom_sheet_button_share_link),
            )
        }

        TextEditorConditionalTopBarAction.SendToChat -> IconButton(onClick = {
            onMenuAction(TextEditorTopBarAction.SendToChat)
        }) {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.MessageArrowUp,
                tint = IconColor.Primary,
                contentDescription = stringResource(sharedR.string.meetings_share_link_bottom_sheet_button_send_link_chat),
            )
        }

        TextEditorConditionalTopBarAction.Share -> IconButton(onClick = {
            onMenuAction(
                TextEditorTopBarAction.Share
            )
        }) {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.ShareNetwork,
                tint = IconColor.Primary,
                contentDescription = stringResource(sharedR.string.general_share),
            )
        }
    }
}
