package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_054_white_alpha_054
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.meeting.ParticipantsSection

/**
 * Call participants list View
 */
@Composable
fun CallParticipantsListView(
    state: MeetingState,
    onScrollChange: (Boolean) -> Unit,
    onBackPressed: () -> Unit,
    onAdmitAllClicked: () -> Unit,
    onDenyParticipantClicked: (ChatParticipant) -> Unit,
    onAdmitParticipantClicked: (ChatParticipant) -> Unit,
    onParticipantMoreOptionsClicked: (ChatParticipant) -> Unit,
) {
    if ((state.participantsSection == ParticipantsSection.WaitingRoomSection &&
                state.shouldWaitingRoomListBeShown &&
                state.chatParticipantsInWaitingRoom.isNotEmpty()) ||
        (state.participantsSection == ParticipantsSection.InCallSection &&
                state.shouldInCallListBeShown &&
                state.chatParticipantsInCall.isNotEmpty()) ||
        (state.participantsSection == ParticipantsSection.NotInCallSection &&
                state.shouldNotInCallListBeShown &&
                state.chatParticipantsNotInCall.isNotEmpty())
    ) {
        val listState = rememberLazyListState()
        val firstItemVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
        val scaffoldState = rememberScaffoldState()
        Scaffold(
            modifier = Modifier.padding(top = 34.dp),
            scaffoldState = scaffoldState,
            topBar = {
                CallListAppBar(
                    participantsSize = when (state.participantsSection) {
                        ParticipantsSection.WaitingRoomSection -> state.chatParticipantsInWaitingRoom.size
                        ParticipantsSection.InCallSection -> state.chatParticipantsInCall.size
                        ParticipantsSection.NotInCallSection -> state.chatParticipantsNotInCall.size
                    },
                    section = state.participantsSection,
                    onBackPressed = onBackPressed,
                    elevation = !firstItemVisible
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(paddingValues)
                ) {
                    item(key = "Participants list") {
                        when (state.participantsSection) {
                            ParticipantsSection.WaitingRoomSection -> state.chatParticipantsInWaitingRoom.indices.forEach { i ->
                                ParticipantInCallItem(
                                    section = state.participantsSection,
                                    hasHostPermission = state.hasHostPermission,
                                    participant = state.chatParticipantsInWaitingRoom[i],
                                    onAdmitParticipantClicked = onAdmitParticipantClicked,
                                    onDenyParticipantClicked = onDenyParticipantClicked,
                                    onParticipantMoreOptionsClicked = {}
                                )
                            }

                            ParticipantsSection.InCallSection -> state.chatParticipantsInCall.indices.forEach { i ->
                                ParticipantInCallItem(
                                    section = state.participantsSection,
                                    hasHostPermission = state.hasHostPermission,
                                    participant = state.chatParticipantsInCall[i],
                                    onAdmitParticipantClicked = {},
                                    onDenyParticipantClicked = {},
                                    onParticipantMoreOptionsClicked = onParticipantMoreOptionsClicked
                                )
                            }

                            ParticipantsSection.NotInCallSection -> state.chatParticipantsNotInCall.indices.forEach { i ->
                                ParticipantInCallItem(
                                    section = state.participantsSection,
                                    hasHostPermission = state.hasHostPermission,
                                    participant = state.chatParticipantsNotInCall[i],
                                    onAdmitParticipantClicked = {},
                                    onDenyParticipantClicked = {},
                                    onParticipantMoreOptionsClicked = {}
                                )
                            }
                        }


                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Row {
                    RaisedDefaultMegaButton(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp)
                            .fillMaxWidth(),
                        textId = R.string.meetings_waiting_room_admit_users_to_call_dialog_admit_button,
                        onClick = onAdmitAllClicked
                    )
                }
            }
        }

        onScrollChange(!firstItemVisible)
    }
}

/**
 * call list App bar view
 *
 * @param participantsSize          Number of participants
 * @param section                   Section selected
 * @param onBackPressed             When on back pressed option is clicked
 * @param elevation                 True if it has elevation. False, if it does not.
 */
@Composable
private fun CallListAppBar(
    participantsSize: Int,
    section: ParticipantsSection,
    onBackPressed: () -> Unit,
    elevation: Boolean,
) {
    val iconColor = MaterialTheme.colors.black_white
    TopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            id = when (section) {
                                ParticipantsSection.WaitingRoomSection -> R.string.meetings_schedule_meeting_waiting_room_label
                                ParticipantsSection.InCallSection -> R.string.meetings_bottom_panel_participants_in_call_button
                                ParticipantsSection.NotInCallSection -> R.string.meetings_bottom_panel_participants_not_in_call_button
                            }
                        ),
                        style = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.black_white),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = pluralStringResource(
                        id = R.plurals.subtitle_of_group_chat,
                        count = participantsSize,
                        participantsSize
                    ),
                    style = MaterialTheme.typography.subtitle2.copy(
                        color = MaterialTheme.colors.grey_alpha_054_white_alpha_054,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

            }
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
        backgroundColor = MaterialTheme.colors.surface,
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp
    )
}

/**
 * [CallParticipantsListView] preview
 */
@Preview
@Composable
fun PreviewCallParticipantsListView() {
    AndroidTheme(isDark = true) {
        CallParticipantsListView(
            state = MeetingState(
                chatParticipantsInWaitingRoom = getListWith4Participants(),
                hasWaitingRoom = true
            ),
            onScrollChange = {},
            onBackPressed = {},
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onAdmitAllClicked = {},
            onParticipantMoreOptionsClicked = {}
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

