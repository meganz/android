package mega.privacy.android.app.presentation.chat.dialog.view

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.list.view.ChatAvatarView
import mega.privacy.android.app.presentation.chat.list.view.ChatDivider
import mega.privacy.android.app.presentation.chat.list.view.ChatUserStatusView
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_054_white_alpha_054
import mega.privacy.android.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.IndividualChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItemStatus
import mega.privacy.android.domain.entity.contacts.UserStatus
import kotlin.random.Random

/**
 * Chat Room Item bottom sheet view
 */
@Composable
internal fun ChatRoomItemBottomSheetView(
    modifier: Modifier = Modifier,
    item: ChatRoomItem?,
    onStartMeetingClick: () -> Unit = {},
    onOccurrencesClick: () -> Unit = {},
    onInfoClick: () -> Unit = {},
    onClearChatClick: () -> Unit = {},
    onMuteClick: () -> Unit = {},
    onUnmuteClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onUnarchiveClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onLeaveClick: () -> Unit = {},
    isCancelSchedMeetingEnabled: Boolean,
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(71.dp)
        ) {
            val (avatarImage, titleText, subtitleText, statusIcon) = createRefs()
            val subtitle = when (item) {
                is IndividualChatRoomItem -> item.peerEmail
                is ChatRoomItem.GroupChatRoomItem -> stringResource(id = R.string.group_chat_label)
                is ChatRoomItem.MeetingChatRoomItem -> {
                    when {
                        item.isRecurring() -> stringResource(id = R.string.meetings_list_recurring_meeting_label)
                        item.isPending -> stringResource(id = R.string.meetings_list_one_off_meeting_label)
                        else -> stringResource(id = R.string.context_meeting)
                    }
                }
            }

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

            Text(
                text = item.title,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.textColorPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.constrainAs(titleText) {
                    linkTo(avatarImage.end, parent.end, 16.dp, 32.dp, 0.dp, 0.dp, 0f)
                    top.linkTo(parent.top)
                    bottom.linkTo(subtitleText.top)
                    width = Dimension.preferredWrapContent
                }
            )

            val userStatus = if (item is IndividualChatRoomItem) item.userStatus else null
            if (userStatus != null) {
                ChatUserStatusView(
                    userStatus = userStatus,
                    modifier = Modifier.constrainAs(statusIcon) {
                        start.linkTo(titleText.end, 4.dp)
                        top.linkTo(titleText.top)
                        bottom.linkTo(titleText.bottom)
                    },
                )
            }

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = MaterialTheme.colors.textColorSecondary,
                    style = MaterialTheme.typography.subtitle2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.constrainAs(subtitleText) {
                        linkTo(avatarImage.end, parent.end, 16.dp, 16.dp, 0.dp, 0.dp, 0f)
                        top.linkTo(titleText.bottom)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.preferredWrapContent
                    }
                )
            }

            createVerticalChain(
                titleText,
                subtitleText,
                chainStyle = ChainStyle.Packed
            )
        }

        ChatDivider(startPadding = 16.dp)

        if (item.isArchived) {
            MenuItem(
                modifier = Modifier.testTag("unarchive"),
                res = R.drawable.ic_chat_archive_off,
                text = R.string.general_unarchive,
                description = "Unarchive",
                onClick = onUnarchiveClick
            )
        } else {
            if (item is ChatRoomItem.MeetingChatRoomItem) {
                if (item.currentCallStatus is ChatRoomItemStatus.NotJoined) {
                    MenuItem(
                        modifier = Modifier.testTag("join_meeting"),
                        res = R.drawable.join_sched_icon,
                        text = R.string.meetings_list_join_scheduled_meeting_option,
                        description = "Join meeting",
                        onClick = onStartMeetingClick
                    )
                } else if (item.currentCallStatus is ChatRoomItemStatus.NotStarted) {
                    MenuItem(
                        modifier = Modifier.testTag("start_meeting"),
                        res = R.drawable.start_sched_icon,
                        text = R.string.meetings_list_start_scheduled_meeting_option,
                        description = "Start meeting",
                        onClick = onStartMeetingClick
                    )
                }
                ChatDivider()

                if (item.isRecurring()) {
                    MenuItem(
                        modifier = Modifier.testTag("occurrences"),
                        res = R.drawable.occurrences_icon,
                        text = R.string.meetings_list_recurring_meeting_occurrences_option,
                        description = "Occurrences",
                        onClick = onOccurrencesClick
                    )
                    ChatDivider()
                }
            }

            MenuItem(
                modifier = Modifier.testTag("info"),
                res = R.drawable.info_ic,
                text = R.string.general_info,
                description = "Info",
                onClick = onInfoClick
            )

            ChatDivider()

            if (item.hasPermissions) {
                MenuItem(
                    modifier = Modifier.testTag("clear_chat_history"),
                    res = R.drawable.ic_eraser,
                    text = R.string.title_properties_chat_clear,
                    description = "Clear chat history",
                    onClick = onClearChatClick
                )
                ChatDivider()
            }

            if (item.isMuted) {
                MenuItem(
                    modifier = Modifier.testTag("unmute"),
                    res = R.drawable.ic_bell,
                    text = R.string.general_unmute,
                    description = "Unmute",
                    onClick = onUnmuteClick
                )
            } else {
                MenuItem(
                    modifier = Modifier.testTag("mute"),
                    res = R.drawable.ic_bell_off,
                    text = R.string.general_mute,
                    description = "Mute",
                    onClick = onMuteClick
                )
            }
            ChatDivider()

            MenuItem(
                modifier = Modifier.testTag("archive"),
                res = R.drawable.ic_chat_archive,
                text = R.string.general_archive,
                description = "Archive",
                onClick = onArchiveClick
            )

            when {
                canCancel(item, isCancelSchedMeetingEnabled) -> {
                    ChatDivider()
                    MenuItem(
                        modifier = Modifier.testTag("cancel"),
                        res = R.drawable.ic_trash,
                        text = R.string.general_cancel,
                        description = "Cancel",
                        tintRed = true,
                        onClick = onCancelClick
                    )
                }

                item is ChatRoomItem.GroupChatRoomItem -> {
                    ChatDivider()
                    MenuItem(
                        modifier = Modifier.testTag("leave"),
                        res = R.drawable.ic_log_out,
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

/**
 * Check if can cancel the scheduled meeting
 *
 * @param item  [ChatRoomItem] of the scheduled meeting
 * @param isCancelSchedMeetingEnabled   Cancel scheduled meeting feature flag status
 */
private fun canCancel(item: ChatRoomItem?, isCancelSchedMeetingEnabled: Boolean): Boolean =
    item is ChatRoomItem.MeetingChatRoomItem && item.isPending && item.hasPermissions && isCancelSchedMeetingEnabled

@Composable
private fun MenuItem(
    modifier: Modifier,
    @DrawableRes res: Int,
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
            painter = painterResource(id = res),
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
            userStatus = UserStatus.Online,
            avatar = ChatAvatarItem("M"),
            lastTimestampFormatted = "1 May 2022 17:53",
            unreadCount = Random.nextInt(150),
            hasPermissions = true,
            isMuted = Random.nextBoolean(),
        ),
        isCancelSchedMeetingEnabled = true,
    )
}

@Preview
@Composable
private fun PreviewGroupChatRoomItemBottomSheetView() {
    ChatRoomItemBottomSheetView(
        modifier = Modifier.background(Color.White),
        item = ChatRoomItem.GroupChatRoomItem(
            chatId = Random.nextLong(),
            title = "Vanuatu - Lakatoro&Lorsup (May)",
            lastMessage = "Anna: Seeya all soon!",
            avatars = listOf(ChatAvatarItem("L"), ChatAvatarItem("J")),
            lastTimestampFormatted = "1 May 2022 17:53",
            unreadCount = Random.nextInt(150),
            isMuted = Random.nextBoolean(),
            isPublic = Random.nextBoolean(),
        ),
        isCancelSchedMeetingEnabled = true,
    )
}

@Preview
@Composable
private fun PreviewMeetingChatRoomItemBottomSheetView() {
    ChatRoomItemBottomSheetView(
        modifier = Modifier.background(Color.White),
        item = ChatRoomItem.MeetingChatRoomItem(
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
        isCancelSchedMeetingEnabled = true,
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
        isCancelSchedMeetingEnabled = true,
    )
}