package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.ChatSessionChanges
import nz.mega.sdk.MegaChatSession
import javax.inject.Inject

/**
 * Mapper to convert chat session changes to List of [ChatSessionChanges]
 */
internal class ChatSessionChangesMapper @Inject constructor() {

    operator fun invoke(changes: Int) =
        sessionChanges.filter { (it.key and changes) != 0 }.values.toList()

    companion object {
        internal val sessionChanges = mapOf(
            MegaChatSession.CHANGE_TYPE_NO_CHANGES to ChatSessionChanges.NoChanges,
            MegaChatSession.CHANGE_TYPE_STATUS to ChatSessionChanges.Status,
            MegaChatSession.CHANGE_TYPE_REMOTE_AVFLAGS to ChatSessionChanges.RemoteAvFlags,
            MegaChatSession.CHANGE_TYPE_SESSION_SPEAK_REQUESTED to ChatSessionChanges.SessionSpeakRequested,
            MegaChatSession.CHANGE_TYPE_SESSION_ON_LOWRES to ChatSessionChanges.SessionOnLowRes,
            MegaChatSession.CHANGE_TYPE_SESSION_ON_HIRES to ChatSessionChanges.SessionOnHiRes,
            MegaChatSession.CHANGE_TYPE_SESSION_ON_HOLD to ChatSessionChanges.SessionOnHold,
            MegaChatSession.CHANGE_TYPE_AUDIO_LEVEL to ChatSessionChanges.AudioLevel,
            MegaChatSession.CHANGE_TYPE_PERMISSIONS to ChatSessionChanges.Permissions,
            MegaChatSession.CHANGE_TYPE_SPEAK_PERMISSION to ChatSessionChanges.SpeakPermissions,
            MegaChatSession.CHANGE_TYPE_SESSION_ON_RECORDING to ChatSessionChanges.SessionOnRecording,
        )
    }
}