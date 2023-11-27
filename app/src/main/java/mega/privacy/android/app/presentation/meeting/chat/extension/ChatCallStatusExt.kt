package mega.privacy.android.app.presentation.meeting.chat.extension

import mega.privacy.android.domain.entity.meeting.ChatCallStatus

/**
 * Is started
 */
val ChatCallStatus.isStarted: Boolean
    get() = this in listOf(
        ChatCallStatus.Connecting,
        ChatCallStatus.Joining,
        ChatCallStatus.InProgress,
        ChatCallStatus.UserNoPresent,
    )

/**
 * Is joined
 */
val ChatCallStatus.isJoined: Boolean
    get() = this in listOf(
        ChatCallStatus.Connecting,
        ChatCallStatus.Joining,
        ChatCallStatus.InProgress,
    )
