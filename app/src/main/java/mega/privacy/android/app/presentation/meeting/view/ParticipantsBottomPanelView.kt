package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.consumed
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.presentation.meeting.WaitingRoomManagementViewModel
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomManagementState
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.call.CallType
import mega.privacy.android.domain.entity.meeting.ParticipantsSection
import mega.privacy.android.legacy.core.ui.controls.chips.CallTextButtonChip
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.original.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.black_white
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_200_grey_700
import mega.privacy.android.shared.original.core.ui.theme.extensions.teal_300_teal_200
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.shared.original.core.ui.utils.isScreenOrientationLandscape
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.mobile.analytics.event.ScheduledMeetingShareMeetingLinkButtonEvent

/**
 * Participants bottom panel view
 */
@Composable
fun ParticipantsBottomPanelView(
    viewModel: MeetingActivityViewModel,
    waitingRoomManagementViewModel: WaitingRoomManagementViewModel,
    onInviteParticipantsClick: () -> Unit = {},
    onParticipantMoreOptionsClicked: (ChatParticipant) -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val waitingRoomManagementState by waitingRoomManagementViewModel.state.collectAsStateWithLifecycle()

    BottomPanelView(
        uiState = uiState,
        waitingRoomManagementState = waitingRoomManagementState,
        onWaitingRoomClick = {
            viewModel.updateParticipantsSection(
                ParticipantsSection.WaitingRoomSection
            )
        },
        onInCallClick = {
            viewModel.updateParticipantsSection(
                ParticipantsSection.InCallSection
            )
        },
        onNotInCallClick = {
            viewModel.updateParticipantsSection(
                ParticipantsSection.NotInCallSection
            )
        },
        onAdmitAllClick = waitingRoomManagementViewModel::admitUsersClick,
        onSeeAllClick = {
            viewModel.onSnackbarMessageConsumed()
            viewModel.onSeeAllClick()
            if (viewModel.state.value.participantsSection == ParticipantsSection.WaitingRoomSection) {
                waitingRoomManagementViewModel.setShowParticipantsInWaitingRoomDialogConsumed()
            }
        },
        onInviteParticipantsClick = onInviteParticipantsClick,
        onShareMeetingLinkClick = {
            viewModel.queryMeetingLink(
                shouldShareMeetingLink = true
            )
        },
        onAllowAddParticipantsClick = viewModel::allowAddParticipantsClick,
        onAdmitParticipantClicked = waitingRoomManagementViewModel::admitUsersClick,
        onParticipantMoreOptionsClicked = onParticipantMoreOptionsClicked,
        onDenyParticipantClicked = waitingRoomManagementViewModel::denyUsersClick,
        onRingParticipantClicked = viewModel::ringParticipant,
        onMuteAllParticipantsClick = viewModel::muteAllParticipants,
        onRingAllParticipantsClicked = viewModel::ringAllAbsentsParticipants,
        onHandRaisedSnackbarMsgConsumed = viewModel::onHandRaisedSnackbarMsgConsumed,
    )
}

/**
 * Bottom panel view
 */
@Composable
fun BottomPanelView(
    uiState: MeetingState,
    waitingRoomManagementState: WaitingRoomManagementState,
    onWaitingRoomClick: () -> Unit,
    onInCallClick: () -> Unit,
    onNotInCallClick: () -> Unit,
    onAdmitAllClick: () -> Unit,
    onSeeAllClick: () -> Unit,
    onInviteParticipantsClick: () -> Unit = {},
    onShareMeetingLinkClick: () -> Unit = {},
    onAllowAddParticipantsClick: () -> Unit = {},
    onAdmitParticipantClicked: (ChatParticipant) -> Unit = {},
    onDenyParticipantClicked: (ChatParticipant) -> Unit = {},
    onParticipantMoreOptionsClicked: (ChatParticipant) -> Unit = {},
    onRingParticipantClicked: (Long) -> Unit = {},
    onRingAllParticipantsClicked: () -> Unit = {},
    onMuteAllParticipantsClick: () -> Unit = {},
    onHandRaisedSnackbarMsgConsumed: () -> Unit = {},
) {

    val listState = rememberLazyListState()

    var isAdmitAllButtonEnabled by rememberSaveable { mutableStateOf(true) }
    var isCallUserLimitWarningShown by rememberSaveable { mutableStateOf(false) }
    var isAllowNonHostAddParticipantEnabled by rememberSaveable { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    EventEffect(
        event = uiState.handRaisedSnackbarMsg,
        onConsumed = {}
    ) {
        if (!uiState.handRaisedSnackbarMsg.equals(consumed) && uiState.isBottomPanelExpanded) {
            coroutineScope.launch {
                val result = snackbarHostState.showAutoDurationSnackbar(
                    message = it,
                )

                if (result == SnackbarResult.Dismissed) {
                    onHandRaisedSnackbarMsgConsumed()
                }
            }
        }
    }

    with(uiState) {
        isCallUserLimitWarningShown =
            if (hasHostPermission() && participantsSection == ParticipantsSection.WaitingRoomSection)
                waitingRoomManagementState.showUserLimitWarningDialogInWR
            else
                false
        isAdmitAllButtonEnabled =
            if (hasHostPermission() && participantsSection == ParticipantsSection.WaitingRoomSection)
                !waitingRoomManagementState.isAdmitAllButtonDisabled
            else
                false

        isAllowNonHostAddParticipantEnabled =
            if (hasHostPermission() && participantsSection == ParticipantsSection.InCallSection)
                !waitingRoomManagementState.isAllowNonHostAddParticipantsButtonDisabled
            else
                false

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                ) {
                    item(key = "Chips") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 20.dp, bottom = 10.dp)
                        ) {
                            if (shouldWaitingRoomSectionBeShown() && usersInWaitingRoomIDs.isNotEmpty()) {
                                CallTextButtonChip(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .fillParentMaxWidth(0.29F),
                                    text = stringResource(id = R.string.meetings_schedule_meeting_waiting_room_label),
                                    onClick = onWaitingRoomClick,
                                    isChecked = participantsSection == ParticipantsSection.WaitingRoomSection
                                )
                            }

                            CallTextButtonChip(
                                modifier = if (shouldWaitingRoomSectionBeShown()) {
                                    Modifier
                                        .padding(end = 8.dp)
                                        .fillParentMaxWidth(0.29F)
                                } else {
                                    Modifier.padding(end = 8.dp)
                                },
                                text = stringResource(id = R.string.meetings_bottom_panel_participants_in_call_button),
                                onClick = onInCallClick,
                                isChecked = participantsSection == ParticipantsSection.InCallSection
                            )

                            CallTextButtonChip(
                                modifier = if (shouldWaitingRoomSectionBeShown()) {
                                    Modifier.fillParentMaxWidth(0.29F)
                                } else {
                                    Modifier
                                },
                                text = stringResource(id = R.string.meetings_bottom_panel_participants_not_in_call_button),
                                onClick = onNotInCallClick,
                                isChecked = participantsSection == ParticipantsSection.NotInCallSection
                            )

                            if (shouldWaitingRoomSectionBeShown() && usersInWaitingRoomIDs.isEmpty()) {
                                CallTextButtonChip(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .fillParentMaxWidth(0.29F),
                                    text = stringResource(id = R.string.meetings_schedule_meeting_waiting_room_label),
                                    onClick = onWaitingRoomClick,
                                    isChecked = participantsSection == ParticipantsSection.WaitingRoomSection
                                )
                            }
                        }
                    }
                    if (hasHostPermission() && participantsSection == ParticipantsSection.InCallSection) {
                        item(key = "Allow non-hosts add participants button") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 20.dp)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable {
                                        if (isAllowNonHostAddParticipantEnabled) {
                                            onAllowAddParticipantsClick()
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically,

                                ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        modifier = Modifier.padding(end = 23.dp),
                                        style = MaterialTheme.typography.subtitle1.copy(
                                            color = MaterialTheme.colors.black_white,
                                            fontSize = 14.sp
                                        ),
                                        text = stringResource(id = R.string.chat_group_chat_info_allow_non_host_participants_option),
                                    )
                                }
                                Box(
                                    modifier = Modifier.wrapContentSize(Alignment.CenterEnd)
                                ) {
                                    MegaSwitch(
                                        modifier = Modifier.align(Alignment.Center),
                                        checked = enabledAllowNonHostAddParticipantsOption,
                                        enabled = isAllowNonHostAddParticipantEnabled,
                                        onCheckedChange = null,
                                    )
                                }
                            }
                        }
                    }

                    if (shouldNumberOfParticipantsItemBeShown()) {
                        item(key = "Number of participants") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 56.dp)
                                    .padding(bottom = 10.dp, start = 16.dp, end = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = when (participantsSection) {
                                            ParticipantsSection.WaitingRoomSection -> stringResource(
                                                id = R.string.meetings_bottom_panel_number_of_participants_in_the_waiting_room_label,
                                                usersInWaitingRoomIDs.size
                                            )

                                            ParticipantsSection.InCallSection -> stringResource(
                                                id = R.string.participants_number,
                                                chatParticipantsInCall.size
                                            )

                                            ParticipantsSection.NotInCallSection -> pluralStringResource(
                                                id = R.plurals.meetings_bottom_panel_number_of_participants_not_in_call_label,
                                                count = chatParticipantsNotInCall.size,
                                                chatParticipantsNotInCall.size
                                            )

                                        },
                                        style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.textColorPrimary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (shouldAdmitAllItemBeShown()) {
                                    Box(
                                        modifier = Modifier.wrapContentSize(Alignment.CenterEnd)
                                    ) {
                                        Row(modifier = Modifier.align(Alignment.Center)) {
                                            TextMegaButton(
                                                text = stringResource(
                                                    id = R.string.meetings_waiting_room_admit_users_to_call_dialog_admit_button,
                                                ),
                                                enabled = isAdmitAllButtonEnabled,
                                                onClick = onAdmitAllClick,
                                            )
                                        }
                                    }
                                }

                                if (shouldMuteAllItemBeShown()) {
                                    Box(
                                        modifier = Modifier
                                            .wrapContentSize(Alignment.CenterEnd)
                                            .testTag(TEST_TAG_MUTE_ALL_ITEM_VIEW)
                                            .clickable(enabled = !areAllParticipantsMuted()) {
                                                onMuteAllParticipantsClick()
                                            },
                                    ) {
                                        Row(modifier = Modifier.align(Alignment.Center)) {
                                            Text(
                                                text = if (areAllParticipantsMuted())
                                                    stringResource(id = R.string.meetings_bottom_panel_in_call_participants_all_muted_label)
                                                else
                                                    stringResource(id = R.string.meetings_bottom_panel_in_call_participants_mute_all_participants_button),
                                                style = MaterialTheme.typography.subtitle2.copy(
                                                    color = if (areAllParticipantsMuted()) MaterialTheme.colors.grey_200_grey_700 else MaterialTheme.colors.teal_300_teal_200
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                if (shouldCallAllItemBeShown()) {
                                    Box(
                                        modifier = Modifier.wrapContentSize(Alignment.CenterEnd)
                                    ) {
                                        Row(modifier = Modifier.align(Alignment.Center)) {
                                            if (isRingingAll) {
                                                Icon(
                                                    imageVector = ImageVector.vectorResource(id = R.drawable.ringing_all_icon),
                                                    contentDescription = "Call all icon",
                                                    tint = MaterialTheme.colors.secondary
                                                )
                                            } else {
                                                TextMegaButton(
                                                    text = stringResource(id = R.string.meetings_bottom_panel_not_in_call_participants_call_all_button),
                                                    onClick = onRingAllParticipantsClicked,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isCallUserLimitWarningShown) {
                        item(key = "Warning") {
                            ParticipantsLimitWarningComposeView(
                                modifier = Modifier.testTag(
                                    TEST_TAG_PARTICIPANTS_WARNING
                                ),
                                isModerator = hasHostPermission(),
                            )
                        }
                    }

                    if (shouldInviteParticipantsItemBeShown()) {
                        item(key = "Invite participants button") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 20.dp)
                                    .height(56.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                InviteParticipantsButton(
                                    onInviteParticipantsClicked = onInviteParticipantsClick
                                )
                            }
                        }
                    }


                    item(key = "Participants list") {
                        when (participantsSection) {
                            ParticipantsSection.WaitingRoomSection -> {
                                if (shouldWaitingRoomSectionBeShown() && chatParticipantsInWaitingRoom.isNotEmpty()) {
                                    chatParticipantsInWaitingRoom.indices.forEach { i ->
                                        if (i < MeetingState.MAX_PARTICIPANTS_IN_BOTTOM_PANEL) {
                                            ParticipantInCallItem(
                                                section = participantsSection,
                                                myPermission = myPermission,
                                                isGuest = isGuest,
                                                isUsersLimitInCallReached = waitingRoomManagementState.isUsersLimitInCallReached(),
                                                participant = chatParticipantsInWaitingRoom[i],
                                                onAdmitParticipantClicked = onAdmitParticipantClicked,
                                                onDenyParticipantClicked = onDenyParticipantClicked,
                                                onParticipantMoreOptionsClicked = onParticipantMoreOptionsClicked
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillParentMaxHeight()
                                            .padding(bottom = if (isScreenOrientationLandscape()) 60.dp else 100.dp)
                                    ) {
                                        EmptyState(section = participantsSection)
                                    }
                                }
                            }

                            ParticipantsSection.InCallSection -> chatParticipantsInCall.indices.forEach { i ->
                                if (i < MeetingState.MAX_PARTICIPANTS_IN_BOTTOM_PANEL) {
                                    ParticipantInCallItem(
                                        section = participantsSection,
                                        myPermission = myPermission,
                                        isGuest = isGuest,
                                        participant = chatParticipantsInCall[i],
                                        onAdmitParticipantClicked = onAdmitParticipantClicked,
                                        onDenyParticipantClicked = onDenyParticipantClicked,
                                        onParticipantMoreOptionsClicked = onParticipantMoreOptionsClicked
                                    )
                                }
                            }

                            ParticipantsSection.NotInCallSection -> {
                                if (chatParticipantsNotInCall.isNotEmpty()) {
                                    chatParticipantsNotInCall.indices.forEach { i ->
                                        if (i < MeetingState.MAX_PARTICIPANTS_IN_BOTTOM_PANEL) {
                                            ParticipantInCallItem(
                                                section = participantsSection,
                                                myPermission = myPermission,
                                                isGuest = isGuest,
                                                participant = chatParticipantsNotInCall[i],
                                                isRingingAll = isRingingAll,
                                                onAdmitParticipantClicked = onAdmitParticipantClicked,
                                                onDenyParticipantClicked = onDenyParticipantClicked,
                                                onParticipantMoreOptionsClicked = onParticipantMoreOptionsClicked,
                                                onRingParticipantClicked = onRingParticipantClicked
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillParentMaxHeight()
                                            .padding(bottom = if (isScreenOrientationLandscape()) 60.dp else 100.dp)
                                    ) {
                                        EmptyState(section = participantsSection)
                                    }
                                }
                            }
                        }
                    }

                    item(key = "See all") {
                        if (showSeeAllButton) {
                            SeeAllParticipantsButton(
                                onSeeAllClicked = onSeeAllClick
                            )
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
            ) { data ->
                MegaSnackbar(snackbarData = data)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 100.dp),
            ) {
                RaisedDefaultMegaButton(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    textId = if (callType == CallType.Meeting) R.string.meetings_scheduled_meeting_info_share_meeting_link_label else R.string.meetings_group_call_bottom_panel_share_chat_link_button,
                    onClick = {
                        Analytics.tracker.trackEvent(ScheduledMeetingShareMeetingLinkButtonEvent)
                        onShareMeetingLinkClick()
                    }
                )
            }
        }
    }
}

/**
 * Empty state view
 *
 * @param section   [ParticipantsSection]
 */
@Composable
private fun EmptyState(section: ParticipantsSection) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_meeting_participants_list_empty),
            contentDescription = "Empty state",
            modifier = Modifier
                .size(size = if (isScreenOrientationLandscape()) 70.dp else 120.dp)
                .padding(bottom = 16.dp)
        )
        Text(
            text = when (section) {
                ParticipantsSection.NotInCallSection -> stringResource(id = R.string.meetings_bottom_panel_participants_not_in_call_empty)
                ParticipantsSection.WaitingRoomSection -> stringResource(id = R.string.meetings_bottom_panel_participants_in_waiting_room_empty)
                else -> ""
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.textColorPrimary),
        )
    }
}

/**
 * See all participants in the list button view
 *
 * @param onSeeAllClicked      Detect when see all button is clicked
 */
@Composable
private fun SeeAllParticipantsButton(
    onSeeAllClicked: () -> Unit,
) {
    Row(modifier = Modifier
        .clickable { onSeeAllClicked() }
        .padding(start = 16.dp, end = 20.dp)
        .fillMaxWidth()) {
        Row(
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .height(40.dp)
                    .width(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.height(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_chevron_right),
                        contentDescription = "See all Icon",
                        tint = MaterialTheme.colors.secondary
                    )
                }
            }
            Text(
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary),
                text = stringResource(id = R.string.meetings_waiting_room_call_ui_see_all_button),
            )
        }
    }
}

/**
 * Invite participants button view
 *
 * @param onInviteParticipantsClicked
 */
@Composable
private fun InviteParticipantsButton(
    onInviteParticipantsClicked: () -> Unit,
) {
    Row(modifier = Modifier
        .clickable { onInviteParticipantsClicked() }
        .fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(bottom = 18.dp, top = 18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.padding(end = 24.dp),
                imageVector = ImageVector.vectorResource(id = R.drawable.invite_participants_icon),
                contentDescription = "Invite participants Icon",
                tint = MaterialTheme.colors.secondary
            )

            Text(
                modifier = Modifier.padding(end = 16.dp),
                style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary),
                text = stringResource(id = R.string.invite_participants),
            )
        }
    }
}

/**
 * [BottomPanelView] preview guest in call section
 */
@Preview
@Composable
fun ParticipantsBottomPanelGuestInCallSectionPreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.InCallSection,
            chatParticipantsInCall = getListWith4Participants(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Standard,
            isGuest = true
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [BottomPanelView] preview guest not in call section
 */
@Preview
@Composable
fun ParticipantsBottomPanelGuestNotInCallSectionPreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = getListWith4Participants(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Standard,
            isGuest = true
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [ParticipantsBottomPanelView] preview guest not in call section empty state
 */
@Preview
@Composable
fun ParticipantsBottomPanelGuestNotInCallSectionEmptyStatePreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = emptyList(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Standard,
            isGuest = true
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [ParticipantsBottomPanelView] preview non host with no open invite in call section
 */
@Preview
@Composable
fun ParticipantsBottomPanelNonHostInCallSectionPreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.InCallSection,
            chatParticipantsInCall = getListWith4Participants(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Standard,
            isGuest = false
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [ParticipantsBottomPanelView] preview non host with open invite in call section
 */
@Preview
@Composable
fun ParticipantsBottomPanelNonHostAndOpenInviteInCallSectionPreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.InCallSection,
            chatParticipantsInCall = getListWith4Participants(),
            hasWaitingRoom = true,
            isRingingAll = true,
            myPermission = ChatRoomPermission.Standard,
            isOpenInvite = true,
            isGuest = false
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [BottomPanelView] preview non host not in call section
 */
@Preview
@Composable
fun ParticipantsBottomPanelNonHostNotInCallSectionPreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = getListWith4Participants(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Standard,
            isGuest = false
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [BottomPanelView] preview non host not in call section empty state
 */
@Preview
@Composable
fun ParticipantsBottomPanelNonHostNotInCallSectionEmptyStatePreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = emptyList(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Standard,
            isGuest = false
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [BottomPanelView] preview in call section with 4 participants in portrait
 */
@Preview
@Composable
fun ParticipantsBottomPanelInCallView4ParticipantsPreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.InCallSection,
            chatParticipantsInCall = getListWith4Participants(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Moderator,
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [BottomPanelView] preview in call section with 6 participants in portrait
 */
@Preview
@Composable
fun ParticipantsBottomPanelInCallView6ParticipantsPreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.InCallSection,
            chatParticipantsInCall = getListWith6Participants(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [BottomPanelView] preview not in call section empty state
 */
@Preview
@Composable
fun ParticipantsBottomPanelNotInCallViewEmptyStatePreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = emptyList(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [BottomPanelView] preview not in call section with 4 participants in portrait
 */
@Preview
@Composable
fun ParticipantsBottomPanelNotInCallView4ParticipantsPreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = getListWith4Participants(),
            hasWaitingRoom = true,
            isRingingAll = true,
            myPermission = ChatRoomPermission.Moderator,
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [BottomPanelView] preview not in call section with 6 participants in portrait
 */
@Preview
@Composable
fun ParticipantsBottomPanelNotInCallView6ParticipantsPreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            isRingingAll = true,
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = getListWith6Participants(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [BottomPanelView] preview not in call section empty state
 */
@Preview(name = "5-inch Device Landscape", widthDp = 640, heightDp = 360)
@Composable
fun ParticipantsBottomPanelNotInCallViewLandEmptyStatePreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = emptyList(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [BottomPanelView] preview waiting room section empty state
 */
@Preview
@Composable
fun ParticipantsBottomPanelWaitingRoomViewEmptyStatePreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.WaitingRoomSection,
            chatParticipantsInWaitingRoom = emptyList(),
            usersInWaitingRoomIDs = emptyList(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Moderator,
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [BottomPanelView] preview waiting room section with 4 participants in portrait
 */
@Preview
@Composable
fun ParticipantsBottomPanelWaitingRoomView4ParticipantsPreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.WaitingRoomSection,
            chatParticipantsInWaitingRoom = getListWith4Participants(),
            usersInWaitingRoomIDs = get4UsersInWaitingRoomIDs(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Moderator,
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [BottomPanelView] preview waiting room section with 4 participants in landscape
 */
@Preview(name = "5-inch Device Landscape", widthDp = 640, heightDp = 360)
@Composable
fun ParticipantsBottomPanelWaitingRoomView4ParticipantsLandPreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.WaitingRoomSection,
            chatParticipantsInWaitingRoom = getListWith4Participants(),
            usersInWaitingRoomIDs = get4UsersInWaitingRoomIDs(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [BottomPanelView] preview waiting room section with 6 participants in portrait
 */
@Preview
@Composable
fun ParticipantsBottomPanelWaitingRoomView6ParticipantsPreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.WaitingRoomSection,
            chatParticipantsInWaitingRoom = getListWith6Participants(),
            usersInWaitingRoomIDs = get6UsersInWaitingRoomIDs(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [BottomPanelView] preview waiting room section with 6 participants in landscape
 */
@Preview(name = "5-inch Device Landscape", widthDp = 640, heightDp = 360)
@Composable
fun ParticipantsBottomPanelWaitingRoomView6ParticipantsLandPreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.WaitingRoomSection,
            chatParticipantsInWaitingRoom = getListWith6Participants(),
            usersInWaitingRoomIDs = get6UsersInWaitingRoomIDs(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

/**
 * [BottomPanelView] preview waiting room section empty state in landscape
 */
@Preview(name = "5-inch Device Landscape", widthDp = 640, heightDp = 360)
@Composable
fun ParticipantsBottomPanelWaitingRoomViewLandEmptyStatePreview() {
    OriginalTempTheme(isDark = true) {
        BottomPanelView(uiState = MeetingState(
            participantsSection = ParticipantsSection.WaitingRoomSection,
            chatParticipantsInWaitingRoom = emptyList(),
            usersInWaitingRoomIDs = emptyList(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
            waitingRoomManagementState = WaitingRoomManagementState(),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAllowAddParticipantsClick = {},
            onShareMeetingLinkClick = {},
            onParticipantMoreOptionsClicked = {},
            onMuteAllParticipantsClick = {},
            onSeeAllClick = {})
    }
}

private fun get4UsersInWaitingRoomIDs(): List<Long> =
    mutableListOf<Long>().apply {
        add(111L)
        add(222L)
        add(333L)
        add(444L)
    }

private fun get6UsersInWaitingRoomIDs(): List<Long> =
    mutableListOf<Long>().apply {
        add(111L)
        add(222L)
        add(333L)
        add(444L)
        add(555L)
        add(666L)
    }

private fun getListWith4Participants(): List<ChatParticipant> {
    val participant1 = ChatParticipant(
        handle = 111L,
        data = ContactData(fullName = "Pepa", alias = null, avatarUri = null),
        email = "pepa+test55@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 111
    )

    val participant2 = ChatParticipant(
        handle = 222L,
        data = ContactData(fullName = "Juan", alias = null, avatarUri = null),
        email = "juan+test255@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 222
    )
    val participant3 = ChatParticipant(
        handle = 333L,
        data = ContactData(fullName = "Rober", alias = null, avatarUri = null),
        email = "rober+test255@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 333
    )
    val participant4 = ChatParticipant(
        handle = 444L,
        data = ContactData(fullName = "Marta", alias = null, avatarUri = null),
        email = "marta+test255@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 444
    )

    return mutableListOf<ChatParticipant>().apply {
        add(participant1)
        add(participant2)
        add(participant3)
        add(participant4)
    }
}

private fun getListWith6Participants(): List<ChatParticipant> {
    val participant1 = ChatParticipant(
        handle = 111L,
        data = ContactData(fullName = "Pepa", alias = null, avatarUri = null),
        email = "pepa+test55@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 111
    )

    val participant2 = ChatParticipant(
        handle = 222L,
        data = ContactData(fullName = "Juan", alias = null, avatarUri = null),
        email = "juan+test255@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 222
    )
    val participant3 = ChatParticipant(
        handle = 333L,
        data = ContactData(fullName = "Rober", alias = null, avatarUri = null),
        email = "rober+test255@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 333
    )
    val participant4 = ChatParticipant(
        handle = 444L,
        data = ContactData(fullName = "Marta", alias = null, avatarUri = null),
        email = "marta+test255@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 444
    )

    val participant5 = ChatParticipant(
        handle = 555L,
        data = ContactData(fullName = "Luis", alias = null, avatarUri = null),
        email = "luis+test255@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 555
    )

    val participant6 = ChatParticipant(
        handle = 666L,
        data = ContactData(fullName = "Rosa", alias = null, avatarUri = null),
        email = "rosa+test255@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 666
    )

    return mutableListOf<ChatParticipant>().apply {
        add(participant1)
        add(participant2)
        add(participant3)
        add(participant4)
        add(participant5)
        add(participant6)
    }
}

internal const val TEST_TAG_MUTE_ALL_ITEM_VIEW = "sync_mute_all_view"
internal const val TEST_TAG_PARTICIPANTS_WARNING = "meeting_info:participants_warning"
