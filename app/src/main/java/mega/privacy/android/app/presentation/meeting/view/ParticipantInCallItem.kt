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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.view.ContactStatusView
import mega.privacy.android.app.presentation.contact.view.DefaultAvatarView
import mega.privacy.android.app.presentation.contact.view.UriAvatarView
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.call.CallParticipantData
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.meeting.MeetingParticipantNotInCallStatus
import mega.privacy.android.domain.entity.meeting.ParticipantsSection
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconR
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaButtonWithIcon
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_038_white_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_054_white_alpha_054
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_087_white

/**
 * View of a participant in the list
 *
 * @param section                           [ParticipantsSection]
 * @param myPermission                      My [ChatRoomPermission]
 * @param isGuest                           True, it's guest. False, if not.
 * @param participant                       [ChatParticipant]
 * @param isRingingAll                      True if is ringing for all participants or False otherwise.
 * @param onAdmitParticipantClicked         Detect when admit is clicked
 * @param onDenyParticipantClicked          Detect when deny is clicked
 * @param onParticipantMoreOptionsClicked   Detect when more options button is clicked
 * @param onRingParticipantClicked          Detect when ring (call) participant is clicked
 * @param isUsersLimitInCallReached         True, users in call limit has been reached. False, if not.
 */
@Composable
fun ParticipantInCallItem(
    section: ParticipantsSection,
    myPermission: ChatRoomPermission,
    isGuest: Boolean,
    participant: ChatParticipant,
    modifier: Modifier = Modifier,
    isRingingAll: Boolean = false,
    isUsersLimitInCallReached: Boolean = false,
    onAdmitParticipantClicked: (ChatParticipant) -> Unit = {},
    onDenyParticipantClicked: (ChatParticipant) -> Unit = {},
    onParticipantMoreOptionsClicked: (ChatParticipant) -> Unit = {},
    onRingParticipantClicked: (Long) -> Unit = {},
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
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Box(
                        modifier = modifier,
                    ) {
                        val avatarModifier = Modifier
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            )
                            .size(40.dp)

                        ParticipantAvatar(
                            modifier = avatarModifier.clip(CircleShape),
                            avatarUri = participant.data.avatarUri,
                            avatarColor = participant.defaultAvatarColor,
                            avatarFirstLetter = participant.getAvatarFirstLetter(),
                        )

                        if (participant.isRaisedHand) {
                            Image(
                                modifier = Modifier
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    )
                                    .size(40.dp),
                                painter = painterResource(id = R.drawable.raised_hand_icon),
                                contentDescription = "Hand Icon Raised"
                            )
                        }

                        if (participant.areCredentialsVerified) {
                            Image(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp),
                                painter = painterResource(id = IconR.drawable.ic_contact_verified),
                                contentDescription = "Verified user"
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val contactName =
                            participant.data.alias ?: participant.data.fullName ?: participant.email
                            ?: ""

                        Text(
                            text = if (participant.isMe) stringResource(
                                R.string.chat_me_text_bracket, contactName
                            ) else contactName,
                            style = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.grey_alpha_087_white),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if ((section == ParticipantsSection.InCallSection || section == ParticipantsSection.NotInCallSection) && participant.privilege == ChatRoomPermission.Moderator) {
                            Image(
                                modifier = Modifier.padding(start = 5.dp),
                                imageVector = ImageVector.vectorResource(id = R.drawable.host_icon),
                                contentDescription = "Host icon"
                            )
                        }

                        if (section == ParticipantsSection.NotInCallSection && participant.status != UserChatStatus.Invalid && !isGuest) {
                            ContactStatusView(status = participant.status)
                        }
                    }

                    if (section == ParticipantsSection.NotInCallSection && !isGuest) {
                        Text(
                            text = stringResource(
                                when {
                                    participant.callStatus == MeetingParticipantNotInCallStatus.Calling || isRingingAll -> {
                                        R.string.meetings_bottom_panel_not_in_call_participants_calling_status
                                    }

                                    participant.callStatus == MeetingParticipantNotInCallStatus.NoResponse -> {
                                        R.string.meetings_bottom_panel_not_in_call_participants_no_response_status
                                    }

                                    else -> {
                                        R.string.meetings_bottom_panel_not_in_call_participants_not_in_call_status
                                    }
                                }
                            ),
                            style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.grey_alpha_054_white_alpha_054),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.wrapContentSize(Alignment.CenterEnd)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (section) {
                        ParticipantsSection.WaitingRoomSection -> {
                            if (myPermission == ChatRoomPermission.Moderator && !isGuest) {
                                Icon(
                                    modifier = Modifier
                                        .testTag(TEST_TAG_DENY_PARTICIPANT_ICON)
                                        .padding(end = 10.dp)
                                        .clickable {
                                            onDenyParticipantClicked(
                                                participant
                                            )
                                        },
                                    imageVector = ImageVector.vectorResource(id = R.drawable.deny_participant_icon),
                                    contentDescription = "Deny icon",
                                    tint = MaterialTheme.colors.grey_alpha_087_white
                                )

                                MegaButtonWithIcon(
                                    modifier = Modifier.testTag(TEST_TAG_ADMIT_PARTICIPANT_ICON),
                                    onClick = {
                                        onAdmitParticipantClicked(
                                            participant
                                        )
                                    },
                                    icon = R.drawable.admit_participant_icon,
                                    iconColor = MaterialTheme.colors.grey_alpha_087_white,
                                    enabled = !isUsersLimitInCallReached
                                )
                            }
                        }

                        ParticipantsSection.InCallSection -> {
                            Icon(
                                modifier = Modifier.padding(start = 5.dp),
                                imageVector = ImageVector.vectorResource(id = if (participant.callParticipantData.isAudioOn) R.drawable.mic_on_participant_icon else R.drawable.mic_off_participant_icon),
                                contentDescription = "Mic icon",
                                tint = MaterialTheme.colors.grey_alpha_038_white_alpha_038
                            )

                            Icon(
                                modifier = Modifier.padding(
                                    start = 10.dp,
                                    end = if (isGuest) 10.dp else 0.dp
                                ),
                                imageVector = ImageVector.vectorResource(id = if (participant.callParticipantData.isVideoOn) R.drawable.video_on_participant_icon else R.drawable.video_off_participant_icon),
                                contentDescription = "Cam icon",
                                tint = MaterialTheme.colors.grey_alpha_038_white_alpha_038
                            )

                            if (!isGuest) {
                                IconButton(onClick = { onParticipantMoreOptionsClicked(participant) }) {
                                    Icon(
                                        modifier = Modifier.padding(start = 10.dp),
                                        painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical),
                                        contentDescription = "Three dots icon",
                                        tint = MaterialTheme.colors.grey_alpha_054_white_alpha_054
                                    )
                                }
                            }
                        }

                        ParticipantsSection.NotInCallSection -> {
                            if (participant.callStatus != MeetingParticipantNotInCallStatus.Calling && !isRingingAll && myPermission > ChatRoomPermission.ReadOnly) {
                                TextMegaButton(
                                    text = stringResource(R.string.meetings_bottom_panel_not_in_call_participants_call_button),
                                    onClick = { onRingParticipantClicked(participant.handle) })
                            }
                        }

                        else -> {}
                    }
                }
            }
        }

        Divider(
            modifier = Modifier.padding(start = 72.dp),
            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
            thickness = 1.dp
        )
    }
}

@Composable
private fun ParticipantAvatar(
    avatarUri: String?,
    avatarColor: Int,
    avatarFirstLetter: String,
    modifier: Modifier = Modifier,
) {
    if (avatarUri != null) {
        UriAvatarView(modifier = modifier, uri = avatarUri)
    } else {
        DefaultAvatarView(
            modifier = modifier, color = Color(avatarColor), content = avatarFirstLetter
        )
    }
}

/**
 * [ParticipantInCallItem] preview
 */
@Preview
@Composable
fun PreviewParticipantInWaitingRoomItem() {
    OriginalTheme(isDark = true) {
        ParticipantInCallItem(
            section = ParticipantsSection.WaitingRoomSection,
            myPermission = ChatRoomPermission.Moderator,
            isGuest = false,
            participant = ChatParticipant(
                handle = 111L,
                data = ContactData(
                    fullName = "Name1",
                    alias = null,
                    avatarUri = null,
                    userVisibility = UserVisibility.Unknown,
                ),
                email = "name1@mega.io",
                isMe = false,
                privilege = ChatRoomPermission.Standard,
                defaultAvatarColor = -11152656,
                areCredentialsVerified = true
            ),
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onParticipantMoreOptionsClicked = {},
        )
    }
}

/**
 * [ParticipantInCallItem] preview
 */
@Preview
@Composable
fun PreviewMeParticipantInCallItem() {
    OriginalTheme(isDark = true) {
        ParticipantInCallItem(
            section = ParticipantsSection.InCallSection,
            myPermission = ChatRoomPermission.Moderator,
            isGuest = false,
            participant = ChatParticipant(
                handle = 222L,
                data = ContactData(
                    fullName = "Name2",
                    alias = null,
                    avatarUri = null,
                    userVisibility = UserVisibility.Unknown,
                ),
                email = "name2@mega.io",
                isMe = true,
                privilege = ChatRoomPermission.Moderator,
                defaultAvatarColor = -6624513,
                isRaisedHand = true,
                callParticipantData = CallParticipantData(
                    clientId = 2L, isAudioOn = true, isVideoOn = true
                )
            ),
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onParticipantMoreOptionsClicked = {},
        )
    }
}

/**
 * [ParticipantInCallItem] preview
 */
@Preview
@Composable
fun PreviewParticipantInCallItem() {
    OriginalTheme(isDark = true) {
        ParticipantInCallItem(
            section = ParticipantsSection.InCallSection,
            myPermission = ChatRoomPermission.Moderator,
            isGuest = false,
            participant = ChatParticipant(
                handle = 333L,
                data = ContactData(
                    fullName = "Name3",
                    alias = null,
                    avatarUri = null,
                    userVisibility = UserVisibility.Unknown,
                ),
                email = "name3@mega.io",
                isMe = false,
                privilege = ChatRoomPermission.Moderator,
                defaultAvatarColor = -30327,
            ),
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onParticipantMoreOptionsClicked = {},
        )
    }
}

/**
 * [ParticipantInCallItem] preview
 */
@Preview
@Composable
fun PreviewGuestParticipantInCallItem() {
    OriginalTheme(isDark = true) {
        ParticipantInCallItem(
            section = ParticipantsSection.InCallSection,
            myPermission = ChatRoomPermission.Moderator,
            isGuest = true,
            participant = ChatParticipant(
                handle = 666L,
                data = ContactData(
                    fullName = "Name6",
                    alias = null,
                    avatarUri = null,
                    userVisibility = UserVisibility.Unknown,
                ),
                email = "name6@mega.io",
                isMe = false,
                privilege = ChatRoomPermission.Moderator,
                defaultAvatarColor = -30327,
            ),
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onParticipantMoreOptionsClicked = {},
        )
    }
}

/**
 * [ParticipantInCallItem] preview
 */
@Preview
@Composable
fun PreviewParticipantNotInCallItem() {
    OriginalTheme(isDark = true) {
        ParticipantInCallItem(
            section = ParticipantsSection.NotInCallSection,
            myPermission = ChatRoomPermission.Moderator,
            isGuest = false,
            participant = ChatParticipant(
                handle = 444L,
                data = ContactData(
                    fullName = "Name4",
                    alias = null,
                    avatarUri = null,
                    userVisibility = UserVisibility.Unknown,
                ),
                email = "name2@mega.io",
                isMe = false,
                privilege = ChatRoomPermission.Moderator,
                defaultAvatarColor = -30327,
                status = UserChatStatus.Online
            ),
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onParticipantMoreOptionsClicked = {},
        )
    }
}

/**
 * [ParticipantInCallItem] preview
 */
@Preview
@Composable
fun PreviewParticipantNotInCallRingingItem() {
    OriginalTheme(isDark = true) {
        ParticipantInCallItem(
            section = ParticipantsSection.NotInCallSection,
            myPermission = ChatRoomPermission.Moderator,
            isGuest = false,
            participant = ChatParticipant(
                handle = 444L,
                data = ContactData(
                    fullName = "Name4",
                    alias = null,
                    avatarUri = null,
                    userVisibility = UserVisibility.Unknown,
                ),
                email = "name2@mega.io",
                isMe = false,
                privilege = ChatRoomPermission.Moderator,
                defaultAvatarColor = -30327,
                status = UserChatStatus.Online,
                callStatus = MeetingParticipantNotInCallStatus.Calling
            ),
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onParticipantMoreOptionsClicked = {},
        )
    }
}

/**
 * [ParticipantInCallItem] preview
 */
@Preview
@Composable
fun PreviewParticipantNotInCallNoResponseItem() {
    OriginalTheme(isDark = true) {
        ParticipantInCallItem(
            section = ParticipantsSection.NotInCallSection,
            myPermission = ChatRoomPermission.Moderator,
            isGuest = false,
            participant = ChatParticipant(
                handle = 444L,
                data = ContactData(
                    fullName = "Name4",
                    alias = null,
                    avatarUri = null,
                    userVisibility = UserVisibility.Unknown,
                ),
                email = "name2@mega.io",
                isMe = false,
                privilege = ChatRoomPermission.Moderator,
                defaultAvatarColor = -30327,
                status = UserChatStatus.Online,
                callStatus = MeetingParticipantNotInCallStatus.NoResponse
            ),
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onParticipantMoreOptionsClicked = {},
        )
    }
}

/**
 * [ParticipantInCallItem] preview
 */
@Preview
@Composable
fun PreviewParticipantNotInCallItemNonHost() {
    OriginalTheme(isDark = true) {
        ParticipantInCallItem(
            section = ParticipantsSection.NotInCallSection,
            myPermission = ChatRoomPermission.Standard,
            isGuest = false,
            participant = ChatParticipant(
                handle = 444L,
                data = ContactData(
                    fullName = "Name4",
                    alias = null,
                    avatarUri = null,
                    userVisibility = UserVisibility.Unknown,
                ),
                email = "name2@mega.io",
                isMe = false,
                privilege = ChatRoomPermission.Moderator,
                defaultAvatarColor = -30327,
                status = UserChatStatus.Online,
                areCredentialsVerified = true
            ),
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onParticipantMoreOptionsClicked = {},
        )
    }
}

/**
 * [ParticipantInCallItem] preview
 */
@Preview
@Composable
fun PreviewGuestParticipantNotInCallItem() {
    OriginalTheme(isDark = true) {
        ParticipantInCallItem(
            section = ParticipantsSection.NotInCallSection,
            myPermission = ChatRoomPermission.Moderator,
            isGuest = true,
            participant = ChatParticipant(
                handle = 555L,
                data = ContactData(
                    fullName = "Name5",
                    alias = null,
                    avatarUri = null,
                    userVisibility = UserVisibility.Unknown,
                ),
                email = "name2@mega.io",
                isMe = false,
                privilege = ChatRoomPermission.Moderator,
                defaultAvatarColor = -30327,
                status = UserChatStatus.Online
            ),
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
            onParticipantMoreOptionsClicked = {},
        )
    }
}

internal const val TEST_TAG_ADMIT_PARTICIPANT_ICON = "participants_view:participant_item:admit_icon"
internal const val TEST_TAG_DENY_PARTICIPANT_ICON = "participants_view:participant_item:deny_icon"
