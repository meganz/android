package mega.privacy.android.app.presentation.meeting.view


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.extensions.isPast
import mega.privacy.android.app.presentation.meeting.chat.view.NoteToSelfView
import mega.privacy.android.app.presentation.meeting.model.ChatInfoAction
import mega.privacy.android.app.presentation.meeting.model.ChatInfoUiState
import mega.privacy.android.app.presentation.meeting.model.ChatParticipantUiState
import mega.privacy.android.app.presentation.meeting.model.NoteToSelfChatUIState
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingManagementUiState
import mega.privacy.android.app.presentation.meeting.view.dialog.DenyEntryToCallDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.UsersInWaitingRoomDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.WaitingRoomWarningDialog
import mega.privacy.android.app.presentation.meeting.view.menuaction.ScheduledMeetingInfoMenuAction
import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.meeting.WaitingRoomReminders
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.legacy.core.ui.controls.divider.CustomDivider
import mega.privacy.android.shared.contact.components.MultiAvatarView
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.theme.black
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_054
import mega.privacy.android.shared.original.core.ui.theme.red_300
import mega.privacy.android.shared.original.core.ui.theme.red_600
import mega.privacy.android.shared.original.core.ui.theme.white
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_054
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedR
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Scheduled meeting info View
 */
@Composable
fun ChatInfoView(
    state: ChatInfoUiState,
    managementState: ScheduledMeetingManagementUiState,
    noteToSelfChatState: NoteToSelfChatUIState,
    onEditClicked: () -> Unit,
    onAddParticipantsClicked: () -> Unit,
    onSeeMoreOrLessClicked: () -> Unit,
    onLeaveGroupClicked: () -> Unit,
    onBackPressed: () -> Unit,
    onDismiss: () -> Unit,
    onLeaveGroupDialog: () -> Unit,
    onInviteParticipantsDialog: () -> Unit,
    onCloseWarningClicked: () -> Unit,
    onResetStateSnackbarMessage: () -> Unit = {},
    onButtonClicked: (ChatInfoAction) -> Unit = {},
    onParticipantClicked: (ChatParticipantUiState) -> Unit = {},
    usersInWaitingRoomDialog: @Composable () -> Unit = { UsersInWaitingRoomDialog() },
    denyEntryToCallDialog: @Composable () -> Unit = { DenyEntryToCallDialog() },
) {
    val shouldShowParticipantsLimitWarning =
        managementState.isCallUnlimitedProPlanFeatureFlagEnabled &&
                state.shouldShowParticipantsLimitWarning && state.isModerator
    val listState = rememberLazyListState()
    val scaffoldState = rememberScaffoldState()

    val shouldShowWarningDialog =
        state.enabledAllowNonHostAddParticipantsOption && state.enabledWaitingRoomOption && state.isHost
                && managementState.waitingRoomReminder == WaitingRoomReminders.Enabled

    MegaScaffold(
        modifier = Modifier.navigationBarsPadding(),
        scaffoldState = scaffoldState,
        topBar = {
            ChatInfoAppBar(
                state = state,
                onEditClicked = onEditClicked,
                onAddParticipantsClicked = onAddParticipantsClicked,
                onBackPressed = onBackPressed,
                titleId = R.string.general_info,
            )
        },
        scrollableContentState = listState
    ) { paddingValues ->
        LeaveGroupAlertDialog(
            state = state,
            onDismiss = { onDismiss() },
            onLeave = { onLeaveGroupDialog() })

        AddParticipantsAlertDialog(
            state = state,
            onDismiss = { onDismiss() },
            onInvite = { onInviteParticipantsDialog() })

        usersInWaitingRoomDialog()

        denyEntryToCallDialog()

        Column {
            if (shouldShowWarningDialog) {
                WaitingRoomWarningDialog(
                    onCloseClicked = onCloseWarningClicked
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.padding(paddingValues)
            ) {
                item(key = "Chat title") {
                    if (state.isNoteToSelf) {
                        NoteToSelfView(
                            isHint = noteToSelfChatState.isNoteToSelfChatEmpty,
                        )
                    } else {
                        ScheduledMeetingTitleView(state = state)
                    }
                }

                items(state.buttons) { button ->
                    ChatActionButton(
                        state = state,
                        noteToSelfChatState = noteToSelfChatState,
                        action = button,
                        enabledMeetingLinkOption = managementState.enabledMeetingLinkOption,
                        isCallInProgress = managementState.isCallInProgress,
                        onButtonClicked = onButtonClicked
                    )
                }

                if (!state.isNoteToSelf) {
                    item(key = "Participants") { ParticipantsHeader(state = state) }

                    if (shouldShowParticipantsLimitWarning) {
                        item(key = "Warning") {
                            ParticipantsLimitWarningComposeView(
                                state.isModerator,
                                modifier = Modifier.testTag(
                                    SCHEDULE_MEETING_INFO_PARTICIPANTS_WARNING_TAG
                                ),
                            )
                        }
                    }

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

                                val participant = state.participantItemList[i]
                                ParticipantItemView(
                                    participant = participant,
                                    showDivider = !isLastOne,
                                    onParticipantClicked = onParticipantClicked,
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
        }

        EventEffect(
            event = state.snackbarMsg, onConsumed = onResetStateSnackbarMessage
        ) {
            scaffoldState.snackbarHostState.showAutoDurationSnackbar(it)
        }
    }
}

/**
 * Scheduled meeting info Alert Dialog
 *
 * @param state                     [ChatInfoUiState]
 * @param onDismiss                 When dismiss the alert dialog
 * @param onLeave                   When leave the group chat room
 */
@Composable
private fun LeaveGroupAlertDialog(
    state: ChatInfoUiState,
    onDismiss: () -> Unit,
    onLeave: () -> Unit,
) {
    if (state.leaveGroupDialog) {
        ConfirmationDialog(
            title = stringResource(id = R.string.meetings_leave_meeting_confirmation_dialog_title),
            text = stringResource(id = R.string.confirmation_leave_group_chat),
            confirmButtonText = stringResource(id = R.string.general_leave),
            cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
            onConfirm = onLeave,
            onDismiss = onDismiss,
        )
    }
}

/**
 * Scheduled meeting info Alert Dialog
 *
 * @param state                     [ChatInfoUiState]
 * @param onDismiss                 When dismiss the alert dialog
 * @param onInvite                  When invite participants to group chat room
 */
@Composable
private fun AddParticipantsAlertDialog(
    state: ChatInfoUiState,
    onDismiss: () -> Unit,
    onInvite: () -> Unit,
) {

    if (state.addParticipantsNoContactsDialog || state.addParticipantsNoContactsLeftToAddDialog) {
        ConfirmationDialog(
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
            cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
            onConfirm = onInvite,
            onDismiss = onDismiss,
        )
    }
}

/**
 * Chat info App bar view
 *
 * @param state                     [ChatInfoUiState]
 * @param onEditClicked             When edit option is clicked
 * @param onAddParticipantsClicked  When add participants option is clicked
 * @param onBackPressed             When on back pressed option is clicked
 * @param titleId                   Title id
 */
@Composable
private fun ChatInfoAppBar(
    state: ChatInfoUiState,
    onEditClicked: () -> Unit,
    onAddParticipantsClicked: () -> Unit,
    onBackPressed: () -> Unit,
    titleId: Int,
) {
    MegaAppBar(
        appBarType = AppBarType.BACK_NAVIGATION,
        title = stringResource(id = titleId),
        onNavigationPressed = onBackPressed,
        actions = if (state.isNoteToSelf) null else buildList {
            if (state.isHost || state.isOpenInvite) {
                add(ScheduledMeetingInfoMenuAction.AddParticipants)
            }
            state.scheduledMeeting?.let { schedMeet ->
                if (state.isHost && !schedMeet.isPast()) {
                    add(ScheduledMeetingInfoMenuAction.EditMeeting)
                }
            }
        },
        onActionPressed = { action ->
            when (action) {
                ScheduledMeetingInfoMenuAction.AddParticipants -> onAddParticipantsClicked()
                ScheduledMeetingInfoMenuAction.EditMeeting -> onEditClicked()
            }
        },
    )
}

/**
 * Scheduled meeting info title view
 *
 * @param state [ChatInfoUiState]
 */
@Composable
private fun ScheduledMeetingTitleView(state: ChatInfoUiState) {
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
                                color = black.takeIf { MaterialTheme.colors.isLight } ?: white,
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


/**
 * Scheduled meeting subtitle
 *
 * @param state [ChatInfoUiState]
 */
@Composable
private fun ScheduledMeetingSubtitle(state: ChatInfoUiState) {
    state.scheduledMeeting?.let { schedMeet ->
        if (schedMeet.isPast()) {
            Text(text = pluralStringResource(
                R.plurals.subtitle_of_group_chat,
                state.numOfParticipants,
                state.numOfParticipants
            ),
                style = MaterialTheme.typography.body1,
                color = grey_alpha_054.takeIf { MaterialTheme.colors.isLight } ?: white_alpha_054,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis)
        } else {
            val text = getRecurringMeetingDateTime(schedMeet, state.is24HourFormat)
            if (text.isNotEmpty()) {
                Text(text = text,
                    style = MaterialTheme.typography.subtitle2,
                    color = grey_alpha_054.takeIf { MaterialTheme.colors.isLight }
                        ?: white_alpha_054,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp)
            }
        }
    }
}


/**
 * Create meeting avatar view
 *
 * @param state [ChatInfoUiState]
 */
@Composable
private fun MeetingAvatar(state: ChatInfoUiState) {
    val avatars = when {
        state.isEmptyMeeting() -> listOf(
            AvatarData.Initials(
                initials = getAvatarFirstLetter(state.chatTitle),
                avatarColor = Color.Gray,
            )
        )

        else -> listOfNotNull(
            state.firstParticipant?.contactItem?.avatar,
            state.secondParticipant?.contactItem?.avatar.takeIf { !state.isSingleMeeting() },
        )
    }
    MultiAvatarView(
        avatars = avatars,
        avatarTimestamp = state.firstParticipant?.avatarUpdateTimestamp,
        modifier = Modifier.border(1.dp, Color.White, CircleShape),
    )
}

/**
 * Participants header view
 *
 * @param state [ChatInfoUiState]
 */
@Composable
private fun ParticipantsHeader(state: ChatInfoUiState) {
    Text(modifier = Modifier.padding(
        start = 16.dp,
        top = 17.dp,
        end = 16.dp,
        bottom = 12.dp
    ),
        text = stringResource(id = R.string.participants_number, state.participantItemList.size),
        style = MaterialTheme.typography.body2,
        fontWeight = FontWeight.Medium,
        color = black.takeIf { MaterialTheme.colors.isLight } ?: white)
}

/**
 * Add participants button view
 *
 * @param state [ChatInfoUiState]
 * @param onAddParticipantsClicked
 */
@Composable
private fun AddParticipantsButton(
    state: ChatInfoUiState,
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
 * @param state [ChatInfoUiState]
 * @param onSeeMoreOrLessClicked
 */
@Composable
private fun SeeMoreOrLessParticipantsButton(
    state: ChatInfoUiState,
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
            color = red_600.takeIf { MaterialTheme.colors.isLight } ?: red_300)
    }
}

/**
 * Scheduled meeting info description view
 *
 * @param state [ChatInfoUiState]
 */
@Composable
private fun ScheduledMeetingDescriptionView(state: ChatInfoUiState) {
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
                        Icon(painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Menu04),
                            contentDescription = "Scheduled meeting description icon",
                            tint = grey_alpha_054.takeIf { MaterialTheme.colors.isLight }
                                ?: white_alpha_054)
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
                            color = black.takeIf { MaterialTheme.colors.isLight } ?: white)
                        Text(modifier = Modifier
                            .padding(start = 32.dp),
                            style = MaterialTheme.typography.subtitle2,
                            text = description,
                            color = grey_alpha_054.takeIf { MaterialTheme.colors.isLight }
                                ?: white_alpha_054,
                            fontWeight = FontWeight.Normal)
                    }
                }
            }
        }
    }
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


internal const val SCHEDULE_MEETING_INFO_PARTICIPANTS_WARNING_TAG =
    "scheduled_meeting_info:participants_warning"

/**
 * Add participants button View Preview
 */
@CombinedThemePreviews
@Composable
fun PreviewAddParticipantsButton() {
    AndroidThemeForPreviews {
        AddParticipantsButton(
            state = ChatInfoUiState(
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
            onAddParticipantsClicked = {},
        )
    }
}

/**
 * Scheduled meeting info View Preview
 */
@CombinedThemePreviews
@Composable
fun PreviewScheduledMeetingInfoView() {
    AndroidThemeForPreviews {
        ChatInfoView(
            state = ChatInfoUiState(
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
            managementState = ScheduledMeetingManagementUiState(),
            noteToSelfChatState = NoteToSelfChatUIState(),
            onButtonClicked = {},
            onEditClicked = {},
            onAddParticipantsClicked = {},
            onSeeMoreOrLessClicked = {},
            onLeaveGroupClicked = {},
            onParticipantClicked = {},
            onBackPressed = {},
            onDismiss = {},
            onLeaveGroupDialog = {},
            onInviteParticipantsDialog = {},
            onResetStateSnackbarMessage = {},
            onCloseWarningClicked = {},
            usersInWaitingRoomDialog = {},
            denyEntryToCallDialog = {},
        )
    }
}

