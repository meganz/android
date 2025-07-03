package mega.privacy.android.app.presentation.chat.dialog.view

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.list.view.ChatAvatarView
import mega.privacy.android.app.presentation.chat.list.view.ChatDivider
import mega.privacy.android.app.presentation.chat.list.view.ChatUserStatusView
import mega.privacy.android.app.presentation.meeting.chat.view.NoteToSelfAvatarView
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.GroupChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.IndividualChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.MeetingChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.NoteToSelfChatRoomItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.meeting.ChatRoomItemStatus
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_054_white_alpha_054
import mega.privacy.android.shared.original.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.shared.resources.R as sharedR
import kotlin.random.Random

/**
 * Chat Room Item bottom sheet view
 */
@Composable
internal fun ChatRoomItemBottomSheetView(
    item: ChatRoomItem?,
    modifier: Modifier = Modifier,
    onStartMeetingClick: () -> Unit = {},
    onOccurrencesClick: () -> Unit = {},
    onInfoClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onClearChatClick: () -> Unit = {},
    onMuteClick: () -> Unit = {},
    onUnmuteClick: () -> Unit = {},
    onArchiveClick: (Boolean) -> Unit = {},
    onUnarchiveClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onLeaveClick: () -> Unit = {},
) {
    if (item == null) {
        Text(
            text = stringResource(id = R.string.chat_error_open_title),
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp)
        )
        return
    }

    val isNoteToSelf = item is NoteToSelfChatRoomItem
    val isGroup = item is GroupChatRoomItem
    val isMeeting = item is MeetingChatRoomItem
    val isOneToOne = item is IndividualChatRoomItem

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(71.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val (avatarImage, titleText, subtitleText, statusIcon) = createRefs()
            val subtitle = when {
                isOneToOne -> item.peerEmail
                isGroup -> stringResource(id = R.string.group_chat_label)
                isMeeting -> {
                    when {
                        item.isRecurring() -> stringResource(id = R.string.meetings_list_recurring_meeting_label)
                        item.isPending -> stringResource(id = R.string.meetings_list_one_off_meeting_label)
                        else -> stringResource(id = R.string.context_meeting)
                    }
                }

                else -> null
            }

            if (isNoteToSelf) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .constrainAs(avatarImage) {
                            start.linkTo(parent.start)
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                        }) {
                    NoteToSelfAvatarView(
                        isHint = item.isEmptyNoteToSelfChatRoom,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("chat_room_item:avatar_image")
                            .size(if (item.isEmptyNoteToSelfChatRoom) 24.dp else 40.dp),
                    )
                }
            } else {
                ChatAvatarView(
                    avatars = item.getChatAvatars(),
                    modifier = Modifier
                        .size(40.dp)
                        .constrainAs(avatarImage) {
                            start.linkTo(parent.start)
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                        }
                )
            }

            MegaText(
                modifier = Modifier.constrainAs(titleText) {
                    linkTo(avatarImage.end, parent.end, 16.dp, 32.dp, 0.dp, 0.dp, 0f)
                    top.linkTo(parent.top)
                    bottom.linkTo(subtitleText.top)
                    width = Dimension.preferredWrapContent
                },
                text = if (isNoteToSelf) stringResource(id = sharedR.string.chat_note_to_self_chat_title) else item.title,
                textColor = TextColor.Primary,
                style = if (isNoteToSelf) MaterialTheme.typography.subtitle1medium else MaterialTheme.typography.subtitle1,
                overflow = LongTextBehaviour.Ellipsis(maxLines = 1),
            )

            val userStatus = if (isOneToOne) item.userChatStatus else null
            if (userStatus != null) {
                ChatUserStatusView(
                    userChatStatus = userStatus,
                    modifier = Modifier.constrainAs(statusIcon) {
                        start.linkTo(titleText.end, 4.dp)
                        top.linkTo(titleText.top)
                        bottom.linkTo(titleText.bottom)
                    },
                )
            }

            if (!subtitle.isNullOrBlank()) {
                MegaText(
                    modifier = Modifier.constrainAs(subtitleText) {
                        linkTo(avatarImage.end, parent.end, 16.dp, 16.dp, 0.dp, 0.dp, 0f)
                        top.linkTo(titleText.bottom)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.preferredWrapContent
                    },
                    text = subtitle,
                    textColor = TextColor.Secondary,
                    style = MaterialTheme.typography.subtitle2,
                    overflow = LongTextBehaviour.Ellipsis(maxLines = 1),
                )
            }

            createVerticalChain(
                titleText,
                subtitleText,
                chainStyle = ChainStyle.Packed
            )
        }

        ChatDivider(startPadding = 16.dp)

        Column(modifier = Modifier.verticalScroll(rememberScrollState()))
        {
            if (item.isArchived) {
                MenuItem(
                    modifier = Modifier.testTag("unarchive"),
                    icon = IconPack.Medium.Regular.Outline.ArchiveArrowUp,
                    text = R.string.general_unarchive,
                    description = "Unarchive",
                    onClick = onUnarchiveClick
                )
            } else {
                if (isMeeting) {
                    if (item.currentCallStatus == ChatRoomItemStatus.NotJoined) {
                        MenuItem(
                            modifier = Modifier.testTag("join_meeting"),
                            icon = IconPack.Medium.Regular.Outline.VideoPlus,
                            text = R.string.meetings_list_join_scheduled_meeting_option,
                            description = "Join meeting",
                            onClick = onStartMeetingClick
                        )
                    } else if (item.currentCallStatus == ChatRoomItemStatus.NotStarted) {
                        MenuItem(
                            modifier = Modifier.testTag("start_meeting"),
                            icon = IconPack.Medium.Regular.Outline.Video,
                            text = R.string.meetings_list_start_scheduled_meeting_option,
                            description = "Start meeting",
                            onClick = onStartMeetingClick
                        )
                    }
                    ChatDivider()

                    if (item.isRecurring() && !item.isCancelled) {
                        MenuItem(
                            modifier = Modifier.testTag("occurrences"),
                            icon = IconPack.Medium.Regular.Outline.RotateCw,
                            text = R.string.meetings_list_recurring_meeting_occurrences_option,
                            description = "Occurrences",
                            onClick = onOccurrencesClick
                        )
                        ChatDivider()
                    }

                    if (item.hasPermissions && item.schedId != null) {
                        MenuItem(
                            modifier = Modifier.testTag("edit"),
                            icon = IconPack.Medium.Regular.Outline.Edit,
                            text = R.string.title_edit_profile_info,
                            description = "Edit",
                            onClick = onEditClick
                        )
                        ChatDivider()
                    }
                }

                if (isGroup || item.hasPermissions) {
                    MenuItem(
                        modifier = Modifier.testTag("info"),
                        icon = IconPack.Medium.Regular.Outline.Info,
                        text = R.string.general_info,
                        description = "Info",
                        onClick = onInfoClick
                    )

                    ChatDivider()
                }

                if (item.hasPermissions && (!isNoteToSelf || !item.isEmptyNoteToSelfChatRoom)) {
                    MenuItem(
                        modifier = Modifier.testTag("clear_chat_history"),
                        icon = IconPack.Medium.Regular.Outline.Eraser,
                        text = R.string.title_properties_chat_clear,
                        description = "Clear chat history",
                        onClick = onClearChatClick
                    )
                    ChatDivider()
                }

                if (!isNoteToSelf && ((isGroup && item.isActive) || (isGroup.not() && item.hasPermissions))) {
                    if (item.isMuted) {
                        MenuItem(
                            modifier = Modifier.testTag("unmute"),
                            icon = IconPack.Medium.Regular.Outline.Bell,
                            text = R.string.general_unmute,
                            description = "Unmute",
                            onClick = onUnmuteClick
                        )
                    } else {
                        MenuItem(
                            modifier = Modifier.testTag("mute"),
                            icon = IconPack.Medium.Regular.Outline.BellOff,
                            text = R.string.general_mute,
                            description = "Mute",
                            onClick = onMuteClick
                        )
                    }

                    ChatDivider()
                }

                MenuItem(
                    modifier = Modifier.testTag("archive"),
                    icon = IconPack.Medium.Regular.Outline.Archive,
                    text = R.string.general_archive,
                    description = "Archive",
                    onClick = {
                        onArchiveClick(isNoteToSelf)
                    }
                )

                when {
                    canCancel(item) -> {
                        ChatDivider()
                        MenuItem(
                            modifier = Modifier.testTag("cancel"),
                            icon = IconPack.Medium.Regular.Outline.Trash,
                            text = sharedR.string.general_dialog_cancel_button,
                            description = "Cancel",
                            tintRed = true,
                            onClick = onCancelClick
                        )
                    }

                    isGroup && item.isActive -> {
                        ChatDivider()
                        MenuItem(
                            modifier = Modifier.testTag("leave"),
                            icon = IconPack.Medium.Regular.Outline.LogOut02,
                            text = R.string.general_leave,
                            description = "Leave",
                            tintRed = true,
                            onClick = onLeaveClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * Check if can cancel the scheduled meeting
 *
 * @param item  [ChatRoomItem] of the scheduled meeting
 */
private fun canCancel(item: ChatRoomItem?): Boolean =
    item is MeetingChatRoomItem && item.isPending && item.hasPermissions

@Composable
private fun MenuItem(
    modifier: Modifier,
    icon: ImageVector,
    @StringRes text: Int,
    description: String,
    tintRed: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .clickable(onClick = onClick)
    ) {
        val iconColor: Color
        val textColor: Color
        if (tintRed) {
            iconColor = MaterialTheme.colors.red_600_red_300
            textColor = MaterialTheme.colors.red_600_red_300
        } else {
            iconColor = MaterialTheme.colors.grey_alpha_054_white_alpha_054
            textColor = MaterialTheme.colors.textColorPrimary
        }
        Icon(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(24.dp)
                .align(Alignment.CenterVertically),
            painter = rememberVectorPainter(icon),
            contentDescription = description,
            tint = iconColor
        )
        Text(
            modifier = Modifier
                .padding(start = 32.dp, end = 16.dp)
                .align(Alignment.CenterVertically),
            text = stringResource(id = text),
            color = textColor,
            style = MaterialTheme.typography.subtitle1
        )
    }
}

@Preview
@Composable
private fun PreviewIndividualChatRoomItemBottomSheetView() {
    ChatRoomItemBottomSheetView(
        modifier = Modifier.background(Color.White),
        item = IndividualChatRoomItem(
            chatId = Random.nextLong(),
            title = "Mieko Kawakami",
            peerEmail = "mieko@miekokawakami.jp",
            userChatStatus = UserChatStatus.Online,
            avatar = ChatAvatarItem("M"),
            lastTimestampFormatted = "1 May 2022 17:53",
            unreadCount = Random.nextInt(150),
            hasPermissions = true,
            isMuted = Random.nextBoolean(),
        ),
    )
}

@Preview
@Composable
private fun PreviewNoteToSelfChatRoomItemBottomSheetView() {
    ChatRoomItemBottomSheetView(
        modifier = Modifier.background(Color.White),
        item = MeetingChatRoomItem(
            chatId = Random.nextLong(),
            schedId = Random.nextLong(),
            title = "Photos Sprint #1",
            lastMessage = "Anna: Seeya all soon!",
            avatars = listOf(ChatAvatarItem("A"), ChatAvatarItem("J")),
            lastTimestampFormatted = "1 May 2022 17:53",
            unreadCount = Random.nextInt(150),
            isMuted = Random.nextBoolean(),
            isRecurringMonthly = Random.nextBoolean(),
            isPublic = Random.nextBoolean(),
        ),
    )
}

@Preview
@Composable
private fun PreviewGroupChatRoomItemBottomSheetView() {
    ChatRoomItemBottomSheetView(
        modifier = Modifier.background(Color.White),
        item = GroupChatRoomItem(
            chatId = Random.nextLong(),
            title = "Vanuatu - Lakatoro&Lorsup (May)",
            lastMessage = "Anna: Seeya all soon!",
            avatars = listOf(ChatAvatarItem("L"), ChatAvatarItem("J")),
            lastTimestampFormatted = "1 May 2022 17:53",
            unreadCount = Random.nextInt(150),
            isMuted = Random.nextBoolean(),
            isPublic = Random.nextBoolean(),
        ),
    )
}

@Preview
@Composable
private fun PreviewMeetingChatRoomItemBottomSheetView() {
    ChatRoomItemBottomSheetView(
        modifier = Modifier.background(Color.White),
        item = MeetingChatRoomItem(
            chatId = Random.nextLong(),
            schedId = Random.nextLong(),
            title = "Photos Sprint #1",
            lastMessage = "Anna: Seeya all soon!",
            avatars = listOf(ChatAvatarItem("A"), ChatAvatarItem("J")),
            lastTimestampFormatted = "1 May 2022 17:53",
            unreadCount = Random.nextInt(150),
            isMuted = Random.nextBoolean(),
            isRecurringMonthly = Random.nextBoolean(),
            isPublic = Random.nextBoolean(),
        ),
    )
}

@Preview
@Composable
private fun PreviewArchivedChatRoomItemBottomSheetView() {
    ChatRoomItemBottomSheetView(
        modifier = Modifier.background(Color.White),
        item = IndividualChatRoomItem(
            chatId = Random.nextLong(),
            title = "Mieko Kawakami",
            peerEmail = "mieko@miekokawakami.jp",
            avatar = ChatAvatarItem("M"),
            lastTimestampFormatted = "1 May 2022 17:53",
            unreadCount = Random.nextInt(150),
            isMuted = Random.nextBoolean(),
            isArchived = true,
        ),
    )
}
