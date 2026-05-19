package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.call.CallParticipantData
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.meeting.MeetingParticipantNotInCallStatus
import mega.privacy.android.domain.entity.meeting.ParticipantsSection
import mega.privacy.android.domain.entity.user.UserVisibility

/**
 * Baseline screenshots for [ParticipantInCallItem] across every section, role,
 * status, and audio/video combination the composable renders today. Captured
 * before migrating the avatar and status indicator to the core-ui primitives so
 * the migration commit can prove visual parity.
 */
class ParticipantInCallItemScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantInCallItemInCallPeer() {
        AndroidThemeForPreviews {
            ParticipantInCallItem(
                section = ParticipantsSection.InCallSection,
                myPermission = ChatRoomPermission.Moderator,
                isGuest = false,
                participant = peerParticipant(
                    isAudioOn = true,
                    isVideoOn = true,
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantInCallItemInCallMeMicAndCamOff() {
        AndroidThemeForPreviews {
            ParticipantInCallItem(
                section = ParticipantsSection.InCallSection,
                myPermission = ChatRoomPermission.Moderator,
                isGuest = false,
                participant = peerParticipant(
                    isMe = true,
                    privilege = ChatRoomPermission.Moderator,
                    isAudioOn = false,
                    isVideoOn = false,
                    isRaisedHand = true,
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantInCallItemInCallGuest() {
        AndroidThemeForPreviews {
            ParticipantInCallItem(
                section = ParticipantsSection.InCallSection,
                myPermission = ChatRoomPermission.Moderator,
                isGuest = true,
                participant = peerParticipant(
                    isAudioOn = true,
                    isVideoOn = false,
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantInCallItemNotInCallOnline() {
        AndroidThemeForPreviews {
            ParticipantInCallItem(
                section = ParticipantsSection.NotInCallSection,
                myPermission = ChatRoomPermission.Moderator,
                isGuest = false,
                participant = peerParticipant(status = UserChatStatus.Online),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantInCallItemNotInCallAwayCalling() {
        AndroidThemeForPreviews {
            ParticipantInCallItem(
                section = ParticipantsSection.NotInCallSection,
                myPermission = ChatRoomPermission.Moderator,
                isGuest = false,
                participant = peerParticipant(
                    status = UserChatStatus.Away,
                    callStatus = MeetingParticipantNotInCallStatus.Calling,
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantInCallItemNotInCallBusyNoResponse() {
        AndroidThemeForPreviews {
            ParticipantInCallItem(
                section = ParticipantsSection.NotInCallSection,
                myPermission = ChatRoomPermission.Moderator,
                isGuest = false,
                participant = peerParticipant(
                    status = UserChatStatus.Busy,
                    callStatus = MeetingParticipantNotInCallStatus.NoResponse,
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantInCallItemNotInCallOfflineNonHost() {
        AndroidThemeForPreviews {
            ParticipantInCallItem(
                section = ParticipantsSection.NotInCallSection,
                myPermission = ChatRoomPermission.Standard,
                isGuest = false,
                participant = peerParticipant(
                    status = UserChatStatus.Offline,
                    areCredentialsVerified = true,
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantInCallItemNotInCallInvalidStatus() {
        AndroidThemeForPreviews {
            ParticipantInCallItem(
                section = ParticipantsSection.NotInCallSection,
                myPermission = ChatRoomPermission.Moderator,
                isGuest = false,
                participant = peerParticipant(status = UserChatStatus.Invalid),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantInCallItemNotInCallGuest() {
        AndroidThemeForPreviews {
            ParticipantInCallItem(
                section = ParticipantsSection.NotInCallSection,
                myPermission = ChatRoomPermission.Moderator,
                isGuest = true,
                participant = peerParticipant(status = UserChatStatus.Online),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantInCallItemWaitingRoomModerator() {
        AndroidThemeForPreviews {
            ParticipantInCallItem(
                section = ParticipantsSection.WaitingRoomSection,
                myPermission = ChatRoomPermission.Moderator,
                isGuest = false,
                participant = peerParticipant(areCredentialsVerified = true),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantInCallItemWaitingRoomUsersLimitReached() {
        AndroidThemeForPreviews {
            ParticipantInCallItem(
                section = ParticipantsSection.WaitingRoomSection,
                myPermission = ChatRoomPermission.Moderator,
                isGuest = false,
                isUsersLimitInCallReached = true,
                participant = peerParticipant(),
            )
        }
    }

    private fun peerParticipant(
        isMe: Boolean = false,
        privilege: ChatRoomPermission = ChatRoomPermission.Standard,
        status: UserChatStatus = UserChatStatus.Invalid,
        callStatus: MeetingParticipantNotInCallStatus = MeetingParticipantNotInCallStatus.NotInCall,
        areCredentialsVerified: Boolean = false,
        isAudioOn: Boolean = false,
        isVideoOn: Boolean = false,
        isRaisedHand: Boolean = false,
    ): ChatParticipant = ChatParticipant(
        handle = 1234L,
        data = ContactData(
            // Single-character name avoids the emoji-shortcode path in
            // getAvatarFirstLetter, which initialises com.vdurmont.emoji
            // EmojiManager and crashes under layoutlib (missing resource).
            fullName = "S",
            alias = null,
            avatarUri = null,
            userVisibility = UserVisibility.Unknown,
        ),
        email = "s@mega.io",
        isMe = isMe,
        privilege = privilege,
        defaultAvatarColor = -11152656,
        areCredentialsVerified = areCredentialsVerified,
        status = status,
        callParticipantData = CallParticipantData(
            clientId = 1L,
            isAudioOn = isAudioOn,
            isVideoOn = isVideoOn,
        ),
        callStatus = callStatus,
        isRaisedHand = isRaisedHand,
    )
}
