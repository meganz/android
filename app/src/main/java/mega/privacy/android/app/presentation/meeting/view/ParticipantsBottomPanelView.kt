package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.list.view.ChatAvatarView
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.chips.CallTextButtonChip
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_038_white_alpha_038
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_087_white
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
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
    onInviteParticipantsClick: () -> Unit,
    onAdmitParticipantClicked: (ChatParticipant) -> Unit = {},
    onDenyParticipantClicked: (ChatParticipant) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .padding(start = 16.dp, top = 10.dp, end = 20.dp, bottom = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            if (state.chatParticipantsInWaitingRoom.isNotEmpty() && state.hasHostPermission) {
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

            if (state.chatParticipantsInWaitingRoom.isEmpty() && state.hasHostPermission) {
                CallTextButtonChip(
                    text = stringResource(id = R.string.meetings_schedule_meeting_waiting_room_label),
                    onClick = onWaitingRoomClick,
                    isChecked = state.participantsSection == ParticipantsSection.WaitingRoomSection
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
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
                            state.chatParticipantsInWaitingRoom.size
                        )

                        ParticipantsSection.InCallSection -> stringResource(
                            id = R.string.participants_number,
                            state.usersInCallList.size
                        )

                        ParticipantsSection.NotInCallSection -> stringResource(
                            id = R.string.participants_number,
                            state.usersNotInCallList.size
                        )
                    },
                    style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.textColorPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
        when (state.participantsSection) {
            ParticipantsSection.WaitingRoomSection ->
                state.chatParticipantsInWaitingRoom.indices.forEach { i ->
                    WaitingRoomParticipantItemView(
                        section = state.participantsSection,
                        hasHostPermission = state.hasHostPermission,
                        participant = state.chatParticipantsInWaitingRoom[i],
                        onAdmitParticipantClicked = onAdmitParticipantClicked,
                        onDenyParticipantClicked = onDenyParticipantClicked
                    )
                }

            ParticipantsSection.InCallSection ->
                state.usersInCallList.indices.forEach { i ->
                    WaitingRoomParticipantItemView(
                        section = state.participantsSection,
                        hasHostPermission = state.hasHostPermission,
                        participant = state.usersInCallList[i],
                        onAdmitParticipantClicked = onAdmitParticipantClicked,
                        onDenyParticipantClicked = onDenyParticipantClicked
                    )
                }

            ParticipantsSection.NotInCallSection -> {}
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
 * View of a participant in the list
 *
 * @param section                       [ParticipantsSection]
 * @param hasHostPermission             True, it's host. False, if not.
 * @param participant                   [ChatParticipant]
 * @param onAdmitParticipantClicked     Detect when admit is clicked
 * @param onDenyParticipantClicked      Detect when deny is clicked
 */
@Composable
private fun WaitingRoomParticipantItemView(
    section: ParticipantsSection,
    hasHostPermission: Boolean,
    participant: ChatParticipant,
    onAdmitParticipantClicked: (ChatParticipant) -> Unit = {},
    onDenyParticipantClicked: (ChatParticipant) -> Unit = {},
    onMicParticipantClicked: (ChatParticipant) -> Unit = {},
    onCamParticipantClicked: (ChatParticipant) -> Unit = {},
    onOptionsParticipantClicked: (ChatParticipant) -> Unit = {},
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    ChatAvatarView(
                        avatarUri = participant.data.avatarUri,
                        avatarPlaceholder = participant.getAvatarFirstLetter(),
                        avatarColor = participant.defaultAvatarColor,
                        avatarTimestamp = participant.avatarUpdateTimestamp,
                        modifier = Modifier
                            .size(40.dp)
                    )
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
                            style = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.grey_alpha_087_white),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (participant.privilege == ChatRoomPermission.Moderator) {
                            Image(
                                modifier = Modifier.padding(start = 5.dp),
                                imageVector = ImageVector.vectorResource(id = R.drawable.host_icon),
                                contentDescription = "Host icon"
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.CenterEnd)
            ) {
                Row(modifier = Modifier.align(Alignment.Center)) {
                    when (section) {
                        ParticipantsSection.WaitingRoomSection -> {
                            if (hasHostPermission) {
                                IconButton(
                                    onClick = { onDenyParticipantClicked(participant) }
                                ) {
                                    Icon(
                                        modifier = Modifier.padding(start = 5.dp),
                                        imageVector = ImageVector.vectorResource(id = R.drawable.deny_participant_icon),
                                        contentDescription = "Deny icon",
                                        tint = MaterialTheme.colors.grey_alpha_087_white
                                    )
                                }
                                IconButton(
                                    onClick = { onAdmitParticipantClicked(participant) }
                                ) {
                                    Icon(
                                        modifier = Modifier.padding(start = 26.dp),
                                        imageVector = ImageVector.vectorResource(id = R.drawable.admit_participant_icon),
                                        contentDescription = "Admit icon",
                                        tint = MaterialTheme.colors.grey_alpha_087_white
                                    )
                                }
                            }

                        }

                        ParticipantsSection.InCallSection, ParticipantsSection.NotInCallSection -> {
                            IconButton(
                                onClick = { onMicParticipantClicked(participant) }
                            ) {
                                Icon(
                                    modifier = Modifier.padding(start = 5.dp),
                                    imageVector = ImageVector.vectorResource(id = R.drawable.mic_on_participant_icon),
                                    contentDescription = "Mic icon",
                                    tint = MaterialTheme.colors.grey_alpha_038_white_alpha_038
                                )
                            }
                            IconButton(
                                onClick = { onCamParticipantClicked(participant) }
                            ) {
                                Icon(
                                    modifier = Modifier.padding(start = 10.dp),
                                    imageVector = ImageVector.vectorResource(id = R.drawable.video_on_participant_icon),
                                    contentDescription = "Cam icon",
                                    tint = MaterialTheme.colors.grey_alpha_038_white_alpha_038
                                )
                            }

                            IconButton(
                                onClick = { onOptionsParticipantClicked(participant) }
                            ) {
                                Icon(
                                    modifier = Modifier.padding(start = 10.dp),
                                    painter = painterResource(id = mega.privacy.android.core.R.drawable.ic_dots_vertical_grey),
                                    contentDescription = "Three dots icon",
                                    tint = MaterialTheme.colors.grey_alpha_038_white_alpha_038
                                )
                            }
                        }
                    }
                }
            }
        }

        Divider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
            thickness = 1.dp
        )
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
            onDenyParticipantClicked = {}
        )
    }
}

/**
 * [PreviewParticipantsBottomPanelWaitingRoomView] preview
 */
@Preview
@Composable
fun PreviewParticipantsBottomPanelWaitingRoomView() {
    AndroidTheme(isDark = true) {
        ParticipantsBottomPanelView(
            state = MeetingState(participantsSection = ParticipantsSection.WaitingRoomSection),
            onWaitingRoomClick = {},
            onInCallClick = {},
            onNotInCallClick = {},
            onAdmitAllClick = {},
            onInviteParticipantsClick = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {}
        )
    }
}
