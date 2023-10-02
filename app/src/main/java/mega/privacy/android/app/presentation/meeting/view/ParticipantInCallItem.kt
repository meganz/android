package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.list.view.ChatAvatarView
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_038_white_alpha_038
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_087_white
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.meeting.ParticipantsSection

/**
 * View of a participant in the list
 *
 * @param section                       [ParticipantsSection]
 * @param hasHostPermission             True, it's host. False, if not.
 * @param participant                   [ChatParticipant]
 * @param onAdmitParticipantClicked     Detect when admit is clicked
 * @param onDenyParticipantClicked      Detect when deny is clicked
 * @param onParticipantMoreOptionsClicked    Detect when more options button is clicked
 */
@Composable
fun ParticipantInCallItem(
    section: ParticipantsSection,
    hasHostPermission: Boolean,
    participant: ChatParticipant,
    onAdmitParticipantClicked: (ChatParticipant) -> Unit = {},
    onDenyParticipantClicked: (ChatParticipant) -> Unit = {},
    onParticipantMoreOptionsClicked: (ChatParticipant) -> Unit = {},
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
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                        ParticipantsSection.InCallSection -> {
                            Icon(
                                modifier = Modifier.padding(start = 5.dp),
                                imageVector = ImageVector.vectorResource(id = if (participant.callParticipantData.isAudioOn) R.drawable.mic_on_participant_icon else R.drawable.mic_off_participant_icon),
                                contentDescription = "Mic icon",
                                tint = MaterialTheme.colors.grey_alpha_038_white_alpha_038
                            )

                            Icon(
                                modifier = Modifier.padding(start = 10.dp),
                                imageVector = ImageVector.vectorResource(id = if (participant.callParticipantData.isVideoOn) R.drawable.video_on_participant_icon else R.drawable.video_off_participant_icon),
                                contentDescription = "Cam icon",
                                tint = MaterialTheme.colors.grey_alpha_038_white_alpha_038
                            )

                            IconButton(
                                onClick = { onParticipantMoreOptionsClicked(participant) }
                            ) {
                                Icon(
                                    modifier = Modifier.padding(start = 10.dp),
                                    painter = painterResource(id = mega.privacy.android.core.R.drawable.ic_dots_vertical_grey),
                                    contentDescription = "Three dots icon",
                                    tint = MaterialTheme.colors.grey_alpha_038_white_alpha_038
                                )
                            }
                        }

                        else -> {}
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

