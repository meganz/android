package mega.privacy.android.feature.texteditor.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.triggered
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
import mega.privacy.android.icon.pack.R as IconPackR
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
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TextEditorLoadingContent()
                    }
                }

                uiState.errorEvent == triggered -> {
                    TextEditorErrorContent(
                        message = stringResource(sharedR.string.general_request_failed_message),
                        onDismiss = viewModel::consumeErrorEvent,
                    )
                }

                else -> {
                    TextEditorContent(
                        content = uiState.content,
                        showLineNumbers = uiState.showLineNumbers,
                        readOnly = uiState.mode == TextEditorMode.View,
                        onContentChange = viewModel::updateContent,
                    )
                }
            }
        }
    }
}

/** Gutter width when line numbers are shown (mirrors legacy text_editor_padding_start_with_nLines). */
private val LineNumberGutterWidth = 36.dp

/** Line number text size (mirrors legacy line_number_size). */
private val LineNumberTextSize = 10.sp

/** Horizontal padding for line number column (mirrors legacy line_number_padding). */
private val LineNumberPadding = 6.dp

@Composable
private fun TextEditorContent(
    content: String,
    showLineNumbers: Boolean,
    readOnly: Boolean,
    onContentChange: (String) -> Unit,
) {
    val lineCount = content.count { it == '\n' } + 1
    val lineNumbers = remember(lineCount) {
        (1..lineCount.coerceAtLeast(1)).joinToString("\n") { "$it" }
    }
    val textStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 14.sp,
    )
    val verticalScrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .verticalScroll(verticalScrollState),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            ) {
                if (showLineNumbers) {
                    BasicTextField(
                        value = lineNumbers,
                        onValueChange = {},
                        readOnly = true,
                        textStyle = textStyle.copy(fontSize = LineNumberTextSize),
                        modifier = Modifier
                            .width(LineNumberGutterWidth)
                            .wrapContentHeight()
                            .padding(end = LineNumberPadding),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                inner()
                            }
                        },
                    )
                }
                BasicTextField(
                    value = content,
                    onValueChange = onContentChange,
                    readOnly = readOnly,
                    textStyle = textStyle,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .wrapContentHeight(),
                )
            }
        }
    }
}

@Composable
private fun TextEditorErrorContent(
    message: String,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MegaText(text = message, modifier = Modifier.padding(16.dp))
            Button(onClick = onDismiss) {
                Text(text = stringResource(sharedR.string.general_ok))
            }
        }
    }
}

/**
 * Loading view matching legacy text editor: text file icon (96dp) and horizontal indeterminate progress bar below.
 * Legacy: loading_layout has layout_constraintTop_toTopOf="parent" and layout_marginTop="153dp", so content is
 * top-anchored 153dp from the top of the screen (not vertically centered).
 */
@Composable
private fun TextEditorLoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 153.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier.size(96.dp),
            painter = painterResource(IconPackR.drawable.ic_text_medium_solid),
            contentDescription = stringResource(sharedR.string.transfers_fake_preview_text),
        )
        Spacer(modifier = Modifier.height(30.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .widthIn(min = 100.dp)
                .padding(horizontal = 44.dp),
            color = MaterialTheme.colorScheme.onSurface,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        )
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
    // TODO: Replace Menu01 with dedicated line-numbers icon when UI/UX provides it (IconPack). Track in backlog.
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
