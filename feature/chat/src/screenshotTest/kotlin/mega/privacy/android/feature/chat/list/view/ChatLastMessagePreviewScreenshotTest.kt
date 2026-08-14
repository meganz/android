package mega.privacy.android.feature.chat.list.view

import androidx.compose.runtime.Composable
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

    @Composable
    private fun Row(
        title: String,
        lastMessage: String,
        avatar: ChatRoomUiItem.ChatRoomUiAvatar,
    ) {
        AndroidThemeForPreviews {
            ChatRoomItemView(
                item = ChatRoomUiItem(
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
                ),
                onItemClick = {},
            )
        }
    }
}
