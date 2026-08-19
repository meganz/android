package mega.privacy.android.feature.chat.list.view

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_ARCHIVE_TAG
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_CLEAR_HISTORY_TAG
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_LEAVE_TAG
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_MUTE_TAG
import mega.privacy.android.feature.chat.list.model.ChatRoomActionUiItem
import mega.privacy.android.feature.chat.list.model.ChatRoomMenuItemConfirmation
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Modal bottom sheet listing the per-chat [actions].
 *
 * The sheet hides itself before any callback runs. The hosting screen decides, from the picked
 * action's [ChatRoomActionUiItem.confirmation], whether to run it immediately or confirm first.
 *
 * @param actions Ordered actions to show.
 * @param onDismiss Called when the sheet is dismissed without picking an action.
 * @param onActionClick Called with the picked action after the sheet has hidden.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatRoomActionsBottomSheet(
    actions: List<ChatRoomActionUiItem>,
    onDismiss: () -> Unit,
    onActionClick: (ChatRoomActionUiItem) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val close = { action: ChatRoomActionUiItem ->
        coroutineScope
            .launch { sheetState.hide() }
            .invokeOnCompletion {
                onDismiss()
                onActionClick(action)
            }
        Unit
    }

    MegaModalBottomSheet(
        modifier = Modifier.testTag(CHAT_ROOM_ACTIONS_SHEET_TAG),
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        sheetState = sheetState,
        onDismissRequest = onDismiss,
    ) {
        ChatRoomActionsSheetContent(
            actions = actions,
            onActionClick = close,
        )
    }
}

/**
 * Stateless list of action rows rendered inside the modal bottom sheet.
 *
 * @param actions Ordered actions to show.
 * @param onActionClick Called with the picked action.
 * @param modifier [Modifier]
 */
@Composable
internal fun ChatRoomActionsSheetContent(
    actions: List<ChatRoomActionUiItem>,
    onActionClick: (ChatRoomActionUiItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        actions.forEach { action ->
            ChatRoomActionItem(
                textRes = action.label,
                icon = action.icon,
                testTag = action.testTag,
                isDestructive = action.isDestructive,
                onClick = { onActionClick(action) },
            )
        }
    }
}

@Composable
private fun ChatRoomActionItem(
    @StringRes textRes: Int,
    icon: ImageVector,
    testTag: String,
    isDestructive: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaIcon(
            painter = rememberVectorPainter(icon),
            tint = if (isDestructive) IconColor.Brand else IconColor.Secondary,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        MegaText(
            text = stringResource(textRes),
            textColor = if (isDestructive) TextColor.Brand else TextColor.Primary,
            style = AppTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 32.dp),
        )
    }
}

/**
 * Confirmation dialog shown before a destructive chat room action runs.
 *
 * @param confirmation Prompt to render.
 * @param onConfirm Called when the user confirms the action.
 * @param onDismiss Called when the dialog is dismissed.
 */
@Composable
internal fun ChatRoomActionConfirmationDialog(
    confirmation: ChatRoomMenuItemConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicDialog(
        modifier = Modifier.testTag(confirmation.testTag),
        title = stringResource(confirmation.title),
        description = stringResource(confirmation.message),
        positiveButtonText = stringResource(confirmation.confirmButtonText),
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onPositiveButtonClicked = onConfirm,
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

internal const val CHAT_ROOM_ACTIONS_SHEET_TAG = "chat_room_actions_sheet:sheet"

@CombinedThemePreviews
@Composable
private fun ChatRoomActionsSheetContentPreview() {
    AndroidThemeForPreviews {
        ChatRoomActionsSheetContent(
            actions = listOf(
                ChatRoomActionUiItem(
                    label = sharedR.string.title_properties_chat_clear,
                    icon = IconPack.Medium.Thin.Outline.Eraser,
                    testTag = CHAT_ROOM_ACTIONS_CLEAR_HISTORY_TAG,
                    isDestructive = false,
                    confirmation = null,
                ),
                ChatRoomActionUiItem(
                    label = sharedR.string.general_mute,
                    icon = IconPack.Medium.Thin.Outline.BellOff,
                    testTag = CHAT_ROOM_ACTIONS_MUTE_TAG,
                    isDestructive = false,
                    confirmation = null,
                ),
                ChatRoomActionUiItem(
                    label = sharedR.string.general_archive,
                    icon = IconPack.Medium.Thin.Outline.Archive,
                    testTag = CHAT_ROOM_ACTIONS_ARCHIVE_TAG,
                    isDestructive = false,
                    confirmation = null,
                ),
                ChatRoomActionUiItem(
                    label = sharedR.string.general_leave,
                    icon = IconPack.Medium.Thin.Outline.LogOut02,
                    testTag = CHAT_ROOM_ACTIONS_LEAVE_TAG,
                    isDestructive = true,
                    confirmation = null,
                ),
            ),
            onActionClick = {},
        )
    }
}
