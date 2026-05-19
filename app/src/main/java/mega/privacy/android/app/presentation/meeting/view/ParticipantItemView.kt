package mega.privacy.android.app.presentation.meeting.view

import androidx.annotation.StringRes
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
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.contact.component.ContactStatusDot
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.presentation.contact.view.getLastSeenString
import mega.privacy.android.app.presentation.meeting.model.ChatParticipantUiState
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.contact.components.MultiAvatarView
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_038

/**
 * View of a participant in the list
 *
 * @param participant               [mega.privacy.android.app.presentation.meeting.model.ChatParticipantUiState]
 * @param showDivider               True, if the divider should be shown. False, if it should be hidden.
 * @param onParticipantClicked       Detect when a participant is clicked
 */
@Composable
internal fun ParticipantItemView(
    participant: ChatParticipantUiState,
    showDivider: Boolean,
    onParticipantClicked: (ChatParticipantUiState) -> Unit = {},
) {
    val status = participant.contactItem.status
    Column {
        Row(
            modifier = Modifier
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
                    MultiAvatarView(
                        avatars = listOf(participant.contactItem.avatar),
                        avatarTimestamp = participant.avatarUpdateTimestamp,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(40.dp),
                    )

                    if (participant.contactItem.isVerified) {
                        Image(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            painter = painterResource(id = R.drawable.ic_contact_verified),
                            contentDescription = "Verified user"
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val contactName = participant.contactItem.displayName

                        MegaText(
                            text = if (participant.isMe) stringResource(
                                mega.privacy.android.app.R.string.chat_me_text_bracket,
                                contactName
                            ) else contactName,
                            overflow = LongTextBehaviour.Ellipsis(1),
                            style = MaterialTheme.typography.subtitle1,
                            textColor = TextColor.Primary,
                        )

                        if (status != ContactItemStatus.Unknown) {
                            ContactStatusDot(status = status)
                        }
                    }

                    if (participant.contactItem.lastSeen != null || status != ContactItemStatus.Unknown) {
                        val statusText = stringResource(id = contactItemStatusText(status))
                        val secondLineText =
                            if (status == ContactItemStatus.Online) {
                                statusText
                            } else {
                                getLastSeenString(participant.contactItem.lastSeen) ?: statusText
                            }

                        MegaText(
                            text = secondLineText,
                            textColor = TextColor.Secondary,
                            overflow = LongTextBehaviour.Marquee,
                            style = MaterialTheme.typography.subtitle2,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.CenterEnd)
            ) {
                Row(modifier = Modifier.align(Alignment.Center)) {
                    ParticipantsPermissionView(participant)
                    Icon(
                        modifier = Modifier.padding(start = 30.dp),
                        painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical),
                        contentDescription = "Three dots icon",
                        tint = grey_alpha_038.takeIf { MaterialTheme.colors.isLight }
                            ?: white_alpha_038)
                }
            }
        }

        if (showDivider) {
            Divider(
                modifier = Modifier.padding(start = 72.dp),
                color = grey_alpha_012.takeIf { MaterialTheme.colors.isLight } ?: white_alpha_012,
                thickness = 1.dp)
        }
    }
}

@StringRes
private fun contactItemStatusText(status: ContactItemStatus): Int = when (status) {
    ContactItemStatus.Online -> mega.privacy.android.app.R.string.online_status
    ContactItemStatus.Away -> mega.privacy.android.app.R.string.away_status
    ContactItemStatus.Busy -> mega.privacy.android.app.R.string.busy_status
    else -> mega.privacy.android.app.R.string.offline_status
}

/**
 * Participants permissions view
 *
 * @param participant [ChatParticipantUiState]
 */
@Composable
private fun ParticipantsPermissionView(participant: ChatParticipantUiState) {
    when (participant.privilege) {
        ChatRoomPermission.Moderator -> {
            Icon(
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.CheckCircle),
                contentDescription = "Permissions icon",
                tint = grey_alpha_038.takeIf { MaterialTheme.colors.isLight } ?: white_alpha_038)
        }

        ChatRoomPermission.Standard -> {
            Icon(
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Pen2),
                contentDescription = "Permissions icon",
                tint = grey_alpha_038.takeIf { MaterialTheme.colors.isLight } ?: white_alpha_038)
        }

        ChatRoomPermission.ReadOnly -> {
            Icon(
                painter = rememberVectorPainter(IconPack.Medium.Thin.Solid.Eye),
                contentDescription = "Permissions icon",
                tint = grey_alpha_038.takeIf { MaterialTheme.colors.isLight } ?: white_alpha_038)
        }

        else -> {}
    }
}

@CombinedThemePreviews
@Composable
fun ParticipantItemViewPreview() {
    AndroidThemeForPreviews {
        ParticipantItemView(
            participant = ChatParticipantUiState(
                contactItem = ContactItemUiState(
                    handle = 1L,
                    displayName = "Ava",
                    status = ContactItemStatus.Online,
                    lastSeen = null,
                    avatar = AvatarData.Initials(
                        initials = "A",
                        avatarColor = Color(0xFF2E7D32),
                    ),
                    isVerified = false,
                ),
                isMe = false,
                privilege = ChatRoomPermission.Standard,
                email = "user@example.com",
                avatarUpdateTimestamp = null,
            ),
            showDivider = true,
        )
    }
}