package mega.privacy.android.feature.chat.list.view

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Screenshot tests that render the chat-list last-message preview strings in a real
 * chat row, providing Weblate translators with the surrounding UI context.
 */
class ChatLastMessagePreviewScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatRoomItemNoHistory() {
        Row(
            title = "Mieko Kawakami",
            lastMessage = stringResource(sharedR.string.chat_last_message_no_history),
            avatar = ChatRoomUiItem.ChatRoomUiAvatar.Peer(
                placeholderText = "M",
                filePath = null,
                color = 0xFFFEBC00.toInt(),
            ),
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatRoomItemSenderMe() {
        Row(
            title = "Recipe test #14",
            lastMessage = "${stringResource(sharedR.string.chat_last_message_sender_me)}: See you all soon!",
            avatar = ChatRoomUiItem.ChatRoomUiAvatar.Group,
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatRoomItemSenderUnknown() {
        Row(
            title = "Recipe test #14",
            lastMessage = "${stringResource(sharedR.string.chat_last_message_sender_unknown)}: See you all soon!",
            avatar = ChatRoomUiItem.ChatRoomUiAvatar.Group,
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatRoomItemsParticipantAndPrivilegeChanges() {
        Rows(
            stringResource(sharedR.string.non_format_message_add_participant, "Bob", "Alice"),
            stringResource(sharedR.string.non_format_message_remove_participant, "Bob", "Alice"),
            stringResource(sharedR.string.non_format_message_participant_left_group_chat, "Bob"),
            stringResource(
                sharedR.string.chat_chats_list_last_message_permissions_changed_to_host,
                "Bob", "Alice",
            ),
            stringResource(
                sharedR.string.chat_chats_list_last_message_permissions_changed_to_standard,
                "Bob", "Alice",
            ),
            stringResource(
                sharedR.string.chat_chats_list_last_message_permissions_changed_to_read_only,
                "Bob", "Alice",
            ),
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatRoomItemsTitleHistoryAndLinkChanges() {
        Rows(
            stringResource(
                sharedR.string.non_format_change_title_messages, "Alice", "Weekend plans",
            ),
            stringResource(sharedR.string.history_cleared_by, "Alice"),
            stringResource(sharedR.string.non_format_history_cleared_by, "Alice"),
            stringResource(sharedR.string.message_created_chat_link, "Alice"),
            stringResource(sharedR.string.message_deleted_chat_link, "Alice"),
            stringResource(sharedR.string.message_set_chat_private, "Alice"),
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatRoomItemsRetentionTimeChanges() {
        Rows(
            stringResource(sharedR.string.retention_history_disabled, "Alice"),
            stringResource(
                sharedR.string.retention_history_changed_by,
                "Alice",
                pluralStringResource(sharedR.plurals.subtitle_properties_manage_chat_label_hours, 1, 1),
            ),
            stringResource(
                sharedR.string.retention_history_changed_by,
                "Alice",
                pluralStringResource(sharedR.plurals.label_time_in_days_full, 1, 1),
            ),
            stringResource(
                sharedR.string.retention_history_changed_by,
                "Alice",
                pluralStringResource(sharedR.plurals.subtitle_properties_manage_chat_label_weeks, 1, 1),
            ),
            stringResource(
                sharedR.string.retention_history_changed_by,
                "Alice",
                pluralStringResource(sharedR.plurals.subtitle_properties_manage_chat_label_months, 1, 1),
            ),
            stringResource(
                sharedR.string.retention_history_changed_by,
                "Alice",
                stringResource(sharedR.string.subtitle_properties_manage_chat_label_year),
            ),
        )
    }

    @Composable
    private fun Row(
        title: String,
        lastMessage: String,
        avatar: ChatRoomUiItem.ChatRoomUiAvatar,
    ) {
        AndroidThemeForPreviews {
            ChatRoomItemView(
                item = chatRoomUiItem(title = title, lastMessage = lastMessage, avatar = avatar),
                onItemClick = {},
            )
        }
    }

    @Composable
    private fun Rows(vararg lastMessages: String) {
        AndroidThemeForPreviews {
            Column {
                lastMessages.forEach { lastMessage ->
                    ChatRoomItemView(
                        item = chatRoomUiItem(
                            title = "Recipe test #14",
                            lastMessage = lastMessage,
                            avatar = ChatRoomUiItem.ChatRoomUiAvatar.Group,
                        ),
                        onItemClick = {},
                    )
                }
            }
        }
    }

    private fun chatRoomUiItem(
        title: String,
        lastMessage: String,
        avatar: ChatRoomUiItem.ChatRoomUiAvatar,
    ) = ChatRoomUiItem(
        chatId = 1L,
        title = title,
        lastMessage = lastMessage,
        lastTimestampFormatted = "Today 14:25",
        scheduledTimestampFormatted = null,
        unreadCount = 0,
        isMuted = false,
        highlight = false,
        isNoteToSelf = false,
        avatar = avatar,
        status = ContactItemStatus.Unknown,
    )
}
