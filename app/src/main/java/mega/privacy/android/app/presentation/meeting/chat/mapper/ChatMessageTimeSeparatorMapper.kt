package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.app.presentation.meeting.chat.model.messages.CallUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.header.TimeHeaderUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.ManagementUiChatMessage
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Chat message time separator mapper
 *
 * @constructor Create empty Chat message date separator mapper
 */
class ChatMessageTimeSeparatorMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param firstMessage
     * @param secondMessage
     * @return Date separator if needed
     */
    operator fun invoke(
        firstMessage: UiChatMessage?,
        secondMessage: UiChatMessage?,
    ): UiChatMessage? {
        val time = secondMessage?.timeSent ?: return null
        if (secondMessage is ManagementUiChatMessage || secondMessage is CallUiMessage) {
            // always show time header for management messages without user name
            return TimeHeaderUiMessage(
                id = secondMessage.id,
                timeSent = time,
                displayAsMine = false,
                userHandle = -1L
            )
        }
        val firstMessageTime = firstMessage?.timeSent ?: return TimeHeaderUiMessage(
            id = secondMessage.id,
            timeSent = time,
            displayAsMine = secondMessage.displayAsMine,
            userHandle = secondMessage.userHandle
        )

        if (firstMessage.userHandle != secondMessage.userHandle
            || time.minus(firstMessageTime).seconds > 3.minutes
        ) {
            return TimeHeaderUiMessage(
                id = secondMessage.id,
                timeSent = time,
                displayAsMine = secondMessage.displayAsMine,
                userHandle = secondMessage.userHandle
            )
        }
        return null
    }
}
