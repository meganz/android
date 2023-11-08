package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.ChatSessionChanges
import nz.mega.sdk.MegaChatSession
import javax.inject.Inject

internal class ChatSessionChangesMapper @Inject constructor() {

    operator fun invoke(chatSessionChanges: Int) = when (chatSessionChanges) {
        MegaChatSession.CHANGE_TYPE_STATUS -> ChatSessionChanges.Status
        MegaChatSession.CHANGE_TYPE_REMOTE_AVFLAGS -> ChatSessionChanges.RemoteAvFlags
        MegaChatSession.CHANGE_TYPE_SESSION_SPEAK_REQUESTED -> ChatSessionChanges.SessionSpeakRequested
        MegaChatSession.CHANGE_TYPE_SESSION_ON_LOWRES -> ChatSessionChanges.SessionOnLowRes
        MegaChatSession.CHANGE_TYPE_SESSION_ON_HIRES -> ChatSessionChanges.SessionOnHiRes
        MegaChatSession.CHANGE_TYPE_SESSION_ON_HOLD -> ChatSessionChanges.SessionOnHold
        MegaChatSession.CHANGE_TYPE_AUDIO_LEVEL -> ChatSessionChanges.AudioLevel
        MegaChatSession.CHANGE_TYPE_PERMISSIONS -> ChatSessionChanges.Permissions
        MegaChatSession.CHANGE_TYPE_SESSION_ON_RECORDING -> ChatSessionChanges.SessionOnRecording
        MegaChatSession.CHANGE_TYPE_SPEAK_PERMISSION -> ChatSessionChanges.SpeakPermissions
        else -> ChatSessionChanges.NoChanges
    }
}