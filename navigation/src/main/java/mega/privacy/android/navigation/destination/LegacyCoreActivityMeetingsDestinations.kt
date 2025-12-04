package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class LegacyMeetingNavKey(
    val chatId: Long,
    val meetingInfo: MeetingNavKeyInfo,
) : NavKey

@Serializable
sealed interface MeetingNavKeyInfo {
    @Serializable
    data class JoinAsGuest(
        val meetingName: String?,
        val link: String,
    ) : MeetingNavKeyInfo

    @Serializable
    data class JoinInProgressCall(
        val meetingName: String,
        val link: String,
    ) : MeetingNavKeyInfo

    @Serializable
    data class RejoinInProgressCall(
        val meetingName: String,
        val publicChatHandle: Long,
        val link: String,
    ) : MeetingNavKeyInfo

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
}

@Serializable
data class LegacyWaitingRoomNavKey(
    val chatId: Long,
    val waitingRoomInfo: WaitingRoomNavKeyInfo,
) : NavKey

@Serializable
sealed interface WaitingRoomNavKeyInfo {
    @Serializable
    data class JoinAsGuest(
        val link: String,
    ) : WaitingRoomNavKeyInfo

    @Serializable
    data class JoinWaitingRoom(
        val link: String,
    ) : WaitingRoomNavKeyInfo
}