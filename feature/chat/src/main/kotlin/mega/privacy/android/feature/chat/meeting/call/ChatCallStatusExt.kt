package mega.privacy.android.feature.chat.meeting.call

import mega.privacy.android.domain.entity.call.ChatCallStatus

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