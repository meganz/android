package mega.privacy.android.app.presentation.meeting.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.dialog.view.CallParticipantBottomSheetView
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_054_white_alpha_054
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.meeting.CallParticipantData
import mega.privacy.android.domain.entity.meeting.CallType
import mega.privacy.android.domain.entity.meeting.ParticipantsSection

/**
 * Participant list view
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ParticipantsFullListView(
    state: MeetingState,
    onAdmitAllClicked: () -> Unit,
    onShareMeetingLink: () -> Unit,
    onBackPressed: () -> Unit,
    onConsumeSelectParticipantEvent: () -> Unit,
    onScrollChange: (Boolean) -> Unit,
    onAdmitParticipantClicked: (ChatParticipant) -> Unit,
    onDenyParticipantClicked: (ChatParticipant) -> Unit,
    onParticipantMoreOptionsClicked: (ChatParticipant) -> Unit,
    onAddContactClicked: () -> Unit,
    onContactInfoClicked: (String) -> Unit,
    onEditProfileClicked: () -> Unit,
    onSendMessageClicked: () -> Unit,
    onMakeHostClicked: () -> Unit,
    onRemoveAsHostClicked: () -> Unit,
    onDisplayInMainViewClicked: () -> Unit,
    onRemoveParticipantClicked: () -> Unit,
    onBottomPanelHiddenClicked: () -> Unit,
    onRemoveParticipant: () -> Unit,
    onDismissRemoveParticipantDialog: () -> Unit,
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
        val coroutineScope = rememberCoroutineScope()
        val modalSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            confirmValueChange = { it != ModalBottomSheetValue.HalfExpanded },
            skipHalfExpanded = true,
        )

        BackHandler(enabled = modalSheetState.isVisible) {
            coroutineScope.launch {
                modalSheetState.hide()
                onBottomPanelHiddenClicked()
            }
        }

        EventEffect(
            event = state.selectParticipantEvent,
            onConsumed = onConsumeSelectParticipantEvent,
            action = { modalSheetState.show() }
        )

        Scaffold(
            modifier = Modifier.padding(top = 34.dp),
            scaffoldState = scaffoldState,
            topBar = {
                ParticipantsFullListAppBar(
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

            state.chatParticipantSelected?.apply {
                RemoveParticipantAlertDialog(
                    shouldShowDialog = state.removeParticipantDialog,
                    participantName = data.fullName ?: email ?: "",
                    onDismiss = { onDismissRemoveParticipantDialog() },
                    onRemove = { onRemoveParticipant() })
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            LazyColumn(
                                state = listState,
                            ) {
                                item(key = "Participants lists") {
                                    when (state.participantsSection) {
                                        ParticipantsSection.WaitingRoomSection ->
                                            state.chatParticipantsInWaitingRoom.indices.forEach { i ->
                                                ParticipantInCallItem(
                                                    section = state.participantsSection,
                                                    hasHostPermission = state.hasHostPermission,
                                                    isGuest = state.isGuest,
                                                    participant = state.chatParticipantsInWaitingRoom[i],
                                                    onAdmitParticipantClicked = onAdmitParticipantClicked,
                                                    onDenyParticipantClicked = onDenyParticipantClicked,
                                                    onParticipantMoreOptionsClicked = onParticipantMoreOptionsClicked
                                                )
                                            }

                                        ParticipantsSection.InCallSection ->
                                            state.chatParticipantsInCall.indices.forEach { i ->
                                                ParticipantInCallItem(
                                                    section = state.participantsSection,
                                                    hasHostPermission = state.hasHostPermission,
                                                    isGuest = state.isGuest,
                                                    participant = state.chatParticipantsInCall[i],
                                                    onAdmitParticipantClicked = onAdmitParticipantClicked,
                                                    onDenyParticipantClicked = onDenyParticipantClicked,
                                                    onParticipantMoreOptionsClicked = onParticipantMoreOptionsClicked
                                                )
                                            }

                                        ParticipantsSection.NotInCallSection ->
                                            state.chatParticipantsNotInCall.indices.forEach { i ->
                                                ParticipantInCallItem(
                                                    section = state.participantsSection,
                                                    hasHostPermission = state.hasHostPermission,
                                                    isGuest = state.isGuest,
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

                if (state.participantsSection != ParticipantsSection.NotInCallSection || !state.hasHostPermission) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RaisedDefaultMegaButton(
                            modifier = Modifier
                                .padding(start = 24.dp, end = 24.dp, bottom = 15.dp)
                                .fillMaxWidth(),
                            textId = when (state.participantsSection) {
                                ParticipantsSection.WaitingRoomSection -> R.string.meetings_waiting_room_admit_users_to_call_dialog_admit_button
                                else -> if (state.callType == CallType.Meeting) R.string.meetings_scheduled_meeting_info_share_meeting_link_label else R.string.meetings_group_call_bottom_panel_share_chat_link_button
                            },
                            onClick = when (state.participantsSection) {
                                ParticipantsSection.WaitingRoomSection -> onAdmitAllClicked
                                else -> onShareMeetingLink
                            }
                        )
                    }
                }
            }
        }

        onScrollChange(!firstItemVisible)

        CallParticipantBottomSheetView(
            modalSheetState = modalSheetState,
            coroutineScope = coroutineScope,
            state = state,
            onAddContactClick = onAddContactClicked,
            onContactInfoClick = onContactInfoClicked,
            onEditProfileClick = onEditProfileClicked,
            onSendMessageClick = onSendMessageClicked,
            onMakeHostClick = onMakeHostClicked,
            onRemoveAsHostClick = onRemoveAsHostClicked,
            onDisplayInMainViewClick = onDisplayInMainViewClicked,
            onRemoveParticipantClick = onRemoveParticipantClicked,
        )
    }
}

/**
 * Remove participant Alert Dialog
 *
 * @param shouldShowDialog          True, show dialog. False, hide dialog.
 * @param participantName           Name of participant selected.
 * @param onDismiss                 When dismiss the alert dialog.
 * @param onRemove                  When remove a participant.
 */
@Composable
private fun RemoveParticipantAlertDialog(
    shouldShowDialog: Boolean,
    participantName: String,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
) {
    if (shouldShowDialog) {
        MegaAlertDialog(
            text = stringResource(id = R.string.confirmation_remove_chat_contact, participantName),
            confirmButtonText = stringResource(id = R.string.general_remove),
            cancelButtonText = stringResource(id = R.string.general_cancel),
            onConfirm = onRemove,
            onDismiss = onDismiss,
        )
    }
}

/**
 * App bar view
 *
 * @param participantsSize          Number of participants
 * @param section                   Section selected
 * @param onBackPressed             When on back pressed option is clicked
 * @param elevation                 True if it has elevation. False, if it does not.
 */
@Composable
private fun ParticipantsFullListAppBar(
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
                        id = when (section) {
                            ParticipantsSection.WaitingRoomSection -> R.plurals.subtitle_of_group_chat
                            ParticipantsSection.InCallSection -> R.plurals.subtitle_of_group_chat
                            ParticipantsSection.NotInCallSection -> R.plurals.meetings_meeting_not_in_call_section_subtitle
                        },
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
 * [ParticipantsFullListView] preview
 */
@Preview
@Composable
fun PreviewUsersListViewWaitingRoom() {
    MegaAppTheme(isDark = true) {
        ParticipantsFullListView(state = MeetingState(
            participantsSection = ParticipantsSection.WaitingRoomSection,
            chatParticipantsInWaitingRoom = getParticipants(),
            hasWaitingRoom = true
        ),
            onBackPressed = {},
            onShareMeetingLink = {},
            onScrollChange = {},
            onAdmitAllClicked = {},
            onConsumeSelectParticipantEvent = {},
            onRemoveParticipantClicked = {},
            onRemoveAsHostClicked = {},
            onMakeHostClicked = {},
            onContactInfoClicked = {},
            onEditProfileClicked = {},
            onParticipantMoreOptionsClicked = {},
            onDenyParticipantClicked = {},
            onAdmitParticipantClicked = {},
            onAddContactClicked = {},
            onBottomPanelHiddenClicked = {},
            onDismissRemoveParticipantDialog = {},
            onDisplayInMainViewClicked = {},
            onRemoveParticipant = {},
            onSendMessageClicked = {})
    }
}

/**
 * [ParticipantsFullListView] preview
 */
@Preview
@Composable
fun PreviewUsersListViewInCall() {
    MegaAppTheme(isDark = true) {
        ParticipantsFullListView(state = MeetingState(
            participantsSection = ParticipantsSection.InCallSection,
            chatParticipantsInCall = getParticipants(),
            hasWaitingRoom = true
        ),
            onBackPressed = {},
            onShareMeetingLink = {},
            onScrollChange = {},
            onAdmitAllClicked = {},
            onConsumeSelectParticipantEvent = {},
            onRemoveParticipantClicked = {},
            onRemoveAsHostClicked = {},
            onMakeHostClicked = {},
            onContactInfoClicked = {},
            onEditProfileClicked = {},
            onParticipantMoreOptionsClicked = {},
            onDenyParticipantClicked = {},
            onAdmitParticipantClicked = {},
            onAddContactClicked = {},
            onBottomPanelHiddenClicked = {},
            onDismissRemoveParticipantDialog = {},
            onDisplayInMainViewClicked = {},
            onRemoveParticipant = {},
            onSendMessageClicked = {})
    }
}

/**
 * [ParticipantsFullListView] preview
 */
@Preview
@Composable
fun PreviewUsersListViewNotInCall() {
    MegaAppTheme(isDark = true) {
        ParticipantsFullListView(state = MeetingState(
            participantsSection = ParticipantsSection.NotInCallSection,
            chatParticipantsNotInCall = getParticipants(),
            hasWaitingRoom = true
        ),
            onBackPressed = {},
            onShareMeetingLink = {},
            onScrollChange = {},
            onAdmitAllClicked = {},
            onConsumeSelectParticipantEvent = {},
            onRemoveParticipantClicked = {},
            onRemoveAsHostClicked = {},
            onMakeHostClicked = {},
            onContactInfoClicked = {},
            onEditProfileClicked = {},
            onParticipantMoreOptionsClicked = {},
            onDenyParticipantClicked = {},
            onAdmitParticipantClicked = {},
            onAddContactClicked = {},
            onBottomPanelHiddenClicked = {},
            onDismissRemoveParticipantDialog = {},
            onDisplayInMainViewClicked = {},
            onRemoveParticipant = {},
            onSendMessageClicked = {})
    }

}

private fun getParticipants(): List<ChatParticipant> {
    val participant1 = ChatParticipant(
        handle = 111L,
        data = ContactData(fullName = "Name1", alias = null, avatarUri = null),
        email = "name1@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 111,
        callParticipantData = CallParticipantData(
            clientId = 1L,
            isAudioOn = true,
            isVideoOn = true,
        )
    )

    val participant2 = ChatParticipant(
        handle = 222L,
        data = ContactData(fullName = "Name2", alias = null, avatarUri = null),
        email = "name2@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 222
    )
    val participant3 = ChatParticipant(
        handle = 333L,
        data = ContactData(fullName = "Name3", alias = null, avatarUri = null),
        email = "name3@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 333,
        callParticipantData = CallParticipantData(
            clientId = 3L,
            isAudioOn = false,
            isVideoOn = true,
        )
    )
    val participant4 = ChatParticipant(
        handle = 444L,
        data = ContactData(fullName = "Name4", alias = null, avatarUri = null),
        email = "name4@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Moderator,
        defaultAvatarColor = 444
    )

    val participant5 = ChatParticipant(
        handle = 555L,
        data = ContactData(fullName = "Name5", alias = null, avatarUri = null),
        email = "name5@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 555,
        callParticipantData = CallParticipantData(
            clientId = 5L,
            isAudioOn = true,
            isVideoOn = false,
        )
    )

    val participant6 = ChatParticipant(
        handle = 666L,
        data = ContactData(fullName = "Name6", alias = null, avatarUri = null),
        email = "name6@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 666
    )

    val participant7 = ChatParticipant(
        handle = 777L,
        data = ContactData(fullName = "Name7", alias = null, avatarUri = null),
        email = "name7@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 777
    )

    val participant8 = ChatParticipant(
        handle = 888L,
        data = ContactData(fullName = "Name8", alias = null, avatarUri = null),
        email = "name8@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 888
    )
    val participant9 = ChatParticipant(
        handle = 999L,
        data = ContactData(fullName = "Name9", alias = null, avatarUri = null),
        email = "name9@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 999
    )
    val participant10 = ChatParticipant(
        handle = 101010L,
        data = ContactData(fullName = "Name10", alias = null, avatarUri = null),
        email = "name10@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 101010
    )

    val participant11 = ChatParticipant(
        handle = 111111L,
        data = ContactData(fullName = "Name11", alias = null, avatarUri = null),
        email = "name11@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 111111
    )

    val participant12 = ChatParticipant(
        handle = 121212L,
        data = ContactData(fullName = "Name12", alias = null, avatarUri = null),
        email = "name12@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 121212
    )

    val participant13 = ChatParticipant(
        handle = 131313L,
        data = ContactData(fullName = "Name13", alias = null, avatarUri = null),
        email = "name13@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 131313
    )

    val participant14 = ChatParticipant(
        handle = 141414L,
        data = ContactData(fullName = "Name14", alias = null, avatarUri = null),
        email = "name14@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 141414
    )
    val participant15 = ChatParticipant(
        handle = 151515L,
        data = ContactData(fullName = "Name15", alias = null, avatarUri = null),
        email = "name15@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 151515
    )
    val participant16 = ChatParticipant(
        handle = 161616L,
        data = ContactData(fullName = "Name16", alias = null, avatarUri = null),
        email = "name16@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 16
    )

    val participant17 = ChatParticipant(
        handle = 171717L,
        data = ContactData(fullName = "Name17", alias = null, avatarUri = null),
        email = "name17@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 171717
    )

    val participant18 = ChatParticipant(
        handle = 181818L,
        data = ContactData(fullName = "Name18", alias = null, avatarUri = null),
        email = "name18@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 181818
    )

    val participant19 = ChatParticipant(
        handle = 191919L,
        data = ContactData(fullName = "Name19", alias = null, avatarUri = null),
        email = "name19@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 191919
    )

    return mutableListOf<ChatParticipant>().apply {
        add(participant1)
        add(participant2)
        add(participant3)
        add(participant4)
        add(participant5)
        add(participant6)
        add(participant7)
        add(participant8)
        add(participant9)
        add(participant10)
        add(participant11)
        add(participant12)
        add(participant13)
        add(participant14)
        add(participant15)
        add(participant16)
        add(participant17)
        add(participant18)
        add(participant19)
    }
}