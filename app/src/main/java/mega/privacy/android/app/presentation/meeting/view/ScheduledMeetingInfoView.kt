package mega.privacy.android.app.presentation.meeting.view

import android.content.res.Configuration
import androidx.compose.foundation.Image
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
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.graphics.toColorInt
import coil.compose.rememberAsyncImagePainter
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.extensions.icon
import mega.privacy.android.app.presentation.extensions.text
import mega.privacy.android.app.presentation.extensions.title
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoAction
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ScheduledMeetingItem
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.presentation.controls.MarqueeText
import mega.privacy.android.presentation.theme.AndroidTheme
import mega.privacy.android.presentation.theme.black
import mega.privacy.android.presentation.theme.grey_alpha_012
import mega.privacy.android.presentation.theme.grey_alpha_038
import mega.privacy.android.presentation.theme.grey_alpha_054
import mega.privacy.android.presentation.theme.red_300
import mega.privacy.android.presentation.theme.red_600
import mega.privacy.android.presentation.theme.white
import mega.privacy.android.presentation.theme.white_alpha_012
import mega.privacy.android.presentation.theme.white_alpha_038
import mega.privacy.android.presentation.theme.white_alpha_054
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Scheduled meeting info View
 */
@Composable
fun ScheduledMeetingInfoView(
    state: ScheduledMeetingInfoState,
    onButtonClicked: (ScheduledMeetingInfoAction) -> Unit = {},
    onEditClicked: () -> Unit,
    onAddParticipantsClicked: () -> Unit,
    onSeeMoreClicked: () -> Unit,
    onLeaveGroupClicked: () -> Unit,
    onParticipantClicked: (ChatParticipant) -> Unit,
    onScrollChange: (Boolean) -> Unit,
    onBackPressed: () -> Unit,
) {
    val listState = rememberLazyListState()
    val firstItemVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val snackbarHostState = remember { SnackbarHostState() }
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
            item(key = "Scheduled meeting title") { ScheduledMeetingTitleView(state = state) }

            state.apply {
                items(buttons) { button ->
                    ActionButton(state = state, action = button, onButtonClicked = onButtonClicked)
                }

                item(key = "Participants") { ParticipantsHeader(state = state) }

                item(key = "Add participants") {
                    AddParticipantsButton(state = state,
                        onAddParticipantsClicked = onAddParticipantsClicked)
                }
                val participantsList = state.participantItemList

                if (state.seeMoreVisible && state.participantItemList.size > 4) {
                    item(key = participantsList[0].participantId) {
                        ParticipantItemView(participant = participantsList[0]) {
                            onParticipantClicked(participantsList[0])
                        }
                    }
                    item(key = participantsList[1].participantId) {
                        ParticipantItemView(participant = participantsList[1]) {
                            onParticipantClicked(participantsList[1])
                        }
                    }
                    item(key = participantsList[2].participantId) {
                        ParticipantItemView(participant = participantsList[2]) {
                            onParticipantClicked(participantsList[2])
                        }
                    }
                    item(key = participantsList[3].participantId) {
                        ParticipantItemView(participant = participantsList[3]) {
                            onParticipantClicked(participantsList[3])
                        }
                    }

                    item(key = "See more participants") {
                        SeeMoreParticipantsButton(onSeeMoreClicked = onSeeMoreClicked)
                    }
                } else {
                    participantsList.forEach { participant ->
                        item(key = participant.participantId) {
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

        state.snackBar?.let { id ->
            val msg = stringResource(id = id)
            LaunchedEffect(scaffoldState.snackbarHostState) {
                scaffoldState.snackbarHostState.showSnackbar(message = msg,
                    duration = SnackbarDuration.Long)
            }
        }
    }

    SnackbarHost(modifier = Modifier.padding(8.dp), hostState = snackbarHostState)

    onScrollChange(!firstItemVisible)
}

/**
 * Scheduled meeting info App bar view
 *
 * @param state                     [ScheduledMeetingInfoState]
 * @param onEditClicked             When edit option is clicked
 * @param onAddParticipantsClicked  When add participants option is clicked
 * @param onBackPressed             When on back pressed option is clicked
 * @param titleId                   Title id
 * @param elevation                 True if it has elevation. False, if it does not.
 */
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
            if (state.isHost || state.isOpenInvite) {
                IconButton(onClick = { onAddParticipantsClicked() }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.add_participants),
                        contentDescription = "Add participants Icon",
                        tint = iconColor
                    )
                }
            }

            if (state.isHost) {
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

/**
 * Scheduled meeting info title view
 *
 * @param state [ScheduledMeetingInfoState]
 */
@Composable
private fun ScheduledMeetingTitleView(state: ScheduledMeetingInfoState) {
    Column {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .size(40.dp)
                .background(Color.Transparent)) {
                MeetingAvatar(state = state)
            }
            Column(modifier = Modifier
                .padding(start = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    state.scheduledMeeting?.let {
                        it.title?.let { title ->
                            Text(text = title,
                                style = MaterialTheme.typography.subtitle1,
                                color = if (MaterialTheme.colors.isLight) black else white,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                state.scheduledMeeting?.let {
                    it.date?.let { date ->
                        Text(text = date,
                            style = MaterialTheme.typography.subtitle2,
                            color = if (MaterialTheme.colors.isLight) grey_alpha_054 else white_alpha_054,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        divider(withStartPadding = false)
    }
}

/**
 * Create meeting avatar view
 *
 * @param state [ScheduledMeetingInfoState]
 */
@Composable
private fun MeetingAvatar(state: ScheduledMeetingInfoState) {
    if (state.isEmptyMeeting()) {
        DefaultAvatar(title = state.chatTitle)
    } else if (state.isSingleMeeting()) {
        OneParticipantAvatar(firstUser = state.firstParticipant ?: return)
    } else {
        SeveralParticipantsAvatar(firstUser = state.lastParticipant ?: return,
            lastUser = state.lastParticipant)
    }
}

/**
 * Create default avatar of a meeting
 *
 * @param title Title of the meeting
 */
@Composable
private fun DefaultAvatar(title: String) {
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
            text = AvatarUtil.getFirstLetter(title),
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.h6
        )
    }
}

/**
 * Meeting avatar with one participant view
 *
 * @param firstUser [ChatParticipant]
 */
@Composable
fun OneParticipantAvatar(firstUser: ChatParticipant) {
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(color = Color(AvatarUtil.getColorAvatar(firstUser.participantId)),
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
        firstUser.contact?.let { contact ->
            Text(
                text = AvatarUtil.getFirstLetter(contact.contactData.fullName),
                textAlign = TextAlign.Center,
                color = Color.White,
                style = MaterialTheme.typography.h6
            )
        }

    }
}

/**
 * Meeting avatar with several participants view
 *
 * @param firstUser [ChatParticipant]
 * @param lastUser  [ChatParticipant]
 */
@Composable
fun SeveralParticipantsAvatar(
    firstUser: ChatParticipant,
    lastUser: ChatParticipant,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        lastUser.contact?.let { lastUser ->
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .border(width = 1.dp, color = Color.White, shape = CircleShape)
                    .clip(CircleShape)
                    .align(Alignment.BottomEnd)
                    .background(color = Color(AvatarUtil.getSpecificAvatarColor(lastUser.defaultAvatarColor)),
                        shape = CircleShape)
            ) {
                Text(
                    text = AvatarUtil.getFirstLetter(lastUser.contactData.fullName),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    style = MaterialTheme.typography.subtitle1
                )
            }
        }

        firstUser.contact?.let { firstUser ->
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .border(width = 1.dp, color = Color.White, shape = CircleShape)
                    .clip(CircleShape)
                    .align(Alignment.TopStart)
                    .background(color = Color(AvatarUtil.getSpecificAvatarColor(firstUser.defaultAvatarColor)),
                        shape = CircleShape)
            ) {
                Text(
                    text = AvatarUtil.getFirstLetter(firstUser.contactData.fullName),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    style = MaterialTheme.typography.subtitle1
                )
            }
        }
    }
}

/**
 * Control and show the available buttons
 *
 * @param state             [ScheduledMeetingInfoState]
 * @param action            [ScheduledMeetingInfoAction]
 * @param onButtonClicked
 */
@Composable
private fun ActionButton(
    state: ScheduledMeetingInfoState,
    action: ScheduledMeetingInfoAction,
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
                if (state.isHost) {
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
                if (state.isHost) {
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
                divider(withStartPadding = state.isHost)
            }
        }
    }
}

/**
 * Participants header view
 *
 * @param state [ScheduledMeetingInfoState]
 */
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

/**
 * Add participants button view
 *
 * @param state [ScheduledMeetingInfoState]
 * @param onAddParticipantsClicked
 */
@Composable
private fun AddParticipantsButton(
    state: ScheduledMeetingInfoState,
    onAddParticipantsClicked: () -> Unit,
) {
    if (state.isHost || state.isOpenInvite) {
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

/**
 * See more participants in the list button view
 *
 * @param onSeeMoreClicked
 */
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

/**
 * Leave group button view
 *
 * @param onLeaveGroupClicked
 */
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

/**
 * Divider options view
 *
 * @param withStartPadding True, if has start padding. False, if not
 */
@Composable
fun divider(withStartPadding: Boolean) {
    Divider(
        modifier = if (withStartPadding) Modifier.padding(start = 72.dp,
            end = 0.dp) else Modifier.padding(start = 0.dp, end = 0.dp),
        color = if (MaterialTheme.colors.isLight) grey_alpha_012 else white_alpha_012,
        thickness = 1.dp)
}

/**
 * Show action buttons options
 *
 * @param action        [ScheduledMeetingInfoAction]
 * @param isEnabled     True, if the option must be enabled. False if not
 * @param hasSwitch     True, if the option has a switch. False if not
 */
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

/**
 * Control the colours of the switch depending on the status
 */
@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = colorResource(id = R.color.teal_300_teal_200),
    checkedTrackColor = colorResource(id = R.color.teal_100_teal_200_038),
    uncheckedThumbColor = colorResource(id = R.color.grey_020_grey_100),
    uncheckedTrackColor = colorResource(id = R.color.grey_700_grey_050_038),
)

/**
 * Text of the available options
 *
 * @param actionText Title of the option
 */
@Composable
private fun ActionText(actionText: Int) {
    Text(modifier = Modifier
        .padding(start = 32.dp, end = 23.dp),
        style = MaterialTheme.typography.subtitle1,
        text = stringResource(id = actionText),
        color = if (MaterialTheme.colors.isLight) black else white)
}

/**
 * View of a participant in the list
 *
 * @param participant   [ContactItem]
 * @param onClick       Detect when a participant is clicked
 */
@Composable
private fun ParticipantItemView(participant: ChatParticipant, onClick: () -> Unit) {
    Column {
        Row(modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier
                .weight(1f)) {
                Box {
                    ParticipantAvatar(participant = participant)
                }
                participant.contact?.let { contact ->
                    Column(modifier = Modifier
                        .align(Alignment.CenterVertically)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val contactName =
                                contact.contactData.alias ?: contact.contactData.fullName
                                ?: contact.email

                            Text(text = contactName,
                                style = MaterialTheme.typography.subtitle1,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis)

                            if (contact.status != UserStatus.Invalid) {
                                ContactStatus(status = contact.status)
                            }
                        }

                        if (contact.lastSeen != null || contact.status != UserStatus.Invalid) {
                            val statusText = stringResource(id = contact.status.text)
                            val secondLineText =
                                if (contact.status == UserStatus.Online) {
                                    statusText
                                } else {
                                    getLastSeenString(contact.lastSeen) ?: statusText
                                }

                            MarqueeText(text = secondLineText,
                                color = if (MaterialTheme.colors.isLight) grey_alpha_054 else white_alpha_054,
                                style = MaterialTheme.typography.subtitle2)
                        }
                    }
                }
            }

            Box(modifier = Modifier
                .wrapContentSize(Alignment.CenterEnd)) {
                Row(modifier = Modifier.align(Alignment.Center)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_permissions_full_access),
                        contentDescription = "Permissions icon",
                        tint = if (MaterialTheme.colors.isLight) grey_alpha_038 else white_alpha_038)

                    Icon(modifier = Modifier.padding(start = 30.dp),
                        painter = painterResource(id = R.drawable.ic_dots_vertical_grey),
                        contentDescription = "Three dots icon",
                        tint = if (MaterialTheme.colors.isLight) grey_alpha_038 else white_alpha_038)
                }
            }
        }

        Divider(modifier = Modifier.padding(start = 72.dp),
            color = if (MaterialTheme.colors.isLight) grey_alpha_012 else white_alpha_012,
            thickness = 1.dp)
    }
}

/**
 * Last seen text
 *
 * @param lastGreen     User last seen.
 * @return              Text with the info of last seen of a participant
 */
@Composable
private fun getLastSeenString(lastGreen: Int?): String? {
    if (lastGreen == null) return null

    val lastGreenCalendar = Calendar.getInstance().apply { add(Calendar.MINUTE, -lastGreen) }
    val timeToConsiderAsLongTimeAgo = 65535

    Timber.d("Ts last green: %s", lastGreenCalendar.timeInMillis)

    return when {
        lastGreen >= timeToConsiderAsLongTimeAgo -> {
            stringResource(id = R.string.last_seen_long_time_ago)
        }
        compareLastSeenWithToday(lastGreenCalendar) == 0 -> {
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = lastGreenCalendar.timeZone
            }
            val time = dateFormat.format(lastGreenCalendar.time)
            stringResource(R.string.last_seen_today, time)
        }
        else -> {
            var dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = lastGreenCalendar.timeZone
            }
            val time = dateFormat.format(lastGreenCalendar.time)
            dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            val day = dateFormat.format(lastGreenCalendar.time)
            stringResource(R.string.last_seen_general, day, time)
        }
    }.replace("[A]", "").replace("[/A]", "")
}

/**
 * Compare last seen with today
 *
 * @param lastGreen     Calendar with last green
 * @return              User last seen.
 */
private fun compareLastSeenWithToday(lastGreen: Calendar): Int {
    val today = Calendar.getInstance()

    return when {
        lastGreen.get(Calendar.YEAR) != today.get(Calendar.YEAR) -> {
            lastGreen.get(Calendar.YEAR) - today.get(Calendar.YEAR)
        }
        lastGreen.get(Calendar.MONTH) != today.get(Calendar.MONTH) -> {
            lastGreen.get(Calendar.MONTH) - today.get(Calendar.MONTH)
        }
        else -> {
            lastGreen.get(Calendar.DAY_OF_MONTH) - today.get(Calendar.DAY_OF_MONTH)
        }
    }
}

/**
 * Contact status view
 *
 * @param status [UserStatus]
 */
@Composable
private fun ContactStatus(
    modifier: Modifier = Modifier.padding(start = 5.dp, top = 2.dp),
    status: UserStatus,
) {
    val isLightTheme = MaterialTheme.colors.isLight
    val statusIcon = when (status) {
        UserStatus.Online ->
            if (isLightTheme) R.drawable.ic_online_light
            else R.drawable.ic_online_dark_standard
        UserStatus.Away ->
            if (isLightTheme) R.drawable.ic_away_light
            else R.drawable.ic_away_dark_standard
        UserStatus.Busy ->
            if (isLightTheme) R.drawable.ic_busy_light
            else R.drawable.ic_busy_dark_standard
        else ->
            if (isLightTheme) R.drawable.ic_offline_light
            else R.drawable.ic_offline_dark_standard
    }

    Image(modifier = modifier,
        painter = painterResource(id = statusIcon),
        contentDescription = "Contact status")
}

/**
 * Participant avatar
 *
 * @param participant [ChatParticipant]
 */
@Composable
private fun ParticipantAvatar(
    modifier: Modifier = Modifier
        .padding(16.dp)
        .size(40.dp)
        .clip(CircleShape),
    participant: ChatParticipant,
) {
    participant.contact?.let { contact ->
        val avatarUri = contact.contactData.avatarUri

        if (avatarUri != null) {
            UriAvatar(modifier = modifier, uri = avatarUri)
        } else {
            DefaultParticipantAvatar(modifier = modifier,
                color = Color(contact.defaultAvatarColor.toColorInt()),
                content = contact.getAvatarFirstLetter())
        }
    }
}

/**
 * Show avatar image view
 *
 * @param uri Uri
 */
@Composable
private fun UriAvatar(modifier: Modifier, uri: String) {
    Image(modifier = modifier,
        painter = rememberAsyncImagePainter(model = uri),
        contentDescription = "User avatar")
}

/**
 * Default participant avatar
 *
 * @param color     Avatar color
 * @param content   First letter
 */
@Composable
fun DefaultParticipantAvatar(
    modifier: Modifier = Modifier.size(40.dp),
    color: Color,
    content: String,
) {
    Box(contentAlignment = Alignment.Center,
        modifier = modifier
            .background(color = color, shape = CircleShape)
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
            text = content,
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.h6
        )
    }
}

/**
 * Meeting link action button View Preview
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewActionButton")
@Composable
fun PreviewActionButton() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ActionButton(state = ScheduledMeetingInfoState(
            scheduledMeeting = ScheduledMeetingItem(chatId = -1,
                scheduledMeetingId = -1,
                date = "date",
                description = "description")),
            action = ScheduledMeetingInfoAction.MeetingLink,
            onButtonClicked = {})
    }
}

/**
 * Add participants button View Preview
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkAddParticipantsButton")
@Composable
fun PreviewAddParticipantsButton() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        AddParticipantsButton(state = ScheduledMeetingInfoState(
            scheduledMeeting = ScheduledMeetingItem(chatId = -1,
                scheduledMeetingId = -1,
                date = "date",
                description = "description")), onAddParticipantsClicked = {})
    }
}

/**
 * Scheduled meeting info View Preview
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewScheduledMeetingInfoView")
@Composable
fun PreviewScheduledMeetingInfoView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ScheduledMeetingInfoView(
            state = ScheduledMeetingInfoState(
                scheduledMeeting = ScheduledMeetingItem(chatId = -1,
                    scheduledMeetingId = -1,
                    date = "date",
                    description = "description")),
            onButtonClicked = {},
            onEditClicked = {},
            onAddParticipantsClicked = {},
            onSeeMoreClicked = {},
            onLeaveGroupClicked = {},
            onParticipantClicked = {},
            onScrollChange = {},
            onBackPressed = {},
        )
    }
}