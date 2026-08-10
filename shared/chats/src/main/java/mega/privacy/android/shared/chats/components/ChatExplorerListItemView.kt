package mega.privacy.android.shared.chats.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.components.profile.MediumProfileIcon
import mega.android.core.ui.components.profile.MediumProfilePicture
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.shared.chats.model.ChatExplorerUiItem
import mega.privacy.android.shared.resources.R as shareR
import java.io.File

/**
 * Composable for the chat explorer list item
 *
 * @param item The ChatExplorerUiItem to display
 * @param onItemClicked Callback when the item is clicked
 */
@Composable
fun ChatExplorerListItemView(
    item: ChatExplorerUiItem,
    isProcessingAction: Boolean,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is ChatExplorerUiItem.NoteToSelf ->
            ChatExplorerListItemView(
                modifier = modifier,
                isSelected = item.isSelected,
                isEnabled = item.isEnabled,
                isProcessingAction = isProcessingAction,
                isHint = item.isHint,
                hasAvatarIcon = item.hasAvatarIcon,
                icon = item.icon,
                title = stringResource(id = shareR.string.chat_note_to_self_chat_title),
                avatarColor = item.avatarPrimaryColor,
                avatarSecondaryColor = item.avatarSecondaryColor,
                onItemClicked = onItemClicked,
            )

        is ChatExplorerUiItem.GroupChatAndMeeting ->
            ChatExplorerListItemView(
                modifier = modifier,
                isSelected = item.isSelected,
                isEnabled = item.isEnabled,
                isProcessingAction = isProcessingAction,
                hasAvatarIcon = item.hasAvatarIcon,
                icon = item.icon,
                title = item.title,
                subtitle = item.participants?.let { count ->
                    pluralStringResource(
                        id = shareR.plurals.general_number_participants,
                        count = count,
                        count,
                    )
                },
                avatarColor = item.avatarPrimaryColor,
                avatarSecondaryColor = item.avatarSecondaryColor,
                onItemClicked = onItemClicked,
            )

        is ChatExplorerUiItem.OneToOneChatAndContact -> ChatExplorerListItemView(
            modifier = modifier,
            isSelected = item.isSelected,
            isEnabled = item.isEnabled,
            isProcessingAction = isProcessingAction,
            title = item.contactName,
            subtitle = item.userStatus?.toSubtitleStringRes()?.let { stringResource(it) },
            avatarColor = item.avatarPrimaryColor,
            avatarSecondaryColor = item.avatarSecondaryColor,
            contactAvatarFile = item.contactAvatarFile,
            onItemClicked = onItemClicked,
        )
    }
}

/**
 * Composable for chat list item
 *
 * @param isSelected Whether the item is currently selected.
 * @param isEnabled Whether item is enabled. When false, the row is dimmed, the
 * trailing checkbox is hidden, and clicks route to [onDisabledItemClicked].
 * @param title The title of the item.
 * @param subtitle The subtitle of the item.
 * @param isHint When true, shows a plain icon (e.g. note-to-self hint) instead of an avatar slot.
 * @param hasAvatarIcon When true (and not [isHint]), shows [MediumProfileIcon] for group, meeting,
 * or note-to-self with icon; otherwise [MediumProfilePicture] when [avatarColor] is set.
 * @param contactAvatarFile The file representing the contact's avatar.
 * @param icon The image vector for hint, group chat, meeting, or note-to-self icon row.
 * @param avatarColor The background primary color for the avatar.
 * @param avatarSecondaryColor The background secondary color for the avatar.
 * @param onItemClicked Callback when the row is clicked.
 */
@Composable
internal fun ChatExplorerListItemView(
    isSelected: Boolean,
    isEnabled: Boolean,
    isProcessingAction: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    isHint: Boolean = false,
    hasAvatarIcon: Boolean = false,
    contactAvatarFile: File? = null,
    icon: ImageVector? = null,
    avatarColor: Color? = null,
    avatarSecondaryColor: Color? = null,
    onItemClicked: () -> Unit,
) {
    val contentAlpha = if (isEnabled) 1f else DISABLED_CONTENT_ALPHA
    GenericListItem(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        enableClick = true,
        leadingElement = {
            when {
                isHint -> {
                    icon?.let {
                        MegaIcon(
                            modifier = Modifier
                                .alpha(contentAlpha)
                                .size(24.dp)
                                .testTag(NOTE_TO_SELF_HINT_ICON_TAG),
                            painter = rememberVectorPainter(it),
                            tint = IconColor.Primary,
                            contentDescription = title,
                        )
                    }
                }

                hasAvatarIcon ->
                    if (icon != null && avatarColor != null) {
                        MediumProfileIcon(
                            modifier = Modifier
                                .alpha(contentAlpha)
                                .padding(8.dp)
                                .testTag(MEETING_ICON_TAG),
                            icon = icon,
                            iconTint = IconColor.Inverse,
                            contentDescription = title,
                            avatarColor = avatarColor,
                            avatarSecondaryColor = avatarSecondaryColor
                        )
                    }

                else ->
                    avatarColor?.let {
                        MediumProfilePicture(
                            modifier = Modifier
                                .alpha(contentAlpha)
                                .padding(8.dp)
                                .testTag(CONTACT_AVATAR_TAG),
                            imageFile = contactAvatarFile,
                            contentDescription = title,
                            name = title,
                            avatarColor = it,
                            avatarSecondaryColor = avatarSecondaryColor,
                        )
                    }
            }
        },
        title = {
            title?.let {
                MegaText(
                    modifier = Modifier
                        .alpha(contentAlpha)
                        .testTag(TITLE_TAG)
                        .padding(bottom = 2.dp),
                    text = it,
                    overflow = TextOverflow.MiddleEllipsis,
                    maxLines = 1,
                    textColor = TextColor.Primary,
                    style = AppTheme.typography.bodyLarge,
                )
            }
        },
        subtitle = {
            subtitle?.let {
                MegaText(
                    text = it,
                    textColor = TextColor.Secondary,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTheme.typography.bodySmall,
                    maxLines = 1,
                    modifier = Modifier
                        .alpha(contentAlpha)
                        .testTag(SUBTITLE_TAG),
                )
            }
        },
        trailingElement = {
            Box(
                modifier = Modifier
                    .alpha(contentAlpha)
                    .size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isSelected,
                    enabled = if (isSelected) !isProcessingAction else true,
                    onCheckStateChanged = { },
                    tapTargetArea = false,
                    clickable = false,
                    modifier = Modifier.testTag(CHECKBOX_TAG)
                )
            }
        },
        onClickListener = onItemClicked,
    )
}

private const val DISABLED_CONTENT_ALPHA = 0.3f

private fun ChatStatus.toSubtitleStringRes(): Int? = when (this) {
    ChatStatus.Online -> shareR.string.online_status
    ChatStatus.Away -> shareR.string.away_status
    ChatStatus.Busy -> shareR.string.busy_status
    ChatStatus.Offline,
    ChatStatus.NoNetworkConnection,
    ChatStatus.Reconnecting,
    ChatStatus.Connecting,
        -> null
}

internal const val NOTE_TO_SELF_HINT_ICON_TAG = "chat_explorer_list_item:note_to_self_hint_icon"

/** Leading [MediumProfileIcon] for group, meeting, or note-to-self (non-hint). */
internal const val MEETING_ICON_TAG = "chat_explorer_list_item:meeting_icon"

internal const val CONTACT_AVATAR_TAG = "chat_explorer_list_item:contact_avatar"
internal const val TITLE_TAG = "chat_explorer_list_item:title"
internal const val SUBTITLE_TAG = "chat_explorer_list_item:subtitle"
internal const val CHECKBOX_TAG = "chat_explorer_list_item:checkbox"

private val previewAvatarPrimary = Color(0xFF6200EE)
private val previewAvatarSecondary = Color(0xFF00D5E2)

@CombinedThemePreviews
@Composable
private fun ChatExplorerListItemViewPreview(
    @PreviewParameter(ChatExplorerListItemPreviewParameterProvider::class) item: ChatExplorerUiItem,
) {
    AndroidThemeForPreviews {
        ChatExplorerListItemView(
            item = item,
            isProcessingAction = true,
            onItemClicked = {},
        )
    }
}

private class ChatExplorerListItemPreviewParameterProvider :
    PreviewParameterProvider<ChatExplorerUiItem> {
    override val values: Sequence<ChatExplorerUiItem>
        get() = sequenceOf(
            ChatExplorerUiItem.NoteToSelf(
                id = 1L,
                isHint = true,
                isSelected = false,
                isEnabled = true,
                isArchived = false,
                lastTimestamp = 0L,
            ),
            ChatExplorerUiItem.NoteToSelf(
                id = 2L,
                isHint = true,
                isSelected = true,
                isEnabled = true,
                isArchived = false,
                lastTimestamp = 0L,
            ),
            ChatExplorerUiItem.NoteToSelf(
                id = 3L,
                isHint = false,
                isSelected = false,
                isEnabled = true,
                isArchived = false,
                lastTimestamp = 0L,
            ),
            ChatExplorerUiItem.GroupChat(
                id = 4L,
                title = "Team standup",
                participants = 12,
                isSelected = false,
                isEnabled = true,
                isArchived = false,
                lastTimestamp = 0L,
            ),
            ChatExplorerUiItem.GroupChat(
                id = 5L,
                title = "Team standup",
                participants = 3,
                isSelected = true,
                isEnabled = false,
                isArchived = false,
                lastTimestamp = 0L,
            ),
            ChatExplorerUiItem.Meeting(
                id = 6L,
                title = "Weekly sync",
                participants = 5,
                isSelected = true,
                isEnabled = true,
                isArchived = false,
                lastTimestamp = 0L,
            ),
            ChatExplorerUiItem.OneToOneChat(
                id = 7L,
                userStatus = ChatStatus.Online,
                contactName = "Alice",
                contactAvatarFile = null,
                primaryColor = previewAvatarPrimary,
                secondaryColor = previewAvatarSecondary,
                isSelected = false,
                isEnabled = true,
                isArchived = false,
                lastTimestamp = 0L,
            ),
            ChatExplorerUiItem.Contact(
                id = 8L,
                userStatus = ChatStatus.Offline,
                contactName = "Bob",
                contactAvatarFile = null,
                primaryColor = previewAvatarPrimary,
                secondaryColor = previewAvatarSecondary,
                isSelected = true,
                isEnabled = true,
            ),
        )
}
