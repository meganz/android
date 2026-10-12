package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

@Serializable
data object CreateScheduledMeetingNavKey : NavKey

/**
 * Navigation key that starts an unscheduled meeting immediately.
 */
@Serializable
data object CreateMeetingNavKey : NavKey

@Serializable
data class LegacyMeetingNavKey(
    val chatId: Long,
    val meetingInfo: MeetingNavKeyInfo,
) : NoSessionNavKey.Optional

/**
 * Microphone, camera and speaker the call starts with, chosen before the in-call screen opens.
 */
@Serializable
data class CallStartSettings(
    val isMicEnabled: Boolean,
    val isCameraEnabled: Boolean,
    val isSpeakerEnabled: Boolean,
)

@Serializable
sealed interface MeetingNavKeyInfo {
    /**
     * Starts a new unscheduled meeting. The [LegacyMeetingNavKey.chatId] is not used, as the
     * chat is created when the call starts.
     */
    @Serializable
    data class CreateMeeting(
        val meetingName: String,
        val callStartSettings: CallStartSettings? = null,
    ) : MeetingNavKeyInfo {
        override fun toString() = "CreateMeeting(callStartSettings=$callStartSettings)"
    }

    @Serializable
    data class JoinAsGuest(
        val meetingName: String?,
        val link: String,
        val guestFirstName: String? = null,
        val guestLastName: String? = null,
        val callStartSettings: CallStartSettings? = null,
    ) : MeetingNavKeyInfo {
        override fun toString() = "JoinAsGuest(callStartSettings=$callStartSettings)"
    }

    @Serializable
    data class JoinInProgressCall(
        val meetingName: String,
        val link: String,
        val callStartSettings: CallStartSettings? = null,
    ) : MeetingNavKeyInfo {
        override fun toString() = "JoinInProgressCall(callStartSettings=$callStartSettings)"
    }

    @Serializable
    data class RejoinInProgressCall(
        val meetingName: String,
        val publicChatHandle: Long,
        val link: String,
        val callStartSettings: CallStartSettings? = null,
    ) : MeetingNavKeyInfo {
        override fun toString() =
            "RejoinInProgressCall(publicChatHandle=$publicChatHandle, callStartSettings=$callStartSettings)"
    }

    @Serializable
    data class ReturnToInProgressCall(
        val isGuest: Boolean,
    ) : MeetingNavKeyInfo

    @Serializable
    data class OpenCall(
        val callId: Long,
        val isGuest: Boolean,
        val hasLocalVideo: Boolean,
        val isOutgoing: Boolean,
        val answer: Boolean,
    ) : MeetingNavKeyInfo

    @Serializable
    data class StartOutgoingCall(
        val isAudioEnable: Boolean,
        val isVideoEnable: Boolean,
    ) : MeetingNavKeyInfo
}

@Serializable
data class LegacyWaitingRoomNavKey(
    val chatId: Long,
    val waitingRoomInfo: WaitingRoomNavKeyInfo,
) : NoSessionNavKey.Optional

@Serializable
sealed interface WaitingRoomNavKeyInfo {
    @Serializable
    data class JoinAsGuest(
        val link: String,
    ) : WaitingRoomNavKeyInfo {
        override fun toString() = "JoinAsGuest()"
    }

    @Serializable
    data class JoinWaitingRoom(
        val link: String,
    ) : WaitingRoomNavKeyInfo {
        override fun toString() = "JoinWaitingRoom()"
    }
}