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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.MeetingChatRoomItem
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * Chat room item view
 *
 * @param item                  [ChatRoomItem]
 * @param isSelected
 * @param isSelectionEnabled
 * @param onItemClick
 * @param onItemMoreClick
 * @param onItemSelected
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatRoomItemView(
    item: ChatRoomItem,
    isSelected: Boolean,
    isSelectionEnabled: Boolean,
    onItemClick: (Long) -> Unit,
    onItemMoreClick: (ChatRoomItem) -> Unit,
    onItemSelected: (Long) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val hasOngoingCall = item.hasOngoingCall()
    val isLoading = item.lastTimestampFormatted.isNullOrBlank()
    var callDuration by remember { mutableStateOf<Long>(0) }

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
            modifier = Modifier
                .constrainAs(titleText) {
                    linkTo(avatarImage.end, parent.end, 16.dp, 56.dp, 72.dp, 0.dp, 0f)
                    top.linkTo(parent.top)
                    bottom.linkTo(middleText.top)
                    width = Dimension.preferredWrapContent
                }
                .placeholder(
                    color = Color.LightGray,
                    shape = RoundedCornerShape(4.dp),
                    highlight = PlaceholderHighlight.fade(Color.White),
                    visible = isLoading,
                ),
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
                    visibility = if (hasOngoingCall)
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
            isLoading = isLoading,
            lastMessage = item.lastMessage,
            isPending = item is MeetingChatRoomItem && item.isPending,
            scheduledTimestamp = if (item is MeetingChatRoomItem) item.scheduledTimestampFormatted else null,
            highlight = item.highlight,
            callDuration = callDuration,
            isRecurringDaily = item is MeetingChatRoomItem && item.isRecurringDaily,
            isRecurringWeekly = item is MeetingChatRoomItem && item.isRecurringWeekly,
            isRecurringMonthly = item is MeetingChatRoomItem && item.isRecurringMonthly,
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
            isLoading = isLoading,
            isRecurring = item is MeetingChatRoomItem && item.isRecurring(),
            isPending = item is MeetingChatRoomItem && item.isPending,
            highlight = item.highlight,
            lastTimestamp = item.lastTimestampFormatted,
            callDuration = callDuration,
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

        LaunchedEffect(hasOngoingCall) {
            callDuration = item.currentCallStatus?.getDuration() ?: 0
            while (hasOngoingCall) {
                delay(1.seconds)
                callDuration++
            }
            callDuration = 0
        }

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
    isLoading: Boolean,
    lastMessage: String?,
    isPending: Boolean,
    scheduledTimestamp: String?,
    highlight: Boolean,
    callDuration: Long,
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

        !isPending && callDuration != 0L ->
            "$lastMessage · ${callDuration.formatCallTime()}"

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
        modifier = modifier
            .padding(vertical = if (isLoading) 2.dp else 0.dp)
            .placeholder(
                color = Color.LightGray,
                shape = RoundedCornerShape(4.dp),
                highlight = PlaceholderHighlight.fade(Color.White),
                visible = isLoading,
            ),
    )
}

@Composable
private fun BottomTextView(
    modifier: Modifier,
    isLoading: Boolean,
    isRecurring: Boolean,
    isPending: Boolean,
    highlight: Boolean,
    lastTimestamp: String?,
    callDuration: Long,
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

    if (isPending && !textMessage.isNullOrBlank() && callDuration != 0L)
        textMessage = "$textMessage · ${callDuration.formatCallTime()}"

    Text(
        text = textMessage ?: stringResource(R.string.error_message_unrecognizable),
        color = textColor,
        style = MaterialTheme.typography.caption,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.placeholder(
            color = Color.LightGray,
            shape = RoundedCornerShape(4.dp),
            highlight = PlaceholderHighlight.fade(Color.White),
            visible = isLoading,
        ),
    )
}

private fun Long.formatCallTime() =
    String.format(
        "%02d:%02d",
        TimeUnit.SECONDS.toMinutes(this) % 60,
        TimeUnit.SECONDS.toSeconds(this) % 60
    )

@CombinedThemePreviews
@Composable
private fun PreviewChatRoomItemView() {
    val meeting = MeetingChatRoomItem(
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
    ChatRoomItemView(
        item = meeting,
        isSelected = Random.nextBoolean(),
        isSelectionEnabled = Random.nextBoolean(),
        onItemClick = {},
        onItemMoreClick = {},
    ) {}
}
