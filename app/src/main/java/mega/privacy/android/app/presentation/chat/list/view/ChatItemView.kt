package mega.privacy.android.app.presentation.chat.list.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.LongPress
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatItem
import mega.privacy.android.domain.entity.chat.ChatItem.MeetingChatItem
import kotlin.random.Random

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatItemView(
    item: ChatItem,
    isSelected: Boolean,
    isSelectionEnabled: Boolean,
    timestampUpdate: Int?,
    onItemClick: (Long) -> Unit,
    onItemMoreClick: (ChatItem) -> Unit,
    onItemSelected: (Long) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(MaterialTheme.colors.surface)
            .combinedClickable(
                onClick = {
                    if (isSelectionEnabled) {
                        hapticFeedback.performHapticFeedback(LongPress)
                        onItemSelected(item.chatId)
                    } else {
                        onItemClick(item.chatId)
                    }
                },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(LongPress)
                    onItemSelected(item.chatId)
                },
            )
            .indication(
                interactionSource = MutableInteractionSource(),
                indication = rememberRipple(bounded = true),
            ),
    ) {
        val (
            avatarImage,
            titleText,
            statusIcon,
            privateIcon,
            muteIcon,
            recurringIcon,
            callIcon,
            lastMessageIcon,
            middleText,
            bottomText,
            moreButton,
            unreadCountIcon,
        ) = createRefs()

        if (isSelected) {
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_select_contact),
                contentDescription = "Selected item",
                modifier = Modifier
                    .testTag("selectedImage")
                    .size(40.dp)
                    .constrainAs(avatarImage) {
                        start.linkTo(parent.start, 16.dp)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    },
            )
        } else {
            ChatAvatarView(
                avatars = item.getChatAvatars(),
                modifier = Modifier
                    .size(40.dp)
                    .constrainAs(avatarImage) {
                        start.linkTo(parent.start, 16.dp)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    },
            )
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.constrainAs(titleText) {
                linkTo(avatarImage.end, parent.end, 16.dp, 56.dp, 72.dp, 0.dp, 0f)
                top.linkTo(parent.top)
                bottom.linkTo(middleText.top)
                width = Dimension.preferredWrapContent
            },
        )

        val userStatus = item.getIndividualUserStatus()
        ChatUserStatusView(
            userStatus = userStatus,
            modifier = Modifier.constrainAs(statusIcon) {
                start.linkTo(titleText.end, 4.dp)
                top.linkTo(titleText.top)
                bottom.linkTo(titleText.bottom)
                visibility = if (userStatus != null) Visibility.Visible else Visibility.Gone
            },
        )

        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_key_02),
            contentDescription = "Private chat icon",
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .size(16.dp)
                .constrainAs(privateIcon) {
                    start.linkTo(statusIcon.end, 4.dp, 4.dp)
                    top.linkTo(titleText.top)
                    bottom.linkTo(titleText.bottom)
                    visibility = if (item.isPublicChat()) Visibility.Gone else Visibility.Visible
                },
        )

        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_bell_off),
            contentDescription = "Mute chat icon",
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .size(16.dp)
                .constrainAs(muteIcon) {
                    start.linkTo(privateIcon.end, 2.dp, 4.dp)
                    top.linkTo(titleText.top)
                    bottom.linkTo(titleText.bottom)
                    visibility = if (item.isMuted) Visibility.Visible else Visibility.Gone
                },
        )

        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_rotate_cw),
            contentDescription = "Recurring meeting icon",
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .size(16.dp)
                .constrainAs(recurringIcon) {
                    start.linkTo(muteIcon.end, 2.dp, 4.dp)
                    top.linkTo(titleText.top)
                    bottom.linkTo(titleText.bottom)
                    visibility = if (item.isRecurringMeeting())
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
                .constrainAs(callIcon) {
                    start.linkTo(recurringIcon.end, 2.dp, 4.dp)
                    top.linkTo(titleText.top)
                    bottom.linkTo(titleText.bottom)
                    visibility = if (item.hasOngoingCall())
                        Visibility.Visible
                    else
                        Visibility.Gone
                },
        )

        Icon(
            imageVector = ImageVector.vectorResource(
                id = if (item.isLastMessageVoiceClip)
                    R.drawable.ic_chat_audio
                else
                    R.drawable.ic_chat_location
            ),
            contentDescription = "Last message location icon",
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .size(16.dp)
                .constrainAs(lastMessageIcon) {
                    start.linkTo(parent.start, 72.dp)
                    top.linkTo(titleText.bottom)
                    bottom.linkTo(bottomText.top)
                    visibility =
                        if (item.isLastMessageVoiceClip || item.isLastMessageGeolocation)
                            Visibility.Visible
                        else
                            Visibility.Gone
                },
        )

        MiddleTextView(
            modifier = Modifier.constrainAs(middleText) {
                linkTo(
                    start = lastMessageIcon.end,
                    end = unreadCountIcon.start,
                    startMargin = 2.dp,
                    endMargin = 8.dp,
                    startGoneMargin = 72.dp,
                    endGoneMargin = 6.dp,
                    bias = 0f
                )
                top.linkTo(titleText.bottom)
                bottom.linkTo(bottomText.top, 5.dp)
                width = Dimension.preferredWrapContent
            },
            timestampUpdate = timestampUpdate,
            lastMessage = item.lastMessage,
            isPending = item is MeetingChatItem && item.isPending,
            scheduledTimestamp = if (item is MeetingChatItem) item.scheduledTimestampFormatted else null,
            highlight = item.highlight,
            hasOngoingCall = item.hasOngoingCall(),
            callDuration = item.getCallDuration(),
            isRecurringDaily = item is MeetingChatItem && item.isRecurringDaily,
            isRecurringWeekly = item is MeetingChatItem && item.isRecurringWeekly,
            isRecurringMonthly = item is MeetingChatItem && item.isRecurringMonthly,
        )

        BottomTextView(
            modifier = Modifier.constrainAs(bottomText) {
                linkTo(
                    start = parent.start,
                    end = unreadCountIcon.start,
                    startMargin = 72.dp,
                    endMargin = 8.dp,
                    startGoneMargin = 72.dp,
                    endGoneMargin = 6.dp,
                    bias = 0f
                )
                bottom.linkTo(parent.bottom)
                top.linkTo(middleText.bottom, 4.dp)
            },
            timestampUpdate = timestampUpdate,
            isRecurring = item is MeetingChatItem && item.isRecurring(),
            isPending = item is MeetingChatItem && item.isPending,
            highlight = item.highlight,
            lastTimestamp = item.lastTimestampFormatted,
            hasOngoingCall = item.hasOngoingCall(),
            callDuration = item.getCallDuration(),
            lastMessage = item.lastMessage,
        )

        IconButton(
            onClick = { onItemMoreClick(item) },
            modifier = Modifier
                .testTag("onItemMore")
                .size(24.dp)
                .constrainAs(moreButton) {
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

        ChatUnreadCountView(
            count = item.unreadCount,
            modifier = Modifier.constrainAs(unreadCountIcon) {
                end.linkTo(moreButton.start, 8.dp)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                visibility = if (item.unreadCount > 0)
                    Visibility.Visible
                else
                    Visibility.Gone
            },
        )

        createVerticalChain(
            titleText,
            middleText,
            bottomText,
            chainStyle = ChainStyle.Packed,
        )
    }
}

@Composable
private fun MiddleTextView(
    modifier: Modifier,
    timestampUpdate: Int?,
    lastMessage: String?,
    isPending: Boolean,
    scheduledTimestamp: String?,
    highlight: Boolean,
    hasOngoingCall: Boolean,
    callDuration: String?,
    isRecurringDaily: Boolean,
    isRecurringWeekly: Boolean,
    isRecurringMonthly: Boolean,
) {
    val textMessage = when {
        isPending && !scheduledTimestamp.isNullOrBlank() ->
            when {
                isRecurringDaily -> stringResource(
                    R.string.meetings_list_scheduled_meeting_daily_label,
                    scheduledTimestamp
                )

                isRecurringWeekly -> stringResource(
                    R.string.meetings_list_scheduled_meeting_weekly_label,
                    scheduledTimestamp
                )

                isRecurringMonthly -> stringResource(
                    R.string.meetings_list_scheduled_meeting_monthly_label,
                    scheduledTimestamp
                )

                else -> scheduledTimestamp
            }

        timestampUpdate != null && !isPending && hasOngoingCall ->
            "$lastMessage · $callDuration"

        else ->
            lastMessage
    }

    val textColor = when {
        isPending -> MaterialTheme.colors.red_600_red_300
        highlight -> MaterialTheme.colors.secondary
        else -> MaterialTheme.colors.textColorSecondary
    }

    Text(
        text = textMessage ?: stringResource(R.string.error_message_unrecognizable),
        color = textColor,
        style = MaterialTheme.typography.subtitle2,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.placeholder(
            color = Color.LightGray,
            shape = RoundedCornerShape(4.dp),
            highlight = PlaceholderHighlight.fade(Color.White),
            visible = textMessage.isNullOrBlank(),
        ),
    )
}

@Composable
private fun BottomTextView(
    modifier: Modifier,
    timestampUpdate: Int?,
    isRecurring: Boolean,
    isPending: Boolean,
    highlight: Boolean,
    lastTimestamp: String?,
    hasOngoingCall: Boolean,
    callDuration: String?,
    lastMessage: String?,
) {
    val textColor: Color
    var textMessage: String?
    when {
        isPending && highlight -> {
            textColor = MaterialTheme.colors.secondary
            textMessage = lastMessage
        }

        isPending && !highlight -> {
            textColor = MaterialTheme.colors.textColorSecondary
            textMessage = if (isRecurring) {
                stringResource(R.string.meetings_list_recurring_meeting_label)
            } else {
                stringResource(R.string.meetings_list_upcoming_meeting_label)
            }
        }

        else -> {
            textColor = MaterialTheme.colors.textColorSecondary
            textMessage = lastTimestamp
        }
    }

    if (timestampUpdate != null && isPending && !textMessage.isNullOrBlank() && hasOngoingCall) {
        textMessage = "$textMessage · $callDuration"
    }

    val showPlaceholder = textMessage.isNullOrBlank()
    Text(
        text = textMessage ?: stringResource(R.string.error_message_unrecognizable),
        color = textColor,
        style = MaterialTheme.typography.caption,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .padding(top = if (showPlaceholder) 4.dp else 0.dp)
            .placeholder(
                color = Color.LightGray,
                shape = RoundedCornerShape(4.dp),
                highlight = PlaceholderHighlight.fade(Color.White),
                visible = showPlaceholder,
            ),
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewChatItemView() {
    val meeting = MeetingChatItem(
        chatId = Random.nextLong(),
        schedId = Random.nextLong(),
        title = "Photos Sprint #${Random.nextInt()}",
        lastMessage = "Anna: Seeya all soon!",
        avatars = listOf(ChatAvatarItem("A"), ChatAvatarItem("J")),
        lastTimestampFormatted = "1 May 2022 17:53",
        unreadCount = Random.nextInt(150),
        isMuted = Random.nextBoolean(),
        isRecurringMonthly = Random.nextBoolean(),
        isPublic = Random.nextBoolean(),
    )
    ChatItemView(
        item = meeting,
        isSelected = Random.nextBoolean(),
        isSelectionEnabled = Random.nextBoolean(),
        timestampUpdate = 0,
        onItemClick = {},
        onItemMoreClick = {},
    ) {}
}
