package mega.privacy.android.app.presentation.meeting.list.view

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.LongPress
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.view.MeetingAvatarView
import mega.privacy.android.core.ui.theme.extensions.red600_300
import mega.privacy.android.core.ui.theme.extensions.teal_300_200
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus
import kotlin.random.Random

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MeetingItemView(
    modifier: Modifier = Modifier,
    meeting: MeetingRoomItem,
    isSelected: Boolean,
    isSelectionEnabled: Boolean,
    timestampUpdate: Int?,
    onItemClick: (Long) -> Unit,
    onItemMoreClick: (Long) -> Unit,
    onItemSelected: (Long) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(MaterialTheme.colors.surface)
            .combinedClickable(
                onClick = {
                    if (isSelectionEnabled) {
                        hapticFeedback.performHapticFeedback(LongPress)
                        onItemSelected(meeting.chatId)
                    } else {
                        onItemClick(meeting.chatId)
                    }
                },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(LongPress)
                    onItemSelected(meeting.chatId)
                },
            )
            .indication(
                interactionSource = MutableInteractionSource(),
                indication = rememberRipple(bounded = true),
            )
    ) {
        val (imgAvatar, txtTitle, imgPrivate, imgMute, imgRecurring, imgCall, imgLastMessage, txtMiddle, txtBottom, btnMore, txtUnreadCount) = createRefs()

        if (isSelected) {
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_select_contact),
                contentDescription = "Selected meeting",
                modifier = Modifier
                    .size(40.dp)
                    .constrainAs(imgAvatar) {
                        start.linkTo(parent.start, 16.dp)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    },
            )
        } else {
            AvatarView(
                meeting = meeting,
                modifier = Modifier
                    .size(40.dp)
                    .constrainAs(imgAvatar) {
                        start.linkTo(parent.start, 16.dp)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
            )
        }

        Text(
            text = meeting.title,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.constrainAs(txtTitle) {
                linkTo(imgAvatar.end, parent.end, 16.dp, 56.dp, 72.dp, 0.dp, 0f)
                top.linkTo(parent.top)
                bottom.linkTo(txtMiddle.top)
                width = Dimension.preferredWrapContent
            }
        )

        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_chat_private),
            contentDescription = "Private chat icon",
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .size(16.dp)
                .constrainAs(imgPrivate) {
                    start.linkTo(txtTitle.end, 4.dp)
                    top.linkTo(txtTitle.top)
                    bottom.linkTo(txtTitle.bottom)
                    visibility = if (meeting.isPublic) Visibility.Gone else Visibility.Visible
                },
        )

        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_chat_mute),
            contentDescription = "Mute chat icon",
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .size(16.dp)
                .constrainAs(imgMute) {
                    start.linkTo(imgPrivate.end, 2.dp, 4.dp)
                    top.linkTo(txtTitle.top)
                    bottom.linkTo(txtTitle.bottom)
                    visibility = if (meeting.isMuted) Visibility.Visible else Visibility.Gone
                },
        )

        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_rotate_cw),
            contentDescription = "Recurring chat icon",
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .size(16.dp)
                .constrainAs(imgRecurring) {
                    start.linkTo(imgMute.end, 2.dp, 4.dp)
                    top.linkTo(txtTitle.top)
                    bottom.linkTo(txtTitle.bottom)
                    visibility = if (meeting.isRecurring())
                        Visibility.Visible
                    else
                        Visibility.Gone
                },
        )

        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_ongoing_call),
            contentDescription = "Ongoing call icon",
            tint = MaterialTheme.colors.secondary,
            modifier = Modifier
                .size(14.dp)
                .constrainAs(imgCall) {
                    start.linkTo(imgRecurring.end, 2.dp, 4.dp)
                    top.linkTo(txtTitle.top)
                    bottom.linkTo(txtTitle.bottom)
                    visibility =
                        if (meeting.scheduledMeetingStatus == null
                            || meeting.scheduledMeetingStatus is ScheduledMeetingStatus.NotStarted
                        )
                            Visibility.Gone
                        else
                            Visibility.Visible
                },
        )

        Icon(
            imageVector = ImageVector.vectorResource(
                id = if (meeting.isLastMessageVoiceClip)
                    R.drawable.ic_chat_audio
                else
                    R.drawable.ic_chat_location
            ),
            contentDescription = "Last message location icon",
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .size(16.dp)
                .constrainAs(imgLastMessage) {
                    start.linkTo(parent.start, 72.dp)
                    top.linkTo(txtTitle.bottom)
                    bottom.linkTo(txtBottom.top)
                    visibility =
                        if (meeting.isLastMessageVoiceClip || meeting.isLastMessageGeolocation)
                            Visibility.Visible
                        else
                            Visibility.Gone
                },
        )

        MiddleTextView(
            meeting = meeting,
            timestampUpdate = timestampUpdate.takeIf { !meeting.isPending },
            modifier = Modifier.constrainAs(txtMiddle) {
                linkTo(
                    start = imgLastMessage.end,
                    end = txtUnreadCount.start,
                    startMargin = 2.dp,
                    endMargin = 8.dp,
                    startGoneMargin = 72.dp,
                    endGoneMargin = 6.dp,
                    bias = 0f
                )
                top.linkTo(txtTitle.bottom)
                bottom.linkTo(txtBottom.top, 5.dp)
                width = Dimension.preferredWrapContent
            }
        )

        BottomTextView(
            meeting = meeting,
            timestampUpdate = timestampUpdate.takeIf { meeting.isPending },
            modifier = Modifier.constrainAs(txtBottom) {
                start.linkTo(parent.start, 72.dp)
                bottom.linkTo(parent.bottom)
                top.linkTo(txtMiddle.bottom, 4.dp)
            }
        )

        IconButton(
            onClick = { onItemMoreClick(meeting.chatId) },
            modifier = Modifier
                .size(24.dp)
                .constrainAs(btnMore) {
                    end.linkTo(parent.end, 16.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_more),
                contentDescription = "See more Icon",
                tint = MaterialTheme.colors.onSurface,
            )
        }

        UnreadCountView(
            count = meeting.unreadCount,
            modifier = Modifier.constrainAs(txtUnreadCount) {
                end.linkTo(btnMore.start, 8.dp)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                visibility = if (meeting.unreadCount > 0)
                    Visibility.Visible
                else
                    Visibility.Gone
            }
        )

        createVerticalChain(
            txtTitle,
            txtMiddle,
            txtBottom,
            chainStyle = ChainStyle.Packed
        )
    }
}

@Composable
private fun MiddleTextView(modifier: Modifier, meeting: MeetingRoomItem, timestampUpdate: Int?) {
    val meetingScheduledTimestamp = meeting.scheduledTimestampFormatted
    val meetingLastMessage = meeting.lastMessage

    val textMessage = when {
        meeting.isPending && !meetingScheduledTimestamp.isNullOrBlank() ->
            when {
                meeting.isRecurringDaily -> stringResource(
                    R.string.meetings_list_scheduled_meeting_daily_label,
                    meetingScheduledTimestamp
                )
                meeting.isRecurringWeekly -> stringResource(
                    R.string.meetings_list_scheduled_meeting_weekly_label,
                    meetingScheduledTimestamp
                )
                meeting.isRecurringMonthly -> stringResource(
                    R.string.meetings_list_scheduled_meeting_monthly_label,
                    meetingScheduledTimestamp
                )
                else -> meetingScheduledTimestamp
            }
        timestampUpdate != null && meeting.hasOngoingCall() ->
            "$meetingLastMessage · ${meeting.getCallDuration()}"
        else ->
            meetingLastMessage
    }

    val textColor = if (meeting.isPending) {
        MaterialTheme.colors.red600_300
    } else {
        if (meeting.highlight) {
            MaterialTheme.colors.teal_300_200
        } else {
            MaterialTheme.colors.textColorSecondary
        }
    }

    Text(
        text = textMessage.takeIf { !it.isNullOrBlank() }
            ?: stringResource(R.string.error_message_unrecognizable),
        color = textColor,
        style = MaterialTheme.typography.subtitle2,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun BottomTextView(modifier: Modifier, meeting: MeetingRoomItem, timestampUpdate: Int?) {
    var textMessage: String?
    val textColor: Color
    when {
        meeting.isPending -> {
            if (meeting.highlight) {
                textMessage = meeting.lastMessage
                textColor = MaterialTheme.colors.teal_300_200
            } else {
                textMessage = if (meeting.isRecurring()) {
                    stringResource(R.string.meetings_list_recurring_meeting_label)
                } else {
                    stringResource(R.string.meetings_list_upcoming_meeting_label)
                }
                textColor = MaterialTheme.colors.textColorSecondary
            }
        }
        else -> {
            textMessage = meeting.lastTimestampFormatted
            textColor = MaterialTheme.colors.textColorSecondary
        }
    }

    if (timestampUpdate != null && !textMessage.isNullOrBlank() && meeting.hasOngoingCall()) {
        textMessage = "$textMessage · ${meeting.getCallDuration()}"
    }

    Text(
        text = textMessage.takeIf { !it.isNullOrBlank() }
            ?: stringResource(R.string.error_message_unrecognizable),
        color = textColor,
        style = MaterialTheme.typography.caption,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun UnreadCountView(modifier: Modifier, count: Int) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .size(24.dp)
            .background(MaterialTheme.colors.secondary),
        contentAlignment = Alignment.Center
    ) {
        val countText = if (count >= 99) "99" else count.toString()
        Text(
            text = countText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.surface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AvatarView(
    modifier: Modifier = Modifier,
    meeting: MeetingRoomItem,
) {
    Box(
        modifier.background(Color.Transparent)
    ) {
        if (meeting.isSingleMeeting()) {
            MeetingAvatarView(
                avatarUri = meeting.firstUserAvatar,
                avatarPlaceholder = meeting.firstUserChar?.takeIf(String::isNotBlank)
                    ?: meeting.title,
                avatarColor = meeting.firstUserColor,
            )
        } else {
            MeetingAvatarView(
                avatarUri = meeting.secondUserAvatar,
                avatarPlaceholder = meeting.secondUserChar?.takeIf(String::isNotBlank)
                    ?: meeting.title,
                avatarColor = meeting.secondUserColor,
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.BottomEnd)
            )
            MeetingAvatarView(
                avatarUri = meeting.firstUserAvatar,
                avatarPlaceholder = meeting.firstUserChar?.takeIf(String::isNotBlank)
                    ?: meeting.title,
                avatarColor = meeting.firstUserColor,
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.TopStart)
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "PreviewMeetingItemView")
@Composable
private fun PreviewMeetingItemView() {
    val meetingStatus =
        if (Random.nextBoolean()) ScheduledMeetingStatus.NotJoined(Random.nextLong(70)) else null
    val meeting = MeetingRoomItem(
        chatId = Random.nextLong(),
        title = "Photos Sprint #${Random.nextInt()}",
        lastMessage = "Anna: Seeya all soon!",
        firstUserChar = "A",
        secondUserChar = "J",
        lastTimestampFormatted = "1 May 2022 17:53",
        unreadCount = Random.nextInt(150),
        isMuted = Random.nextBoolean(),
        isRecurringMonthly = Random.nextBoolean(),
        isPublic = Random.nextBoolean(),
        scheduledMeetingStatus = meetingStatus
    )
    MeetingItemView(
        meeting = meeting,
        isSelected = Random.nextBoolean(),
        isSelectionEnabled = Random.nextBoolean(),
        timestampUpdate = 0,
        onItemClick = {},
        onItemMoreClick = {},
    ) {}
}
