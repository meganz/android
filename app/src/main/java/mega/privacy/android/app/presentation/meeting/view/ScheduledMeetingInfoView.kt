package mega.privacy.android.app.presentation.meeting.view

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.group.data.GroupChatParticipant
import mega.privacy.android.app.meeting.list.adapter.ScheduledMeetingItem
import mega.privacy.android.app.presentation.chat.view.ParticipantItemView
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.extensions.icon
import mega.privacy.android.app.presentation.extensions.title
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoAction
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.presentation.theme.AndroidTheme
import mega.privacy.android.presentation.theme.black
import mega.privacy.android.presentation.theme.grey_alpha_012
import mega.privacy.android.presentation.theme.grey_alpha_054
import mega.privacy.android.presentation.theme.red_300
import mega.privacy.android.presentation.theme.red_600
import mega.privacy.android.presentation.theme.white
import mega.privacy.android.presentation.theme.white_alpha_012
import mega.privacy.android.presentation.theme.white_alpha_054

@Composable
fun ScheduledMeetingInfoView(
    state: ScheduledMeetingInfoState,
    onButtonClicked: (ScheduledMeetingInfoAction) -> Unit = {},
    onEditClicked: () -> Unit,
    onAddParticipantsClicked: () -> Unit,
    onSeeMoreClicked: () -> Unit,
    onLeaveGroupClicked: () -> Unit,
    onParticipantClicked: (ContactItem) -> Unit,
    onScrollChange: (Boolean) -> Unit,
    onBackPressed: () -> Unit,
) {
    val listState = rememberLazyListState()
    val firstItemVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            ScheduledMeetingInfoAppBar(
                state = state,
                onEditClicked = onEditClicked,
                onAddParticipantsClicked = onAddParticipantsClicked,
                onBackPressed = onBackPressed,
                titleId = R.string.general_info,
                elevation = !firstItemVisible
            )
        }
    ) { paddingValues ->
        LazyColumn(state = listState,
            modifier = Modifier.padding(paddingValues)) {
            item(key = "Scheduled meeting title") { ScheduledMeetingTitleView(uiState = state) }

            state.apply {
                items(buttons) { button ->
                    ActionButton(state = state, action = button,
                        scheduledMeeting = scheduledMeeting,
                        onButtonClicked = onButtonClicked)
                }

                item(key = "Participants") { ParticipantsHeader(state = state) }

                item(key = "Add participants") {
                    AddParticipantsButton(state = state,
                        onAddParticipantsClicked = onAddParticipantsClicked)
                }
                val participantsList = state.participantItemList

                if (state.seeMoreVisible && state.participantItemList.size > 4) {
                    item(key = participantsList[0].handle) {
                        ParticipantItemView(participant = participantsList[0]) {
                            onParticipantClicked(participantsList[0])
                        }
                    }
                    item(key = participantsList[1].handle) {
                        ParticipantItemView(participant = participantsList[1]) {
                            onParticipantClicked(participantsList[1])
                        }
                    }
                    item(key = participantsList[2].handle) {
                        ParticipantItemView(participant = participantsList[2]) {
                            onParticipantClicked(participantsList[2])
                        }
                    }
                    item(key = participantsList[3].handle) {
                        ParticipantItemView(participant = participantsList[3]) {
                            onParticipantClicked(participantsList[3])
                        }
                    }

                    item(key = "See more participants") {
                        SeeMoreParticipantsButton(onSeeMoreClicked = onSeeMoreClicked)
                    }
                } else {
                    participantsList.forEach { participant ->
                        item(key = participant.handle) {
                            ParticipantItemView(participant) {
                                onParticipantClicked(participant)
                            }
                        }
                    }
                }

                item(key = "Leave group") {
                    LeaveGroupButton(onLeaveGroupClicked = onLeaveGroupClicked)
                }
            }
        }
    }

    onScrollChange(!firstItemVisible)
}

@Composable
private fun ScheduledMeetingInfoAppBar(
    state: ScheduledMeetingInfoState,
    onEditClicked: () -> Unit,
    onAddParticipantsClicked: () -> Unit,
    onBackPressed: () -> Unit,
    titleId: Int,
    elevation: Boolean,
) {
    val iconColor = if (MaterialTheme.colors.isLight) Color.Black else Color.White
    TopAppBar(
        title = {
            Text(text = stringResource(id = titleId),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium)
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back button",
                    tint = iconColor)
            }
        },
        actions = {
            if (state.scheduledMeeting.isHost || state.scheduledMeeting.isAllowAddParticipants) {
                IconButton(onClick = { onAddParticipantsClicked() }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.add_participants),
                        contentDescription = "Add participants Icon",
                        tint = iconColor
                    )
                }
            }

            if (state.scheduledMeeting.isHost) {
                IconButton(onClick = { onEditClicked() }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_scheduled_meeting_edit),
                        contentDescription = "Edit Icon",
                        tint = iconColor
                    )
                }
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp
    )
}

@Composable
private fun ScheduledMeetingTitleView(uiState: ScheduledMeetingInfoState) {
    Column {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .size(40.dp)
                .background(Color.Transparent)) {
                MeetingAvatar(scheduledMeetingItem = uiState.scheduledMeeting)
            }
            Column(modifier = Modifier
                .padding(start = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = uiState.scheduledMeeting.title,
                        style = MaterialTheme.typography.subtitle1,
                        color = if (MaterialTheme.colors.isLight) black else white,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                Text(text = uiState.scheduledMeeting.date,
                    style = MaterialTheme.typography.subtitle2,
                    color = if (MaterialTheme.colors.isLight) grey_alpha_054 else white_alpha_054,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }

        divider(withStartPadding = false)
    }
}

@Composable
private fun MeetingAvatar(scheduledMeetingItem: ScheduledMeetingItem) {
    if (scheduledMeetingItem.isEmptyMeeting()) {
        DefaultAvatar(titleChat = scheduledMeetingItem.title)
    } else if (scheduledMeetingItem.isSingleMeeting()) {
        OneParticipantAvatar(firstUser = scheduledMeetingItem.firstUser ?: return)
    } else {
        SeveralParticipantsAvatar(firstUser = scheduledMeetingItem.firstUser ?: return,
            lastUser = scheduledMeetingItem.lastUser ?: return)
    }
}

@Composable
private fun DefaultAvatar(titleChat: String) {
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(color = Color(AvatarUtil.getSpecificAvatarColor(Constants.AVATAR_GROUP_CHAT_COLOR)),
                shape = CircleShape)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val currentHeight = placeable.height
                var heightCircle = currentHeight
                if (placeable.width > heightCircle)
                    heightCircle = placeable.width

                layout(heightCircle, heightCircle) {
                    placeable.placeRelative(0, (heightCircle - currentHeight) / 2)
                }
            }) {
        Text(
            text = AvatarUtil.getFirstLetter(titleChat),
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.h6
        )
    }
}

@Composable
fun OneParticipantAvatar(firstUser: GroupChatParticipant) {
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(color = Color(AvatarUtil.getColorAvatar(firstUser.user.handle)),
                shape = CircleShape)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val currentHeight = placeable.height
                var heightCircle = currentHeight
                if (placeable.width > heightCircle)
                    heightCircle = placeable.width

                layout(heightCircle, heightCircle) {
                    placeable.placeRelative(0, (heightCircle - currentHeight) / 2)
                }
            }) {
        Text(
            text = AvatarUtil.getFirstLetter(firstUser.user.contactData.fullName),
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.h6
        )
    }
}

@Composable
fun SeveralParticipantsAvatar(
    firstUser: GroupChatParticipant,
    lastUser: GroupChatParticipant,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .border(width = 1.dp, color = Color.White, shape = CircleShape)
                .clip(CircleShape)
                .align(Alignment.BottomEnd)
                .background(color = Color(AvatarUtil.getSpecificAvatarColor(lastUser.user.defaultAvatarColor)),
                    shape = CircleShape)
        ) {
            Text(
                text = AvatarUtil.getFirstLetter(lastUser.user.contactData.fullName),
                textAlign = TextAlign.Center,
                color = Color.White,
                style = MaterialTheme.typography.subtitle1
            )
        }
        Box(contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .border(width = 1.dp, color = Color.White, shape = CircleShape)
                .clip(CircleShape)
                .align(Alignment.TopStart)
                .background(color = Color(AvatarUtil.getSpecificAvatarColor(firstUser.user.defaultAvatarColor)),
                    shape = CircleShape)
        ) {
            Text(
                text = AvatarUtil.getFirstLetter(firstUser.user.contactData.fullName),
                textAlign = TextAlign.Center,
                color = Color.White,
                style = MaterialTheme.typography.subtitle1
            )
        }
    }
}

@Composable
private fun ParticipantsHeader(state: ScheduledMeetingInfoState) {
    Text(modifier = Modifier.padding(start = 16.dp,
        top = 17.dp,
        end = 16.dp,
        bottom = 12.dp),
        text = stringResource(id = R.string.participants_number, state.participantItemList.size),
        style = MaterialTheme.typography.body2,
        fontWeight = FontWeight.Medium,
        color = if (MaterialTheme.colors.isLight) black else white)
}

@Composable
private fun ActionButton(
    state: ScheduledMeetingInfoState,
    action: ScheduledMeetingInfoAction,
    scheduledMeeting: ScheduledMeetingItem,
    onButtonClicked: (ScheduledMeetingInfoAction) -> Unit = {},
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable {
            onButtonClicked(action)
        }) {
        when (action) {
            ScheduledMeetingInfoAction.ShareMeetingLink -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(modifier = Modifier.padding(start = 72.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 16.dp),
                        style = MaterialTheme.typography.button,
                        text = stringResource(id = action.title),
                        color = MaterialTheme.colors.secondary)
                }

                divider(withStartPadding = true)
            }
            ScheduledMeetingInfoAction.EnableEncryptedKeyRotation -> {
                if (scheduledMeeting.isHost) {
                    Text(modifier = Modifier.padding(start = 14.dp,
                        end = 16.dp,
                        top = 18.dp),
                        style = MaterialTheme.typography.button,
                        text = stringResource(id = action.title),
                        color = MaterialTheme.colors.secondary)

                    action.description?.let { description ->
                        Text(modifier = Modifier.padding(start = 14.dp,
                            end = 16.dp,
                            top = 10.dp,
                            bottom = 8.dp),
                            style = MaterialTheme.typography.subtitle2,
                            text = stringResource(id = description),
                            color = if (MaterialTheme.colors.isLight) grey_alpha_054 else white_alpha_054)
                    }

                    divider(withStartPadding = false)
                }
            }
            ScheduledMeetingInfoAction.MeetingLink,
            ScheduledMeetingInfoAction.AllowNonHostAddParticipants,
            ScheduledMeetingInfoAction.ManageChatHistory,
            -> {
                if (scheduledMeeting.isHost) {
                    ActionOption(action = action,
                        isEnabled = if (action == ScheduledMeetingInfoAction.MeetingLink) state.enabledMeetingLinkOption else state.enabledAllowNonHostAddParticipantsOption,
                        hasSwitch = action != ScheduledMeetingInfoAction.ManageChatHistory)
                    divider(withStartPadding = action != ScheduledMeetingInfoAction.ManageChatHistory)
                }
            }
            ScheduledMeetingInfoAction.ChatNotifications -> {
                ActionOption(action = action,
                    isEnabled = state.enabledChatNotificationsOption,
                    hasSwitch = true)
                divider(withStartPadding = true)
            }
            ScheduledMeetingInfoAction.ShareFiles -> {
                ActionOption(action = action, isEnabled = true, hasSwitch = false)
                divider(withStartPadding = scheduledMeeting.isHost)
            }
        }
    }
}

@Composable
private fun AddParticipantsButton(
    state: ScheduledMeetingInfoState,
    onAddParticipantsClicked: () -> Unit,
) {
    if (state.scheduledMeeting.isHost || state.scheduledMeeting.isAllowAddParticipants) {
        Row(modifier = Modifier
            .clickable { onAddParticipantsClicked() }
            .fillMaxWidth()) {
            Row(modifier = Modifier
                .padding(bottom = 18.dp, top = 18.dp)
                .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                    imageVector = ImageVector.vectorResource(id = R.drawable.add_participants),
                    contentDescription = "Add participants Icon",
                    tint = MaterialTheme.colors.secondary)

                Text(modifier = Modifier.padding(end = 16.dp),
                    style = MaterialTheme.typography.button,
                    text = stringResource(id = R.string.add_participants_menu_item),
                    color = MaterialTheme.colors.secondary)
            }
        }
        if (state.participantItemList.isNotEmpty()) {
            divider(withStartPadding = true)
        }
    }
}

@Composable
private fun SeeMoreParticipantsButton(
    onSeeMoreClicked: () -> Unit,
) {
    Row(modifier = Modifier
        .clickable { onSeeMoreClicked() }
        .fillMaxWidth()) {
        Row(modifier = Modifier
            .padding(top = 16.dp, bottom = 24.dp)
            .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_chevron_down),
                contentDescription = "See more Icon",
                tint = MaterialTheme.colors.secondary)

            Text(modifier = Modifier.padding(end = 16.dp),
                style = MaterialTheme.typography.button,
                text = stringResource(id = R.string.meetings_scheduled_meeting_info_see_more_participants_label),
                color = MaterialTheme.colors.secondary)
        }
    }
}

@Composable
private fun LeaveGroupButton(
    onLeaveGroupClicked: () -> Unit,
) {
    divider(withStartPadding = false)
    Row(modifier = Modifier
        .clickable { onLeaveGroupClicked() }
        .padding(top = 36.dp, bottom = 18.dp)
        .fillMaxWidth()
        .wrapContentSize(Alignment.Center),
        verticalAlignment = Alignment.CenterVertically) {
        Text(textAlign = TextAlign.Center,
            style = MaterialTheme.typography.button,
            text = stringResource(id = R.string.meetings_scheduled_meeting_info_leave_group_label),
            color = if (MaterialTheme.colors.isLight) red_600 else red_300)
    }
}

@Composable
fun divider(withStartPadding: Boolean) {
    Divider(
        modifier = if (withStartPadding) Modifier.padding(start = 72.dp,
            end = 0.dp) else Modifier.padding(start = 0.dp, end = 0.dp),
        color = if (MaterialTheme.colors.isLight) grey_alpha_012 else white_alpha_012,
        thickness = 1.dp)
}

@Composable
private fun ActionOption(
    action: ScheduledMeetingInfoAction,
    isEnabled: Boolean,
    hasSwitch: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)) {
        Row(modifier = Modifier
            .weight(1f)) {
            Box(
                modifier = Modifier
                    .clip(RectangleShape)
                    .wrapContentSize(Alignment.Center)

            ) {
                action.icon?.let { icon ->
                    Icon(painter = painterResource(id = icon),
                        contentDescription = "${action.name} icon",
                        tint = if (MaterialTheme.colors.isLight) grey_alpha_054 else white_alpha_054)
                }
            }

            ActionText(actionText = action.title)
        }

        if (hasSwitch) {
            Box(modifier = Modifier
                .wrapContentSize(Alignment.CenterEnd)
                .size(40.dp)) {
                Switch(modifier = Modifier.align(Alignment.Center),
                    checked = isEnabled,
                    onCheckedChange = null,
                    colors = switchColors()
                )
            }

        }
    }
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = colorResource(id = R.color.teal_300_teal_200),
    checkedTrackColor = colorResource(id = R.color.teal_100_teal_200_038),
    uncheckedThumbColor = colorResource(id = R.color.grey_020_grey_100),
    uncheckedTrackColor = colorResource(id = R.color.grey_700_grey_050_038),
)

@Composable
private fun ActionText(actionText: Int) {
    Text(modifier = Modifier
        .padding(start = 32.dp, end = 23.dp),
        style = MaterialTheme.typography.subtitle1,
        text = stringResource(id = actionText),
        color = if (MaterialTheme.colors.isLight) black else white)
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewActionButton")
@Composable
fun PreviewActionButton() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ActionButton(state = ScheduledMeetingInfoState(),
            action = ScheduledMeetingInfoAction.MeetingLink,
            scheduledMeeting = ScheduledMeetingItem(),
            onButtonClicked = {})
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkAddParticipantsButton")
@Composable
fun PreviewAddParticipantsButton() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        AddParticipantsButton(state = ScheduledMeetingInfoState(), onAddParticipantsClicked = {})
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewScheduledMeetingInfoView")
@Composable
fun PreviewScheduledMeetingInfoView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ScheduledMeetingInfoView(
            state = ScheduledMeetingInfoState(),
            onButtonClicked = {},
            onEditClicked = {},
            onAddParticipantsClicked = {},
            onSeeMoreClicked = {},
            onLeaveGroupClicked = {},
            onParticipantClicked = {},
            onBackPressed = {},
            onScrollChange = {},
        )
    }
}