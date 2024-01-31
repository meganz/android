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
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_700
import mega.privacy.android.core.ui.theme.extensions.teal_300_teal_200
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.utils.isScreenOrientationLandscape
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.meeting.CallType
import mega.privacy.android.domain.entity.meeting.ParticipantsSection
import mega.privacy.android.legacy.core.ui.controls.chips.CallTextButtonChip
import mega.privacy.mobile.analytics.event.ScheduledMeetingShareMeetingLinkButtonEvent

/**
 * Participants bottom panel view
 */
@Composable
fun ParticipantsBottomPanelView(
    state: MeetingState,
    onWaitingRoomClick: () -> Unit,
    onInCallClick: () -> Unit,
    onNotInCallClick: () -> Unit,
    onAdmitAllClick: () -> Unit,
    onSeeAllClick: () -> Unit,
    onInviteParticipantsClick: () -> Unit,
    onShareMeetingLinkClick: () -> Unit,
    onAllowAddParticipantsClick: () -> Unit,
    onAdmitParticipantClicked: (ChatParticipant) -> Unit = {},
    onDenyParticipantClicked: (ChatParticipant) -> Unit = {},
    onParticipantMoreOptionsClicked: (ChatParticipant) -> Unit = {},
    onRingParticipantClicked: (ChatParticipant) -> Unit = {},
    onRingAllParticipantsClicked: () -> Unit = {},
    onMuteAllParticipantsClick: () -> Unit,
) {

    val listState = rememberLazyListState()
    val maxNumParticipantsNoSeeAllOption = 4

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
                        if (state.shouldWaitingRoomSectionBeShown() && state.usersInWaitingRoomIDs.isNotEmpty()) {
                            CallTextButtonChip(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .fillParentMaxWidth(0.29F),
                                text = stringResource(id = R.string.meetings_schedule_meeting_waiting_room_label),
                                onClick = onWaitingRoomClick,
                                isChecked = state.participantsSection == ParticipantsSection.WaitingRoomSection
                            )
                        }

                        CallTextButtonChip(
                            modifier = if (state.shouldWaitingRoomSectionBeShown()) {
                                Modifier
                                    .padding(end = 8.dp)
                                    .fillParentMaxWidth(0.29F)
                            } else {
                                Modifier.padding(end = 8.dp)
                            },
                            text = stringResource(id = R.string.meetings_bottom_panel_participants_in_call_button),
                            onClick = onInCallClick,
                            isChecked = state.participantsSection == ParticipantsSection.InCallSection
                        )

                        CallTextButtonChip(
                            modifier = if (state.shouldWaitingRoomSectionBeShown()) {
                                Modifier.fillParentMaxWidth(0.29F)
                            } else {
                                Modifier
                            },
                            text = stringResource(id = R.string.meetings_bottom_panel_participants_not_in_call_button),
                            onClick = onNotInCallClick,
                            isChecked = state.participantsSection == ParticipantsSection.NotInCallSection
                        )

                        if (state.shouldWaitingRoomSectionBeShown() && state.usersInWaitingRoomIDs.isEmpty()) {
                            CallTextButtonChip(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .fillParentMaxWidth(0.29F),
                                text = stringResource(id = R.string.meetings_schedule_meeting_waiting_room_label),
                                onClick = onWaitingRoomClick,
                                isChecked = state.participantsSection == ParticipantsSection.WaitingRoomSection
                            )
                        }
                    }
                }
                if (state.hasHostPermission() && state.participantsSection == ParticipantsSection.InCallSection) {
                    item(key = "Allow non-hosts add participants button") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 20.dp)
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable {
                                    onAllowAddParticipantsClick()
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
                                        color = MaterialTheme.colors.black_white, fontSize = 14.sp
                                    ),
                                    text = stringResource(id = R.string.chat_group_chat_info_allow_non_host_participants_option),
                                )
                            }
                            Box(
                                modifier = Modifier.wrapContentSize(Alignment.CenterEnd)
                            ) {
                                Switch(
                                    modifier = Modifier.align(Alignment.Center),
                                    checked = state.enabledAllowNonHostAddParticipantsOption,
                                    enabled = true,
                                    onCheckedChange = null,
                                    colors = switchColors()
                                )
                            }
                        }
                    }
                }

                if (state.shouldNumberOfParticipantsItemBeShown()) {
                    item(key = "Number of participants") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .padding(bottom = 10.dp, start = 16.dp, end = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = when (state.participantsSection) {
                                            ParticipantsSection.WaitingRoomSection -> stringResource(
                                                id = R.string.meetings_bottom_panel_number_of_participants_in_the_waiting_room_label,
                                                state.usersInWaitingRoomIDs.size
                                            )

                                            ParticipantsSection.InCallSection -> stringResource(
                                                id = R.string.participants_number,
                                                state.chatParticipantsInCall.size
                                            )

                                            ParticipantsSection.NotInCallSection -> pluralStringResource(
                                                id = R.plurals.meetings_bottom_panel_number_of_participants_not_in_call_label,
                                                count = state.chatParticipantsNotInCall.size,
                                                state.chatParticipantsNotInCall.size
                                            )

                                        },
                                        style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.textColorPrimary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (state.shouldMuteAllItemBeShown()) {
                                    Box(
                                        modifier = Modifier
                                            .wrapContentSize(Alignment.CenterEnd)
                                            .padding(start = 22.dp)
                                            .clickable {
                                                onMuteAllParticipantsClick()
                                            },

                                        ) {
                                        Text(
                                            text = if (state.allParticipantsAreMuted)
                                                stringResource(id = R.string.meetings_bottom_panel_in_call_participants_all_muted_label)
                                            else
                                                stringResource(id = R.string.meetings_bottom_panel_in_call_participants_mute_all_participants_button),
                                            style = MaterialTheme.typography.subtitle2.copy(color = if (state.allParticipantsAreMuted) MaterialTheme.colors.grey_020_grey_700 else MaterialTheme.colors.teal_300_teal_200),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            if (state.participantsSection == ParticipantsSection.WaitingRoomSection && state.usersInWaitingRoomIDs.isNotEmpty()) {
                                Box(
                                    modifier = Modifier.wrapContentSize(Alignment.CenterEnd)
                                ) {
                                    Row(modifier = Modifier.align(Alignment.Center)) {
                                        TextMegaButton(
                                            text = stringResource(
                                                id = R.string.meetings_waiting_room_admit_users_to_call_dialog_admit_button,
                                            ),
                                            onClick = onAdmitAllClick,
                                        )
                                    }
                                }
                            }

                            if (state.participantsSection == ParticipantsSection.NotInCallSection && state.myPermission > ChatRoomPermission.ReadOnly) {
                                Box(
                                    modifier = Modifier.wrapContentSize(Alignment.CenterEnd)
                                ) {
                                    Row(modifier = Modifier.align(Alignment.Center)) {
                                        if (state.isRingingAll) {
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

                if (state.participantsSection == ParticipantsSection.InCallSection && !state.isGuest && (state.hasHostPermission() || state.isOpenInvite)) {
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
                    when (state.participantsSection) {
                        ParticipantsSection.WaitingRoomSection -> {
                            if (state.shouldWaitingRoomSectionBeShown() && state.chatParticipantsInWaitingRoom.isNotEmpty()) {
                                state.chatParticipantsInWaitingRoom.indices.forEach { i ->
                                    if (i < maxNumParticipantsNoSeeAllOption) {
                                        ParticipantInCallItem(
                                            section = state.participantsSection,
                                            myPermission = state.myPermission,
                                            isGuest = state.isGuest,
                                            participant = state.chatParticipantsInWaitingRoom[i],
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
                                    EmptyState(section = state.participantsSection)
                                }
                            }
                        }

                        ParticipantsSection.InCallSection -> state.chatParticipantsInCall.indices.forEach { i ->
                            if (i < maxNumParticipantsNoSeeAllOption) {
                                ParticipantInCallItem(
                                    section = state.participantsSection,
                                    myPermission = state.myPermission,
                                    isGuest = state.isGuest,
                                    participant = state.chatParticipantsInCall[i],
                                    onAdmitParticipantClicked = onAdmitParticipantClicked,
                                    onDenyParticipantClicked = onDenyParticipantClicked,
                                    onParticipantMoreOptionsClicked = onParticipantMoreOptionsClicked
                                )
                            }
                        }

                        ParticipantsSection.NotInCallSection -> {
                            if (state.chatParticipantsNotInCall.isNotEmpty()) {
                                state.chatParticipantsNotInCall.indices.forEach { i ->
                                    if (i < maxNumParticipantsNoSeeAllOption) {
                                        ParticipantInCallItem(
                                            section = state.participantsSection,
                                            myPermission = state.myPermission,
                                            isGuest = state.isGuest,
                                            participant = state.chatParticipantsNotInCall[i],
                                            isRingingAll = state.isRingingAll,
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
                                    EmptyState(section = state.participantsSection)
                                }
                            }
                        }
                    }
                }

                item(key = "See all") {
                    if (
                        (state.participantsSection == ParticipantsSection.WaitingRoomSection && state.shouldWaitingRoomSectionBeShown() && state.chatParticipantsInWaitingRoom.size > maxNumParticipantsNoSeeAllOption) ||
                        (state.participantsSection == ParticipantsSection.InCallSection && state.chatParticipantsInCall.size > maxNumParticipantsNoSeeAllOption) ||
                        (state.participantsSection == ParticipantsSection.NotInCallSection && state.chatParticipantsNotInCall.size > maxNumParticipantsNoSeeAllOption)
                    ) {
                        SeeAllParticipantsButton(
                            onSeeAllClicked = onSeeAllClick
                        )
                    }
                }
            }
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
                textId = if (state.callType == CallType.Meeting) R.string.meetings_scheduled_meeting_info_share_meeting_link_label else R.string.meetings_group_call_bottom_panel_share_chat_link_button,
                onClick = {
                    Analytics.tracker.trackEvent(ScheduledMeetingShareMeetingLinkButtonEvent)
                    onShareMeetingLinkClick()
                }
            )
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
 * [ParticipantsBottomPanelView] preview guest in call section
 */
@Preview
@Composable
fun ParticipantsBottomPanelGuestInCallSectionPreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.InCallSection,
            chatParticipantsInCall = getListWith4Participants(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Standard,
            isGuest = true
        ),
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
 * [ParticipantsBottomPanelView] preview guest not in call section
 */
@Preview
@Composable
fun ParticipantsBottomPanelGuestNotInCallSectionPreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = getListWith4Participants(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Standard,
            isGuest = true
        ),
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
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = emptyList(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Standard,
            isGuest = true
        ),
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
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.InCallSection,
            chatParticipantsInCall = getListWith4Participants(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Standard,
            isGuest = false
        ),
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
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.InCallSection,
            chatParticipantsInCall = getListWith4Participants(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Standard,
            isOpenInvite = true,
            isGuest = false
        ),
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
 * [ParticipantsBottomPanelView] preview non host not in call section
 */
@Preview
@Composable
fun ParticipantsBottomPanelNonHostNotInCallSectionPreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = getListWith4Participants(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Standard,
            isGuest = false
        ),
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
 * [ParticipantsBottomPanelView] preview non host not in call section empty state
 */
@Preview
@Composable
fun ParticipantsBottomPanelNonHostNotInCallSectionEmptyStatePreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = emptyList(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Standard,
            isGuest = false
        ),
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
 * [ParticipantsBottomPanelView] preview in call section with 4 participants in portrait
 */
@Preview
@Composable
fun ParticipantsBottomPanelInCallView4ParticipantsPreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.InCallSection,
            chatParticipantsInCall = getListWith4Participants(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Moderator,
        ),
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
 * [ParticipantsBottomPanelView] preview in call section with 6 participants in portrait
 */
@Preview
@Composable
fun ParticipantsBottomPanelInCallView6ParticipantsPreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.InCallSection,
            chatParticipantsInCall = getListWith6Participants(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
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
 * [ParticipantsBottomPanelView] preview not in call section empty state
 */
@Preview
@Composable
fun ParticipantsBottomPanelNotInCallViewEmptyStatePreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = emptyList(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
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
 * [ParticipantsBottomPanelView] preview not in call section with 4 participants in portrait
 */
@Preview
@Composable
fun ParticipantsBottomPanelNotInCallView4ParticipantsPreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = getListWith4Participants(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Moderator,
        ),
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
 * [ParticipantsBottomPanelView] preview not in call section with 6 participants in portrait
 */
@Preview
@Composable
fun ParticipantsBottomPanelNotInCallView6ParticipantsPreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = getListWith6Participants(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
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
 * [ParticipantsBottomPanelView] preview not in call section empty state
 */
@Preview(name = "5-inch Device Landscape", widthDp = 640, heightDp = 360)
@Composable
fun ParticipantsBottomPanelNotInCallViewLandEmptyStatePreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = emptyList(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
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
 * [ParticipantsBottomPanelView] preview waiting room section empty state
 */
@Preview
@Composable
fun ParticipantsBottomPanelWaitingRoomViewEmptyStatePreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.WaitingRoomSection,
            chatParticipantsInWaitingRoom = emptyList(),
            usersInWaitingRoomIDs = emptyList(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Moderator,
        ),
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
 * [ParticipantsBottomPanelView] preview waiting room section with 4 participants in portrait
 */
@Preview
@Composable
fun ParticipantsBottomPanelWaitingRoomView4ParticipantsPreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.WaitingRoomSection,
            chatParticipantsInWaitingRoom = getListWith4Participants(),
            usersInWaitingRoomIDs = get4UsersInWaitingRoomIDs(),
            hasWaitingRoom = true,
            myPermission = ChatRoomPermission.Moderator,
        ),
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
 * [ParticipantsBottomPanelView] preview waiting room section with 4 participants in landscape
 */
@Preview(name = "5-inch Device Landscape", widthDp = 640, heightDp = 360)
@Composable
fun ParticipantsBottomPanelWaitingRoomView4ParticipantsLandPreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.WaitingRoomSection,
            chatParticipantsInWaitingRoom = getListWith4Participants(),
            usersInWaitingRoomIDs = get4UsersInWaitingRoomIDs(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
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
 * [ParticipantsBottomPanelView] preview waiting room section with 6 participants in portrait
 */
@Preview
@Composable
fun ParticipantsBottomPanelWaitingRoomView6ParticipantsPreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.WaitingRoomSection,
            chatParticipantsInWaitingRoom = getListWith6Participants(),
            usersInWaitingRoomIDs = get6UsersInWaitingRoomIDs(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
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
 * [ParticipantsBottomPanelView] preview waiting room section with 6 participants in landscape
 */
@Preview(name = "5-inch Device Landscape", widthDp = 640, heightDp = 360)
@Composable
fun ParticipantsBottomPanelWaitingRoomView6ParticipantsLandPreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.WaitingRoomSection,
            chatParticipantsInWaitingRoom = getListWith6Participants(),
            usersInWaitingRoomIDs = get6UsersInWaitingRoomIDs(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
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
 * [ParticipantsBottomPanelView] preview waiting room section empty state in landscape
 */
@Preview(name = "5-inch Device Landscape", widthDp = 640, heightDp = 360)
@Composable
fun ParticipantsBottomPanelWaitingRoomViewLandEmptyStatePreview() {
    MegaAppTheme(isDark = true) {
        ParticipantsBottomPanelView(state = MeetingState(
            participantsSection = ParticipantsSection.WaitingRoomSection,
            chatParticipantsInWaitingRoom = emptyList(),
            usersInWaitingRoomIDs = emptyList(),
            myPermission = ChatRoomPermission.Moderator,
            hasWaitingRoom = true
        ),
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