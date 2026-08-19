package mega.privacy.android.feature.chat.list.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_ARCHIVE_TAG
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_CLEAR_DIALOG_TAG
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_CLEAR_HISTORY_TAG
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_LEAVE_DIALOG_TAG
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_LEAVE_TAG
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_MUTE_TAG
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_UNARCHIVE_TAG
import mega.privacy.android.feature.chat.list.model.ChatRoomActionUiItem
import mega.privacy.android.feature.chat.list.model.ChatRoomMenuItemConfirmation
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Screenshot tests for the chat list item actions bottom sheet content and its
 * destructive-action confirmation dialog, providing Weblate translators with the
 * surrounding UI context for the action-label and confirmation strings.
 */
class ChatRoomActionsBottomSheetScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun GroupChatActions() {
        SheetContent(listOf(clearHistory(), mute(), archive(), leave()))
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MutedChatActions() {
        SheetContent(listOf(clearHistory(), unmute(), archive()))
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ArchivedChatActions() {
        SheetContent(listOf(unarchive()))
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun LeaveConfirmation() {
        AndroidThemeForPreviews {
            ChatRoomActionConfirmationDialog(
                confirmation = leaveConfirmation,
                onConfirm = {},
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ClearHistoryConfirmation() {
        AndroidThemeForPreviews {
            ChatRoomActionConfirmationDialog(
                confirmation = clearHistoryConfirmation,
                onConfirm = {},
                onDismiss = {},
            )
        }
    }

    @Composable
    private fun SheetContent(actions: List<ChatRoomActionUiItem>) {
        AndroidThemeForPreviews {
            ChatRoomActionsSheetContent(
                actions = actions,
                onActionClick = {},
            )
        }
    }

    private fun clearHistory() = ChatRoomActionUiItem(
        label = sharedR.string.title_properties_chat_clear,
        icon = IconPack.Medium.Thin.Outline.Eraser,
        testTag = CHAT_ROOM_ACTIONS_CLEAR_HISTORY_TAG,
        isDestructive = false,
        confirmation = clearHistoryConfirmation,
    )

    private fun mute() = ChatRoomActionUiItem(
        label = sharedR.string.general_mute,
        icon = IconPack.Medium.Thin.Outline.BellOff,
        testTag = CHAT_ROOM_ACTIONS_MUTE_TAG,
        isDestructive = false,
        confirmation = null,
    )

    private fun unmute() = ChatRoomActionUiItem(
        label = sharedR.string.general_unmute,
        icon = IconPack.Medium.Thin.Outline.Bell,
        testTag = CHAT_ROOM_ACTIONS_MUTE_TAG,
        isDestructive = false,
        confirmation = null,
    )

    private fun archive() = ChatRoomActionUiItem(
        label = sharedR.string.general_archive,
        icon = IconPack.Medium.Thin.Outline.Archive,
        testTag = CHAT_ROOM_ACTIONS_ARCHIVE_TAG,
        isDestructive = false,
        confirmation = null,
    )

    private fun unarchive() = ChatRoomActionUiItem(
        label = sharedR.string.general_unarchive,
        icon = IconPack.Medium.Thin.Outline.ArchiveArrowUp,
        testTag = CHAT_ROOM_ACTIONS_UNARCHIVE_TAG,
        isDestructive = false,
        confirmation = null,
    )

    private fun leave() = ChatRoomActionUiItem(
        label = sharedR.string.general_leave,
        icon = IconPack.Medium.Thin.Outline.LogOut02,
        testTag = CHAT_ROOM_ACTIONS_LEAVE_TAG,
        isDestructive = true,
        confirmation = leaveConfirmation,
    )

    private val leaveConfirmation = ChatRoomMenuItemConfirmation(
        title = sharedR.string.title_confirmation_leave_group_chat,
        message = sharedR.string.confirmation_leave_group_chat,
        confirmButtonText = sharedR.string.general_leave,
        testTag = CHAT_ROOM_ACTIONS_LEAVE_DIALOG_TAG,
    )

    private val clearHistoryConfirmation = ChatRoomMenuItemConfirmation(
        title = sharedR.string.title_properties_chat_clear,
        message = sharedR.string.confirmation_clear_chat_history,
        confirmButtonText = sharedR.string.general_clear,
        testTag = CHAT_ROOM_ACTIONS_CLEAR_DIALOG_TAG,
    )
}
