package mega.privacy.android.app.meeting.adapter

import java.io.File

data class Participant(
    val name: String,
    val avatar: File?,
    val avatarBackground: String,
    val isMe: Boolean,
    val isModerator: Boolean,
    val isAudioOn: Boolean,
    val isVideoOn: Boolean,
)
