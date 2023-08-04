package mega.privacy.android.app.presentation.meeting.view

import mega.privacy.android.core.R as CoreUiR
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.list.view.ChatAvatarView
import mega.privacy.android.app.presentation.contact.view.ContactStatus
import mega.privacy.android.app.presentation.contact.view.getLastSeenString
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.extensions.icon
import mega.privacy.android.app.presentation.extensions.isPast
import mega.privacy.android.app.presentation.extensions.text
import mega.privacy.android.app.presentation.extensions.title
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoAction
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingManagementState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.controls.divider.CustomDivider
import mega.privacy.android.core.ui.controls.text.MarqueeText
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.grey_alpha_038
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.red_300
import mega.privacy.android.core.ui.theme.red_600
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_012
import mega.privacy.android.core.ui.theme.white_alpha_038
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.contacts.UserStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Scheduled meeting info View
 */
@Composable
fun ScheduledMeetingInfoView(
    state: ScheduledMeetingInfoState,
    managementState: ScheduledMeetingManagementState,
    onButtonClicked: (ScheduledMeetingInfoAction) -> Unit = {},
    onEditClicked: () -> Unit,
    onAddParticipantsClicked: () -> Unit,
    onSeeMoreOrLessClicked: () -> Unit,
    onLeaveGroupClicked: () -> Unit,
    onParticipantClicked: (ChatParticipant) -> Unit = {},
    onScrollChange: (Boolean) -> Unit,
    onBackPressed: () -> Unit,
    onDismiss: () -> Unit,
    onLeaveGroupDialog: () -> Unit,
    onInviteParticipantsDialog: () -> Unit,
    onSnackbarShown: () -> Unit,
    onLearnMoreWarningClicked: () -> Unit,
    onCloseWarningClicked: () -> Unit,
) {
    val listState = rememberLazyListState()
    val firstItemVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val snackbarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(hostState = it) { data ->
                Snackbar(
                    snackbarData = data,
                    backgroundColor = MaterialTheme.colors.onPrimary
                )
            }
        },
        topBar = {
            ScheduledMeetingInfoAppBar(
                state = state,
                onEditClicked = onEditClicked,
                onAddParticipantsClicked = onAddParticipantsClicked,
                onBackPressed = onBackPressed,
                titleId = R.string.general_info,
                elevation = !firstItemVisible,
            )
        }
    ) { paddingValues ->
        LeaveGroupAlertDialog(
            state = state,
            onDismiss = { onDismiss() },
            onLeave = { onLeaveGroupDialog() })

        AddParticipantsAlertDialog(
            state = state,
            onDismiss = { onDismiss() },
            onInvite = { onInviteParticipantsDialog() })

        Column {
            if (state.enabledAllowNonHostAddParticipantsOption && managementState.enabledWaitingRoomOption && !managementState.isWarningClosed && managementState.isWaitingRoomFeatureFlagEnabled) {
                WaitingRoomWarningDialog(
                    onLearnMoreClicked = onLearnMoreWarningClicked,
                    onCloseClicked = onCloseWarningClicked
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.padding(paddingValues)
            ) {
                item(key = "Scheduled meeting title") {
                    ScheduledMeetingTitleView(state = state)
                }

                items(state.buttons) { button ->
                    ActionButton(
                        state = state,
                        action = button,
                        isWaitingRoomFeatureFlagEnabled = managementState.isWaitingRoomFeatureFlagEnabled,
                        enabledMeetingLinkOption = managementState.enabledMeetingLinkOption,
                        enabledWaitingRoomOption = managementState.enabledWaitingRoomOption,
                        onButtonClicked = onButtonClicked
                    )
                }

                item(key = "Participants") { ParticipantsHeader(state = state) }

                item(key = "Add participants") {
                    AddParticipantsButton(
                        state = state,
                        onAddParticipantsClicked = onAddParticipantsClicked
                    )
                }

                item(key = "Participants list") {
                    state.participantItemList.indices.forEach { i ->
                        if (i < 4 || !state.seeMoreVisible) {
                            val isLastOne =
                                state.participantItemList.size <= 4 && i == state.participantItemList.size - 1

                            ParticipantItemView(
                                participant = state.participantItemList[i],
                                !isLastOne, onParticipantClicked = onParticipantClicked
                            )
                        }
                    }

                    if (state.participantItemList.size > 4) {
                        SeeMoreOrLessParticipantsButton(
                            state,
                            onSeeMoreOrLessClicked = onSeeMoreOrLessClicked
                        )
                    }
                }

                item(key = "Scheduled meeting description") {
                    ScheduledMeetingDescriptionView(state = state)
                }

                item(key = "Leave group") {
                    LeaveGroupButton(onLeaveGroupClicked = onLeaveGroupClicked)
                }
            }
        }


        if (state.snackBar != null) {
            val msg =
                if (state.snackBar == R.string.context_contact_request_sent && state.selected != null) {
                    stringResource(id = state.snackBar, state.selected.email)
                } else if (state.snackBar == R.string.invite_not_sent_already_sent && state.selected != null) {
                    stringResource(id = state.snackBar, state.selected.email)
                } else if (state.snackBar == R.string.context_contact_already_exists && state.selected != null) {
                    stringResource(id = state.snackBar, state.selected.email)
                } else {
                    stringResource(id = state.snackBar)
                }

            LaunchedEffect(scaffoldState.snackbarHostState) {
                val s = scaffoldState.snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )

                if (s == SnackbarResult.Dismissed) {
                    onSnackbarShown()
                }
            }
        }
    }

    SnackbarHost(modifier = Modifier.padding(8.dp), hostState = snackbarHostState)

    onScrollChange(!firstItemVisible)
}

/**
 * Scheduled meeting info Alert Dialog
 *
 * @param state                     [ScheduledMeetingInfoState]
 * @param onDismiss                 When dismiss the alert dialog
 * @param onLeave                   When leave the group chat room
 */
@Composable
private fun LeaveGroupAlertDialog(
    state: ScheduledMeetingInfoState,
    onDismiss: () -> Unit,
    onLeave: () -> Unit,
) {
    if (state.leaveGroupDialog) {
        MegaAlertDialog(
            title = stringResource(id = R.string.meetings_leave_meeting_confirmation_dialog_title),
            text = stringResource(id = R.string.confirmation_leave_group_chat),
            confirmButtonText = stringResource(id = R.string.general_leave),
            cancelButtonText = stringResource(id = R.string.general_cancel),
            onConfirm = onLeave,
            onDismiss = onDismiss,
        )
    }
}

/**
 * Scheduled meeting info Alert Dialog
 *
 * @param state                     [ScheduledMeetingInfoState]
 * @param onDismiss                 When dismiss the alert dialog
 * @param onInvite                  When invite participants to group chat room
 */
@Composable
private fun AddParticipantsAlertDialog(
    state: ScheduledMeetingInfoState,
    onDismiss: () -> Unit,
    onInvite: () -> Unit,
) {

    if (state.addParticipantsNoContactsDialog || state.addParticipantsNoContactsLeftToAddDialog) {
        MegaAlertDialog(
            title = stringResource(
                id = if (state.addParticipantsNoContactsDialog)
                    R.string.chat_add_participants_no_contacts_title
                else
                    R.string.chat_add_participants_no_contacts_left_to_add_title
            ),
            text = stringResource(
                id = if (state.addParticipantsNoContactsDialog)
                    R.string.chat_add_participants_no_contacts_message
                else
                    R.string.chat_add_participants_no_contacts_left_to_add_message
            ),
            confirmButtonText = stringResource(id = R.string.contact_invite),
            cancelButtonText = stringResource(id = R.string.button_cancel),
            onConfirm = onInvite,
            onDismiss = onDismiss,
        )
    }
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
    val iconColor = black.takeIf { isLight() } ?: white

    TopAppBar(
        title = {
            Text(
                text = stringResource(id = titleId),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back button",
                    tint = iconColor
                )
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
            state.scheduledMeeting?.let { schedMeet ->
                if (state.isHost && !schedMeet.isPast()) {
                    IconButton(onClick = { onEditClicked() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_scheduled_meeting_edit),
                            contentDescription = "Edit Icon",
                            tint = iconColor
                        )
                    }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Transparent)
            ) {
                MeetingAvatar(state = state)
            }
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    state.scheduledMeeting?.let {
                        it.title?.let { title ->
                            Text(text = title,
                                style = MaterialTheme.typography.subtitle1,
                                color = black.takeIf { isLight() } ?: white,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                ScheduledMeetingSubtitle(state = state)
            }
        }

        CustomDivider(withStartPadding = false)
    }
}

@Composable
private fun isLight(): Boolean = MaterialTheme.colors.isLight


/**
 * Scheduled meeting subtitle
 *
 * @param state [ScheduledMeetingInfoState]
 */
@Composable
private fun ScheduledMeetingSubtitle(state: ScheduledMeetingInfoState) {
    state.scheduledMeeting?.let { schedMeet ->
        if (schedMeet.isPast()) {
            Text(text = pluralStringResource(
                R.plurals.subtitle_of_group_chat,
                state.numOfParticipants,
                state.numOfParticipants
            ),
                style = MaterialTheme.typography.body1,
                color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis)
        } else {
            val text = getRecurringMeetingDateTime(schedMeet, state.is24HourFormat)
            if (text.isNotEmpty()) {
                Text(text = text,
                    style = MaterialTheme.typography.subtitle2,
                    color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp)
            }
        }
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
        ChatAvatarView(
            avatarUri = null,
            avatarPlaceholder = state.chatTitle,
            avatarColor = null,
            modifier = Modifier.border(1.dp, Color.White, CircleShape)
        )
    } else if (state.isSingleMeeting()) {
        state.firstParticipant?.let { participant ->
            ChatAvatarView(
                avatarUri = participant.data.avatarUri,
                avatarPlaceholder = participant.getAvatarFirstLetter(),
                avatarColor = participant.defaultAvatarColor,
                avatarTimestamp = participant.avatarUpdateTimestamp,
                modifier = Modifier.border(1.dp, Color.White, CircleShape),
            )
        }
    } else if (state.firstParticipant != null && state.secondParticipant != null) {
        Box(
            Modifier.fillMaxSize()
        ) {
            ChatAvatarView(
                avatarUri = state.secondParticipant.data.avatarUri,
                avatarPlaceholder = state.secondParticipant.getAvatarFirstLetter(),
                avatarColor = state.secondParticipant.defaultAvatarColor,
                avatarTimestamp = state.secondParticipant.avatarUpdateTimestamp,
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.BottomEnd)
                    .border(1.dp, Color.White, CircleShape)
            )
            ChatAvatarView(
                avatarUri = state.firstParticipant.data.avatarUri,
                avatarPlaceholder = state.firstParticipant.getAvatarFirstLetter(),
                avatarColor = state.firstParticipant.defaultAvatarColor,
                avatarTimestamp = state.firstParticipant.avatarUpdateTimestamp,
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.TopStart)
                    .border(1.dp, Color.White, CircleShape)
            )
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
    isWaitingRoomFeatureFlagEnabled: Boolean,
    enabledMeetingLinkOption: Boolean,
    enabledWaitingRoomOption: Boolean,
    action: ScheduledMeetingInfoAction,
    onButtonClicked: (ScheduledMeetingInfoAction) -> Unit = {},
) {
    Column(modifier = Modifier
        .testTag(ACTION_BUTTON_OPTION_TAG)
        .fillMaxWidth()
        .clickable {
            if (action != ScheduledMeetingInfoAction.EnabledEncryptedKeyRotation) {
                onButtonClicked(action)
            }
        }) {
        when (action) {
            ScheduledMeetingInfoAction.ShareMeetingLink,
            ScheduledMeetingInfoAction.ShareMeetingLinkNonHosts,
            -> {
                if (state.isPublic && enabledMeetingLinkOption) {
                    if (action == ScheduledMeetingInfoAction.ShareMeetingLink && state.isHost) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                modifier = Modifier.padding(
                                    start = 72.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = 16.dp
                                ),
                                style = MaterialTheme.typography.button,
                                text = stringResource(id = action.title),
                                color = MaterialTheme.colors.secondary
                            )
                        }
                        CustomDivider(withStartPadding = true)
                    } else if (action == ScheduledMeetingInfoAction.ShareMeetingLinkNonHosts && !state.isHost) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ActionOption(
                                state = state,
                                action = action,
                                isEnabled = true,
                                hasSwitch = false
                            )
                        }
                        CustomDivider(withStartPadding = false)
                    }
                }
            }

            ScheduledMeetingInfoAction.EnableEncryptedKeyRotation ->
                if (state.isHost && state.isPublic) {
                    Text(
                        modifier = Modifier.padding(
                            start = 14.dp,
                            end = 16.dp,
                            top = 18.dp
                        ),
                        style = MaterialTheme.typography.button,
                        text = stringResource(id = action.title),
                        color = MaterialTheme.colors.secondary
                    )

                    action.description?.let { description ->
                        Text(modifier = Modifier.padding(
                            start = 14.dp,
                            end = 16.dp,
                            top = 10.dp,
                            bottom = 8.dp
                        ),
                            style = MaterialTheme.typography.subtitle2,
                            text = stringResource(id = description),
                            color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054)
                    }

                    CustomDivider(withStartPadding = false)
                }

            ScheduledMeetingInfoAction.EnabledEncryptedKeyRotation,
            -> if (state.isHost && !state.isPublic) {
                Text(modifier = Modifier.padding(
                    start = 14.dp,
                    end = 16.dp,
                    top = 18.dp
                ),
                    style = MaterialTheme.typography.subtitle1,
                    text = stringResource(id = action.title),
                    color = black.takeIf { isLight() } ?: white)

                action.description?.let { description ->
                    Text(modifier = Modifier.padding(
                        start = 14.dp,
                        end = 16.dp,
                        top = 10.dp,
                        bottom = 8.dp
                    ),
                        style = MaterialTheme.typography.subtitle2,
                        text = stringResource(id = description),
                        color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054)
                }

                CustomDivider(withStartPadding = false)
            }

            ScheduledMeetingInfoAction.MeetingLink,
            -> if (state.isHost && state.isPublic) {
                ActionOption(
                    state = state,
                    action = action,
                    isEnabled = enabledMeetingLinkOption,
                    hasSwitch = true
                )
                CustomDivider(withStartPadding = true)
            }

            ScheduledMeetingInfoAction.AllowNonHostAddParticipants ->
                if (state.isHost) {
                    ActionOption(
                        state = state,
                        action = action,
                        isEnabled = state.enabledAllowNonHostAddParticipantsOption,
                        hasSwitch = true
                    )
                    CustomDivider(withStartPadding = true)
                }

            ScheduledMeetingInfoAction.WaitingRoom -> {
                if (state.isHost && isWaitingRoomFeatureFlagEnabled) {
                    ActionOption(
                        state = state,
                        action = action,
                        isEnabled = enabledWaitingRoomOption,
                        hasSwitch = true
                    )

                    action.description?.let { description ->
                        Text(modifier = Modifier.padding(
                            start = 72.dp,
                            end = 16.dp,
                            top = 2.dp,
                            bottom = 18.dp
                        ),
                            style = MaterialTheme.typography.subtitle2,
                            text = stringResource(id = description),
                            color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054)
                    }
                    CustomDivider(withStartPadding = true)
                }
            }

            ScheduledMeetingInfoAction.ManageChatHistory ->
                if (state.isHost) {
                    ActionOption(
                        state = state,
                        action = action,
                        isEnabled = true,
                        hasSwitch = false
                    )
                    CustomDivider(withStartPadding = true)

                }

            ScheduledMeetingInfoAction.ChatNotifications -> {
                ActionOption(
                    state = state,
                    action = action,
                    isEnabled = state.dndSeconds == null,
                    hasSwitch = true
                )
                CustomDivider(withStartPadding = true)
            }

            ScheduledMeetingInfoAction.ShareFiles -> {
                ActionOption(
                    state = state,
                    action = action,
                    isEnabled = true,
                    hasSwitch = false
                )
                CustomDivider(withStartPadding = true)
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
    Text(modifier = Modifier.padding(
        start = 16.dp,
        top = 17.dp,
        end = 16.dp,
        bottom = 12.dp
    ),
        text = stringResource(id = R.string.participants_number, state.participantItemList.size),
        style = MaterialTheme.typography.body2,
        fontWeight = FontWeight.Medium,
        color = black.takeIf { isLight() } ?: white)
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
            Row(
                modifier = Modifier
                    .padding(bottom = 18.dp, top = 18.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                    imageVector = ImageVector.vectorResource(id = R.drawable.add_participants),
                    contentDescription = "Add participants Icon",
                    tint = MaterialTheme.colors.secondary
                )

                Text(
                    modifier = Modifier.padding(end = 16.dp),
                    style = MaterialTheme.typography.button,
                    text = stringResource(id = R.string.add_participants_menu_item),
                    color = MaterialTheme.colors.secondary
                )
            }
        }
        if (state.participantItemList.isNotEmpty()) {
            CustomDivider(withStartPadding = true)
        }
    }
}

/**
 * See more participants in the list button view
 *
 * @param state [ScheduledMeetingInfoState]
 * @param onSeeMoreOrLessClicked
 */
@Composable
private fun SeeMoreOrLessParticipantsButton(
    state: ScheduledMeetingInfoState,
    onSeeMoreOrLessClicked: () -> Unit,
) {
    Row(modifier = Modifier
        .clickable { onSeeMoreOrLessClicked() }
        .fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                imageVector = ImageVector.vectorResource(id = if (state.seeMoreVisible) CoreUiR.drawable.ic_chevron_down else CoreUiR.drawable.ic_chevron_up),
                contentDescription = "See more Icon",
                tint = MaterialTheme.colors.secondary
            )

            Text(
                modifier = Modifier.padding(end = 16.dp),
                style = MaterialTheme.typography.button,
                text = stringResource(id = if (state.seeMoreVisible) R.string.meetings_scheduled_meeting_info_see_more_participants_label else R.string.meetings_scheduled_meeting_info_see_less_participants_label),
                color = MaterialTheme.colors.secondary
            )
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
    CustomDivider(withStartPadding = false)
    Row(modifier = Modifier
        .clickable { onLeaveGroupClicked() }
        .padding(top = 36.dp, bottom = 18.dp)
        .fillMaxWidth()
        .wrapContentSize(Alignment.Center),
        verticalAlignment = Alignment.CenterVertically) {
        Text(textAlign = TextAlign.Center,
            style = MaterialTheme.typography.button,
            text = stringResource(id = R.string.meetings_scheduled_meeting_info_leave_group_label),
            color = red_600.takeIf { isLight() } ?: red_300)
    }
}

/**
 * Scheduled meeting info description view
 *
 * @param state [ScheduledMeetingInfoState]
 */
@Composable
private fun ScheduledMeetingDescriptionView(state: ScheduledMeetingInfoState) {
    state.scheduledMeeting?.let { schedMeet ->
        schedMeet.description?.let { description ->
            CustomDivider(withStartPadding = false)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 22.dp)
                            .clip(RectangleShape)
                            .wrapContentSize(Alignment.Center)

                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_sched_meeting_description),
                            contentDescription = "Scheduled meeting description icon",
                            tint = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054)
                    }

                    Column(
                        modifier = Modifier
                            .padding(top = 14.dp)
                            .fillMaxSize()
                    ) {
                        Text(modifier = Modifier
                            .padding(start = 32.dp, bottom = 6.dp),
                            style = MaterialTheme.typography.subtitle1,
                            text = stringResource(id = R.string.meetings_scheduled_meeting_info_scheduled_meeting_description_label),
                            color = black.takeIf { isLight() } ?: white)
                        Text(modifier = Modifier
                            .padding(start = 32.dp),
                            style = MaterialTheme.typography.subtitle2,
                            text = description,
                            color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054,
                            fontWeight = FontWeight.Normal)
                    }
                }
            }
        }
    }
}

/**
 * Show action buttons options
 *
 * @param state         [ScheduledMeetingInfoState]
 * @param action        [ScheduledMeetingInfoAction]
 * @param isEnabled     True, if the option must be enabled. False if not
 * @param hasSwitch     True, if the option has a switch. False if not
 */
@Composable
private fun ActionOption(
    state: ScheduledMeetingInfoState,
    action: ScheduledMeetingInfoAction,
    isEnabled: Boolean,
    hasSwitch: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .clip(RectangleShape)
                    .wrapContentSize(Alignment.Center)

            ) {
                action.icon?.let { icon ->
                    Icon(painter = painterResource(id = icon),
                        contentDescription = "${action.name} icon",
                        tint = grey_alpha_054.takeIf { isLight() }
                            ?: white_alpha_054)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                ActionText(actionText = action.title)

                state.retentionTimeSeconds?.let { time ->
                    if (action == ScheduledMeetingInfoAction.ManageChatHistory) {
                        ManageChatHistorySubtitle(seconds = time)
                    }
                }

                state.dndSeconds?.let { time ->
                    if (action == ScheduledMeetingInfoAction.ChatNotifications) {
                        ChatNotificationSubtitle(seconds = time)
                    }
                }
            }
        }

        if (hasSwitch) {
            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.CenterEnd)
                    .size(40.dp)
            ) {
                Switch(
                    modifier = Modifier.align(Alignment.Center),
                    checked = isEnabled,
                    onCheckedChange = null,
                    colors = switchColors()
                )
            }

        }
    }
}

/**
 * Subtitle text of the available options
 *
 * @param text subtitle text
 */
@Composable
private fun ActionSubtitleText(text: String) {
    Text(modifier = Modifier
        .padding(start = 32.dp, end = 23.dp),
        style = MaterialTheme.typography.subtitle2,
        text = text,
        color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054)
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
        color = black.takeIf { isLight() } ?: white)
}

/**
 * View of a participant in the list
 *
 * @param participant               [ChatParticipant]
 * @param showDivider               True, if the divider should be shown. False, if it should be hidden.
 * @param onParticipantClicked       Detect when a participant is clicked
 */
@Composable
private fun ParticipantItemView(
    participant: ChatParticipant,
    showDivider: Boolean,
    onParticipantClicked: (ChatParticipant) -> Unit = {},
) {
    Column {
        Row(modifier = Modifier
            .clickable {
                onParticipantClicked(participant)
            }
            .fillMaxWidth()
            .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
            ) {
                Box {
                    ChatAvatarView(
                        avatarUri = participant.data.avatarUri,
                        avatarPlaceholder = participant.getAvatarFirstLetter(),
                        avatarColor = participant.defaultAvatarColor,
                        avatarTimestamp = participant.avatarUpdateTimestamp,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(40.dp)
                    )

                    if (participant.areCredentialsVerified) {
                        Image(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            painter = painterResource(id = R.drawable.ic_verified),
                            contentDescription = "Verified user"
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val contactName =
                            participant.data.alias ?: participant.data.fullName
                            ?: participant.email

                        Text(
                            text = if (participant.isMe) stringResource(
                                R.string.chat_me_text_bracket,
                                contactName
                            ) else contactName,
                            style = MaterialTheme.typography.subtitle1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (participant.status != UserStatus.Invalid) {
                            ContactStatus(status = participant.status)
                        }
                    }

                    if (participant.lastSeen != null || participant.status != UserStatus.Invalid) {
                        val statusText = stringResource(id = participant.status.text)
                        val secondLineText =
                            if (participant.status == UserStatus.Online) {
                                statusText
                            } else {
                                getLastSeenString(participant.lastSeen) ?: statusText
                            }

                        MarqueeText(text = secondLineText,
                            color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054,
                            style = MaterialTheme.typography.subtitle2)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.CenterEnd)
            ) {
                Row(modifier = Modifier.align(Alignment.Center)) {
                    ParticipantsPermissionView(participant)
                    Icon(modifier = Modifier.padding(start = 30.dp),
                        painter = painterResource(id = CoreUiR.drawable.ic_dots_vertical_grey),
                        contentDescription = "Three dots icon",
                        tint = grey_alpha_038.takeIf { isLight() } ?: white_alpha_038)
                }
            }
        }

        if (showDivider) {
            Divider(modifier = Modifier.padding(start = 72.dp),
                color = grey_alpha_012.takeIf { isLight() } ?: white_alpha_012,
                thickness = 1.dp)
        }
    }
}

/**
 * Participants permissions view
 *
 * @param participant [ChatParticipant]
 */
@Composable
private fun ParticipantsPermissionView(participant: ChatParticipant) {
    when (participant.privilege) {
        ChatRoomPermission.Moderator -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_permissions_full_access),
                contentDescription = "Permissions icon",
                tint = grey_alpha_038.takeIf { isLight() } ?: white_alpha_038)
        }

        ChatRoomPermission.Standard -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_permissions_read_write),
                contentDescription = "Permissions icon",
                tint = grey_alpha_038.takeIf { isLight() } ?: white_alpha_038)
        }

        ChatRoomPermission.ReadOnly -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_permissions_read_only),
                contentDescription = "Permissions icon",
                tint = grey_alpha_038.takeIf { isLight() } ?: white_alpha_038)
        }

        else -> {}
    }
}


/**
 * Manage chat history subtitle
 *
 * @param seconds  Retention time seconds
 */
@Composable
private fun ManageChatHistorySubtitle(seconds: Long) {
    var text = formatRetentionTimeInSecondsToString(seconds)
    if (text.isNotEmpty()) {
        text =
            stringResource(R.string.subtitle_properties_manage_chat) + " " + text
    }

    ActionSubtitleText(text)
}

/**
 * Chat notification subtitle
 *
 * @param seconds  Dnd seconds
 */
@Composable
private fun ChatNotificationSubtitle(seconds: Long) {
    val text = if (seconds == 0L) {
        stringResource(R.string.mute_chatroom_notification_option_off)
    } else {
        getStringForDndTime(seconds)
    }

    ActionSubtitleText(text)
}

/**
 * Get appropriate String from the seconds of retention time.
 *
 * @param seconds  The retention time in seconds
 * @return         The formatted text
 */
@Composable
fun formatRetentionTimeInSecondsToString(seconds: Long): String {
    if (seconds == Constants.DISABLED_RETENTION_TIME)
        return ""

    val years = seconds % Constants.SECONDS_IN_YEAR
    if (years == 0L) {
        return stringResource(R.string.subtitle_properties_manage_chat_label_year)
    }

    val months = seconds % Constants.SECONDS_IN_MONTH_30
    if (months == 0L) {
        val month = (seconds / Constants.SECONDS_IN_MONTH_30).toInt()
        return pluralStringResource(
            R.plurals.subtitle_properties_manage_chat_label_months,
            month,
            month
        )
    }

    val weeks = seconds % Constants.SECONDS_IN_WEEK
    if (weeks == 0L) {
        val week = (seconds / Constants.SECONDS_IN_WEEK).toInt()
        return pluralStringResource(
            R.plurals.subtitle_properties_manage_chat_label_weeks,
            week,
            week
        )
    }

    val days = seconds % Constants.SECONDS_IN_DAY
    if (days == 0L) {
        val day = (seconds / Constants.SECONDS_IN_DAY).toInt()
        return pluralStringResource(
            R.plurals.label_time_in_days_full,
            day,
            day
        )
    }

    val hours = seconds % Constants.SECONDS_IN_HOUR
    if (hours == 0L) {
        val hour = (seconds / Constants.SECONDS_IN_HOUR).toInt()
        return pluralStringResource(
            R.plurals.subtitle_properties_manage_chat_label_hours,
            hour,
            hour
        )
    }
    return ""
}

/**
 * Get the appropriate text depending on the time selected for the do not disturb option
 *
 * @param seconds       The seconds which have been set for do not disturb mode
 * @return              The right string
 */
@Composable
fun getStringForDndTime(seconds: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = seconds * 1000

    val calToday = Calendar.getInstance()
    calToday.timeInMillis = System.currentTimeMillis()

    val calTomorrow = Calendar.getInstance()
    calTomorrow.add(Calendar.DATE, +1)

    val df =
        SimpleDateFormat(
            android.text.format.DateFormat.getBestDateTimePattern(
                Locale.getDefault(),
                "HH:mm"
            ), Locale.getDefault()
        )
    val tz = cal.timeZone

    df.timeZone = tz

    return pluralStringResource(
        R.plurals.chat_notifications_muted_until_specific_time,
        cal[Calendar.HOUR_OF_DAY], df.format(cal.time)
    )
}


internal const val ACTION_BUTTON_OPTION_TAG = "scheduled_meeting_info:action_button_option"

/**
 * Meeting link action button View Preview
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewActionButton")
@Composable
fun PreviewActionButton() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ActionButton(state = ScheduledMeetingInfoState(
            scheduledMeeting = ChatScheduledMeeting(
                chatId = -1,
                schedId = -1,
                parentSchedId = null,
                organizerUserId = null,
                timezone = null,
                startDateTime = -1,
                endDateTime = -1,
                title = "Scheduled title",
                description = "Scheduled description",
                attributes = null,
                overrides = null,
                flags = null,
                rules = null,
                changes = null
            )
        ),
            action = ScheduledMeetingInfoAction.MeetingLink,
            isWaitingRoomFeatureFlagEnabled = true,
            enabledMeetingLinkOption = true,
            enabledWaitingRoomOption = true,
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
            scheduledMeeting = ChatScheduledMeeting(
                chatId = -1,
                schedId = -1,
                parentSchedId = null,
                organizerUserId = null,
                timezone = null,
                startDateTime = -1,
                endDateTime = -1,
                title = "Scheduled title",
                description = "Scheduled description",
                attributes = null,
                overrides = null,
                flags = null,
                rules = null,
                changes = null
            )
        ), onAddParticipantsClicked = {})
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
                scheduledMeeting = ChatScheduledMeeting(
                    chatId = -1,
                    schedId = -1,
                    parentSchedId = null,
                    organizerUserId = null,
                    timezone = null,
                    startDateTime = -1,
                    endDateTime = -1,
                    title = "Scheduled title",
                    description = "Scheduled description",
                    attributes = null,
                    overrides = null,
                    flags = null,
                    rules = null,
                    changes = null
                )
            ),
            managementState = ScheduledMeetingManagementState(),
            onButtonClicked = {},
            onEditClicked = {},
            onAddParticipantsClicked = {},
            onSeeMoreOrLessClicked = {},
            onLeaveGroupClicked = {},
            onParticipantClicked = {},
            onScrollChange = {},
            onBackPressed = {},
            onDismiss = {},
            onLeaveGroupDialog = {},
            onInviteParticipantsDialog = {},
            onSnackbarShown = {},
            onLearnMoreWarningClicked = {},
            onCloseWarningClicked = {},
        )
    }
}