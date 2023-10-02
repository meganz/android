package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.chips.CallTextButtonChip
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.meeting.ParticipantsSection

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
    onSeeAllClick: (ParticipantsSection) -> Unit,
    onInviteParticipantsClick: () -> Unit,
    onShareMeetingLinkClick: () -> Unit,
    onAllowAddParticipantsClick: () -> Unit,
    onAdmitParticipantClicked: (ChatParticipant) -> Unit = {},
    onDenyParticipantClicked: (ChatParticipant) -> Unit = {},
    onParticipantMoreOptionsClicked: (ChatParticipant) -> Unit = {},
) {

    val listState = rememberLazyListState()
    val maxNumParticipantsNoSeeAllOption = 4

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 10.dp, end = 20.dp, bottom = 10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                if (state.chatParticipantsInWaitingRoom.isNotEmpty() && state.hasHostPermission && state.isWaitingRoomFeatureFlagEnabled && state.hasWaitingRoom) {
                    CallTextButtonChip(
                        modifier = Modifier
                            .padding(end = 8.dp),
                        text = stringResource(id = R.string.meetings_schedule_meeting_waiting_room_label),
                        onClick = onWaitingRoomClick,
                        isChecked = state.participantsSection == ParticipantsSection.WaitingRoomSection
                    )
                }

                CallTextButtonChip(
                    modifier = Modifier
                        .padding(end = 8.dp),
                    text = stringResource(id = R.string.meetings_bottom_panel_participants_in_call_button),
                    onClick = onInCallClick,
                    isChecked = state.participantsSection == ParticipantsSection.InCallSection
                )

                CallTextButtonChip(
                    modifier = Modifier
                        .padding(end = if (state.chatParticipantsInWaitingRoom.isEmpty()) 8.dp else 0.dp),
                    text = stringResource(id = R.string.meetings_bottom_panel_participants_not_in_call_button),
                    onClick = onNotInCallClick,
                    isChecked = state.participantsSection == ParticipantsSection.NotInCallSection
                )


                if (state.chatParticipantsInWaitingRoom.isEmpty() && state.hasHostPermission && state.isWaitingRoomFeatureFlagEnabled && state.hasWaitingRoom) {
                    CallTextButtonChip(
                        text = stringResource(id = R.string.meetings_schedule_meeting_waiting_room_label),
                        onClick = onWaitingRoomClick,
                        isChecked = state.participantsSection == ParticipantsSection.WaitingRoomSection
                    )
                }
            }

            if (state.isOpenInvite && state.participantsSection == ParticipantsSection.InCallSection) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable {
                            onAllowAddParticipantsClick()
                        },
                    verticalAlignment = Alignment.CenterVertically,

                    ) {
                    Row(
                        modifier = Modifier
                            .weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(end = 23.dp),
                            style = MaterialTheme.typography.subtitle1.copy(
                                color = MaterialTheme.colors.black_white,
                                fontSize = 14.sp
                            ),
                            text = stringResource(id = R.string.chat_group_chat_info_allow_non_host_participants_option),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .wrapContentSize(Alignment.CenterEnd)
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.participantsSection == ParticipantsSection.WaitingRoomSection || state.participantsSection == ParticipantsSection.InCallSection) {
                        Text(
                            text = when (state.participantsSection) {
                                ParticipantsSection.WaitingRoomSection -> stringResource(
                                    id = R.string.meetings_bottom_panel_number_of_participants_in_the_waiting_room_label,
                                    state.chatParticipantsInWaitingRoom.size
                                )

                                ParticipantsSection.InCallSection -> stringResource(
                                    id = R.string.participants_number,
                                    state.chatParticipantsInCall.size
                                )

                                else -> {
                                    ""
                                }
                            },
                            style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.textColorPrimary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (state.participantsSection == ParticipantsSection.WaitingRoomSection && state.hasHostPermission && state.chatParticipantsInWaitingRoom.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .wrapContentSize(Alignment.CenterEnd)
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
            }
            if (state.participantsSection == ParticipantsSection.InCallSection) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InviteParticipantsButton(
                        isHost = state.hasHostPermission,
                        isOpenInvite = state.isOpenInvite,
                        onInviteParticipantsClicked = onInviteParticipantsClick
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column {
                    LazyColumn(
                        state = listState,
                    ) {

                        item(key = "Participants lists") {
                            when (state.participantsSection) {
                                ParticipantsSection.WaitingRoomSection -> {
                                    val numParticipants = state.chatParticipantsInWaitingRoom.size
                                    state.chatParticipantsInWaitingRoom.indices.forEach { i ->
                                        if (numParticipants > maxNumParticipantsNoSeeAllOption && i == maxNumParticipantsNoSeeAllOption) {
                                            SeeAllParticipantsButton(
                                                section = state.participantsSection,
                                                onSeeAllClicked = onSeeAllClick
                                            )
                                        } else if (i < maxNumParticipantsNoSeeAllOption) {
                                            ParticipantInCallItem(
                                                section = state.participantsSection,
                                                hasHostPermission = state.hasHostPermission,
                                                participant = state.chatParticipantsInWaitingRoom[i],
                                                onAdmitParticipantClicked = onAdmitParticipantClicked,
                                                onDenyParticipantClicked = onDenyParticipantClicked,
                                                onParticipantMoreOptionsClicked = onParticipantMoreOptionsClicked
                                            )
                                        }
                                    }
                                }

                                ParticipantsSection.InCallSection -> {
                                    val numParticipants = state.chatParticipantsInCall.size

                                    state.chatParticipantsInCall.indices.forEach { i ->
                                        if (numParticipants > maxNumParticipantsNoSeeAllOption && i == maxNumParticipantsNoSeeAllOption) {
                                            SeeAllParticipantsButton(
                                                section = state.participantsSection,
                                                onSeeAllClicked = onSeeAllClick
                                            )
                                        } else if (i < maxNumParticipantsNoSeeAllOption) {
                                            ParticipantInCallItem(
                                                section = state.participantsSection,
                                                hasHostPermission = state.hasHostPermission,
                                                participant = state.chatParticipantsInCall[i],
                                                onAdmitParticipantClicked = onAdmitParticipantClicked,
                                                onDenyParticipantClicked = onDenyParticipantClicked,
                                                onParticipantMoreOptionsClicked = onParticipantMoreOptionsClicked
                                            )
                                        }
                                    }
                                }

                                ParticipantsSection.NotInCallSection -> {
                                    val numParticipants = state.chatParticipantsNotInCall.size
                                    state.chatParticipantsNotInCall.indices.forEach { i ->
                                        if (numParticipants > maxNumParticipantsNoSeeAllOption && i == maxNumParticipantsNoSeeAllOption) {
                                            SeeAllParticipantsButton(
                                                section = state.participantsSection,
                                                onSeeAllClicked = onSeeAllClick
                                            )
                                        } else if (i < maxNumParticipantsNoSeeAllOption) {
                                            ParticipantInCallItem(
                                                section = state.participantsSection,
                                                hasHostPermission = state.hasHostPermission,
                                                participant = state.chatParticipantsNotInCall[i],
                                                onAdmitParticipantClicked = onAdmitParticipantClicked,
                                                onDenyParticipantClicked = onDenyParticipantClicked,
                                                onParticipantMoreOptionsClicked = onParticipantMoreOptionsClicked
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 130.dp),
        ) {
            RaisedDefaultMegaButton(
                modifier = Modifier
                    .fillMaxWidth(),
                textId = R.string.meetings_scheduled_meeting_info_share_meeting_link_label,
                onClick = onShareMeetingLinkClick
            )
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
 * See all participants in the list button view
 *
 * @param onSeeAllClicked      Detect when see all button is clicked
 */
@Composable
private fun SeeAllParticipantsButton(
    section: ParticipantsSection,
    onSeeAllClicked: (ParticipantsSection) -> Unit,
) {
    Row(modifier = Modifier
        .clickable { onSeeAllClicked(section) }
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
                    modifier = Modifier
                        .height(40.dp),
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
 * @param isHost True, it's host. False, is not host.
 * @param isOpenInvite  True it's open invite, false if not.
 * @param onInviteParticipantsClicked
 */
@Composable
private fun InviteParticipantsButton(
    isHost: Boolean,
    isOpenInvite: Boolean,
    onInviteParticipantsClicked: () -> Unit,
) {
    if (isHost || isOpenInvite) {
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
}


/**
 * [ParticipantsBottomPanelView] preview
 */
@Preview
@Composable
fun PreviewParticipantsBottomPanelView() {
    AndroidTheme(isDark = true) {
        ParticipantsBottomPanelView(
            state = MeetingState(),
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
            onSeeAllClick = {}
        )
    }
}

/**
 * [PreviewParticipantsBottomPanelWaitingRoomView4Participants] preview
 */
@Preview
@Composable
fun PreviewParticipantsBottomPanelWaitingRoomView4Participants() {
    AndroidTheme(isDark = true) {
        ParticipantsBottomPanelView(
            state = MeetingState(
                participantsSection = ParticipantsSection.WaitingRoomSection,
                chatParticipantsInWaitingRoom = getListWith4Participants(),
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
            onSeeAllClick = {}
        )
    }
}

/**
 * [PreviewParticipantsBottomPanelWaitingRoomView6Participants] preview
 */
@Preview
@Composable
fun PreviewParticipantsBottomPanelWaitingRoomView6Participants() {
    AndroidTheme(isDark = true) {
        ParticipantsBottomPanelView(
            state = MeetingState(
                participantsSection = ParticipantsSection.WaitingRoomSection,
                chatParticipantsInWaitingRoom = getListWith6Participants(),
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
            onSeeAllClick = {}
        )
    }
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

