package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.header.DateHeaderUiMessage
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Chat message date separator mapper
 *
 * @constructor Create empty Chat message date separator mapper
 */
class ChatMessageDateSeparatorMapper @Inject constructor() {
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
        val firstMessageTime = firstMessage?.timeSent ?: return DateHeaderUiMessage(time)

        if (getDayOfYear(time) != getDayOfYear(firstMessageTime)) {
            return DateHeaderUiMessage(time)
        }
        return null
    }

    private fun getDayOfYear(time: Long) = LocalDateTime.ofEpochSecond(
        time,
        0,
        ZoneOffset.UTC
    ).dayOfYear
}
