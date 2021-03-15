package mega.privacy.android.app.meeting

import java.io.File

data class Participant(
    val name: String,
    val avatar: File?,
    val isMe: Boolean,
    val isModerator: Boolean,
    val isAudioOn: Boolean,
    val isVideoOn: Boolean,
)
