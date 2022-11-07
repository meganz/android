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
import mega.privacy.android.app.contacts.group.data.ContactGroupUser
import mega.privacy.android.app.meeting.list.adapter.ScheduledMeetingItem
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.extensions.icon
import mega.privacy.android.app.presentation.extensions.title
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoAction
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.presentation.theme.AndroidTheme
import mega.privacy.android.presentation.theme.black
import mega.privacy.android.presentation.theme.grey_alpha_012
import mega.privacy.android.presentation.theme.grey_alpha_033
import mega.privacy.android.presentation.theme.grey_alpha_054
import mega.privacy.android.presentation.theme.white
import mega.privacy.android.presentation.theme.white_alpha_012
import mega.privacy.android.presentation.theme.white_alpha_054

@Composable
fun ScheduledMeetingInfoView(
    state: ScheduledMeetingInfoState,
    onButtonClicked: (ScheduledMeetingInfoAction) -> Unit = {},
    onEditClicked: () -> Unit,
    onAddParticipantsClicked: () -> Unit,
    onBackPressed: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            ScheduledMeetingInfoAppBar(
                uiState = state,
                onEditClicked = onEditClicked,
                onAddParticipantsClicked = onAddParticipantsClicked,
                onBackPressed = onBackPressed,
                elevation = false,
                titleId = R.string.general_info
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

                val contactsList = contactItemList

                when {
                    contactsList.isNotEmpty() -> {
                        item(key = "Participants") { ParticipantsHeader() }

                        /* val defaultAvatarContent = contactsList[0].getAvatarFirstLetter()

                         header = defaultAvatarContent

                         item(key = contactsList[0].handle.hashCode()) {
                             HeaderItem(text = defaultAvatarContent)
                         }*/
                    }
                }

                /*contactsList.forEach { contact ->
                    val defaultAvatarContent = contact.getAvatarFirstLetter()

                    if (header != defaultAvatarContent) {
                        header = defaultAvatarContent

                        item(key = contact.handle.hashCode()) {
                            HeaderItem(text = defaultAvatarContent)
                        }
                    }

                    item(key = contact.handle) { ContactItemView(contact) { onContactClicked(contact) } }
                }*/
            }
        }
    }
}

@Composable
fun ScheduledMeetingInfoAppBar(
    uiState: ScheduledMeetingInfoState,
    onEditClicked: () -> Unit,
    onAddParticipantsClicked: () -> Unit,
    onBackPressed: () -> Unit,
    elevation: Boolean,
    titleId: Int,
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
            if (uiState.scheduledMeeting.isHost || uiState.scheduledMeeting.isAllowAddParticipants) {
                IconButton(onClick = { onAddParticipantsClicked() }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.add_participants),
                        contentDescription = "Add participants Icon",
                        tint = iconColor
                    )
                }
            }

            if (uiState.scheduledMeeting.isHost) {
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
                    color = grey_alpha_033,
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
fun OneParticipantAvatar(firstUser: ContactGroupUser) {
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(color = Color(AvatarUtil.getColorAvatar(firstUser.handle)),
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
            text = AvatarUtil.getFirstLetter(firstUser.firstName),
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.h6
        )
    }
}

@Composable
fun SeveralParticipantsAvatar(
    firstUser: ContactGroupUser,
    lastUser: ContactGroupUser,
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
                .background(color = Color(lastUser.avatarColor),
                    shape = CircleShape)
        ) {
            Text(
                text = AvatarUtil.getFirstLetter(lastUser.firstName),
                textAlign = TextAlign.Center,
                color = Color.White,
                style = MaterialTheme.typography.subtitle1
            )
        }
        Box(contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .border(width = 2.dp, color = Color.White, shape = CircleShape)
                .clip(CircleShape)
                .align(Alignment.TopStart)
                .background(color = Color(firstUser.avatarColor),
                    shape = CircleShape)
        ) {
            Text(
                text = AvatarUtil.getFirstLetter(firstUser.firstName),
                textAlign = TextAlign.Center,
                color = Color.White,
                style = MaterialTheme.typography.subtitle1
            )
        }
    }
}

@Composable
private fun ParticipantsHeader() {
    Text(modifier = Modifier.padding(start = 16.dp,
        top = 16.dp,
        end = 16.dp,
        bottom = 8.dp),
        text = stringResource(id = R.string.participants_chat_label),
        style = MaterialTheme.typography.body2)
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
fun divider(withStartPadding: Boolean) {
    Divider(
        modifier = if (withStartPadding) Modifier.padding(start = 72.dp,
            end = 0.dp) else Modifier.padding(start = 0.dp, end = 0.dp),
        color = if (MaterialTheme.colors.isLight) grey_alpha_012 else white_alpha_012,
        thickness = 1.dp)
}

@Composable
private fun InviteContactsButton(onInviteContactsClicked: () -> Unit) {
    Row(modifier = Modifier
        .clickable { onInviteContactsClicked() }
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(modifier = Modifier.padding(horizontal = 26.dp, vertical = 18.dp),
            painter = painterResource(id = R.drawable.ic_invite_contacts),
            contentDescription = stringResource(id = R.string.invite_contacts) + "icon",
            tint = MaterialTheme.colors.secondary)

        ActionText(actionText = R.string.invite_contacts)
    }
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
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkInviteContactsButton")
@Composable
fun PreviewInviteContactsButton() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        InviteContactsButton(onInviteContactsClicked = {})
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
            onBackPressed = {}
        )
    }
}