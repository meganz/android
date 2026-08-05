package mega.privacy.android.feature.chat.list.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.badge.NotificationBadge
import mega.android.core.ui.components.contact.component.ContactStatusDot
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.components.profile.MediumProfileIcon
import mega.android.core.ui.components.profile.MediumProfilePicture
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem.ChatRoomUiAvatar
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import java.io.File

/**
 * Chat list row showing avatar, title, muted indicator, last message preview,
 * timestamp and unread count badge.
 *
 * @param item Row content.
 * @param onItemClick Callback when the row is clicked, with the chat id.
 * @param modifier [Modifier]
 */
@Composable
internal fun ChatRoomItemView(
    item: ChatRoomUiItem,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    GenericListItem(
        modifier = modifier.testTag(CHAT_ROOM_ITEM_TAG),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        leadingElement = { ChatRoomAvatarView(item) },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MegaText(
                    text = if (item.isNoteToSelf) {
                        stringResource(sharedR.string.chat_note_to_self_chat_title)
                    } else {
                        item.title
                    },
                    textColor = TextColor.Primary,
                    style = AppTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(bottom = 2.dp)
                        .testTag(CHAT_ROOM_ITEM_TITLE_TAG),
                )
                if (item.isMuted) {
                    MegaIcon(
                        painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.BellOff),
                        tint = IconColor.Secondary,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(16.dp)
                            .testTag(CHAT_ROOM_ITEM_MUTE_TAG),
                    )
                }
            }
        },
        subtitle = {
            val message = item.scheduledTimestampFormatted ?: item.lastMessage
            message?.let {
                MegaText(
                    text = it,
                    textColor = if (item.highlight) TextColor.Accent else TextColor.Secondary,
                    style = AppTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(CHAT_ROOM_ITEM_MESSAGE_TAG),
                )
            }
            item.lastTimestampFormatted?.let {
                MegaText(
                    text = it,
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(CHAT_ROOM_ITEM_TIMESTAMP_TAG),
                )
            }
        },
        trailingElement = {
            if (item.unreadCount > 0) {
                NotificationBadge(
                    count = item.unreadCount,
                    modifier = Modifier.testTag(CHAT_ROOM_ITEM_UNREAD_TAG),
                )
            }
        },
        onClickListener = { onItemClick(item.chatId) },
    )
}

@Composable
private fun ChatRoomAvatarView(item: ChatRoomUiItem) {
    Box {
        when (val avatar = item.avatar) {
            is ChatRoomUiAvatar.Peer -> MediumProfilePicture(
                imageFile = avatar.filePath?.let(::File),
                name = avatar.placeholderText ?: item.title,
                contentDescription = item.title,
                avatarColor = avatar.color?.let(::Color) ?: PeerAvatarFallbackColor,
                modifier = Modifier
                    .padding(8.dp)
                    .testTag(CHAT_ROOM_ITEM_AVATAR_TAG),
            )

            ChatRoomUiAvatar.Group -> ChatRoomIconAvatarView(
                item = item,
                icon = ChatRoomIconAvatar.Group,
            )

            ChatRoomUiAvatar.Meeting -> ChatRoomIconAvatarView(
                item = item,
                icon = ChatRoomIconAvatar.Meeting,
            )

            ChatRoomUiAvatar.NoteToSelf -> ChatRoomIconAvatarView(
                item = item,
                icon = ChatRoomIconAvatar.NoteToSelf,
            )
        }
        if (item.status != ContactItemStatus.Unknown) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .testTag(CHAT_ROOM_ITEM_STATUS_TAG),
            ) {
                ContactStatusDot(status = item.status)
            }
        }
    }
}

@Composable
private fun ChatRoomIconAvatarView(
    item: ChatRoomUiItem,
    icon: ChatRoomIconAvatar,
) {
    MediumProfileIcon(
        icon = icon.imageVector,
        iconTint = IconColor.Inverse,
        contentDescription = item.title,
        avatarColor = icon.avatarColor,
        avatarSecondaryColor = icon.avatarSecondaryColor,
        modifier = Modifier
            .padding(8.dp)
            .testTag(CHAT_ROOM_ITEM_AVATAR_TAG),
    )
}

private enum class ChatRoomIconAvatar(
    val avatarColor: Color,
    val avatarSecondaryColor: Color,
) {
    Group(Color(0xFF00ACC1), Color(0xFF00BDB2)),
    Meeting(Color(0xFF00897B), Color(0xFF00ACC1)),
    NoteToSelf(Color(0xFF00ACC1), Color(0xFF00BDB2)),
    ;

    val imageVector
        get() = when (this) {
            Group -> IconPack.Medium.Thin.Solid.MessageChatCircle
            Meeting -> IconPack.Medium.Thin.Solid.Video
            NoteToSelf -> IconPack.Medium.Thin.Outline.FileText
        }
}

private val PeerAvatarFallbackColor = Color(0xFF00ACC1)

internal const val CHAT_ROOM_ITEM_TAG = "chat_room_item:row"
internal const val CHAT_ROOM_ITEM_AVATAR_TAG = "chat_room_item:avatar"
internal const val CHAT_ROOM_ITEM_STATUS_TAG = "chat_room_item:status_dot"
internal const val CHAT_ROOM_ITEM_TITLE_TAG = "chat_room_item:title"
internal const val CHAT_ROOM_ITEM_MUTE_TAG = "chat_room_item:mute_icon"
internal const val CHAT_ROOM_ITEM_MESSAGE_TAG = "chat_room_item:last_message"
internal const val CHAT_ROOM_ITEM_TIMESTAMP_TAG = "chat_room_item:timestamp"
internal const val CHAT_ROOM_ITEM_UNREAD_TAG = "chat_room_item:unread_badge"

@CombinedThemePreviews
@Composable
private fun ChatRoomItemViewPreview(
    @PreviewParameter(ChatRoomUiItemPreviewProvider::class) item: ChatRoomUiItem,
) {
    AndroidThemeForPreviews {
        ChatRoomItemView(
            item = item,
            onItemClick = {},
        )
    }
}

private class ChatRoomUiItemPreviewProvider : PreviewParameterProvider<ChatRoomUiItem> {
    override val values = sequenceOf(
        ChatRoomUiItem(
            chatId = 1L,
            title = "Mieko Kawakami",
            lastMessage = "See you tomorrow!",
            lastTimestampFormatted = "Today 14:25",
            scheduledTimestampFormatted = null,
            unreadCount = 5,
            isMuted = false,
            highlight = true,
            isNoteToSelf = false,
            avatar = ChatRoomUiItem.ChatRoomUiAvatar.Peer(
                placeholderText = "M",
                filePath = null,
                color = 0xFFFEBC00.toInt(),
            ),
            status = ContactItemStatus.Online,
        ),
        ChatRoomUiItem(
            chatId = 2L,
            title = "Recipe test #14",
            lastMessage = "Anna: Seeya all soon!",
            lastTimestampFormatted = "1 May 2022 17:53",
            scheduledTimestampFormatted = null,
            unreadCount = 150,
            isMuted = true,
            highlight = false,
            isNoteToSelf = false,
            avatar = ChatRoomUiItem.ChatRoomUiAvatar.Group,
            status = ContactItemStatus.Unknown,
        ),
        ChatRoomUiItem(
            chatId = 3L,
            title = "Weekly sync",
            lastMessage = null,
            lastTimestampFormatted = "1 May 2022 17:53",
            scheduledTimestampFormatted = "10:00am - 11:00am",
            unreadCount = 0,
            isMuted = false,
            highlight = false,
            isNoteToSelf = false,
            avatar = ChatRoomUiItem.ChatRoomUiAvatar.Meeting,
            status = ContactItemStatus.Unknown,
        ),
        ChatRoomUiItem(
            chatId = 4L,
            title = "Note to self",
            lastMessage = "Remember the keys",
            lastTimestampFormatted = "Monday 14:25",
            scheduledTimestampFormatted = null,
            unreadCount = 0,
            isMuted = false,
            highlight = false,
            isNoteToSelf = true,
            avatar = ChatRoomUiItem.ChatRoomUiAvatar.NoteToSelf,
            status = ContactItemStatus.Unknown,
        ),
    )
}
