package mega.privacy.android.navigation.destination

import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

@Serializable
data class LegacyMeetingNavKey(
    val chatId: Long,
    val meetingInfo: MeetingNavKeyInfo,
) : NoSessionNavKey.Optional

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
) : NoSessionNavKey.Optional

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