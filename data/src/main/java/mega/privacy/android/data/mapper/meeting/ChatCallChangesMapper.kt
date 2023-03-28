package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Mapper to convert chat call changes to [ChatCallChanges]
 */
internal class ChatCallChangesMapper @Inject constructor() {
    operator fun invoke(change: Int): ChatCallChanges =
        callChanges[change] ?: ChatCallChanges.Unknown

    companion object {
        internal val callChanges = mapOf(
            MegaChatCall.CHANGE_TYPE_STATUS to ChatCallChanges.Status,
            MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS to ChatCallChanges.LocalAVFlags,
            MegaChatCall.CHANGE_TYPE_RINGING_STATUS to ChatCallChanges.RingingStatus,
            MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION to ChatCallChanges.CallComposition,
            MegaChatCall.CHANGE_TYPE_CALL_ON_HOLD to ChatCallChanges.OnHold,
            MegaChatCall.CHANGE_TYPE_CALL_SPEAK to ChatCallChanges.Speaker,
            MegaChatCall.CHANGE_TYPE_AUDIO_LEVEL to ChatCallChanges.AudioLevel,
            MegaChatCall.CHANGE_TYPE_NETWORK_QUALITY to ChatCallChanges.NetworkQuality,
            MegaChatCall.CHANGE_TYPE_OUTGOING_RINGING_STOP to ChatCallChanges.OutgoingRingingStop,
        )
    }
}