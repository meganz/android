package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs

/**
 * Scan message mapper
 *
 * @property uiChatMessageMapper
 */
class ScanMessageMapper @Inject constructor(
    private val uiChatMessageMapper: UiChatMessageMapper,
) {
    /**
     * Invoke
     *
     * When new message comes, we only need to check the latest message in the list. and update it.
     */
    operator fun invoke(
        isOneToOne: Boolean,
        currentItems: List<UiChatMessage>,
        newMessage: TypedMessage,
    ): List<UiChatMessage> {
        val latestMessage = currentItems.firstOrNull()
        val newUiMessage = uiChatMessageMapper(
            message = newMessage,
            isOneToOne = isOneToOne,
            showAvatar = !newMessage.isMine && (newMessage.userHandle != latestMessage?.message?.userHandle || latestMessage.avatarComposable == null),
            showTime = true,
            showDate = true,
        )
        return if (latestMessage == null) {
            listOf(newUiMessage)
        } else {
            val latestUiMessage = uiChatMessageMapper(
                message = latestMessage.message,
                isOneToOne = isOneToOne,
                showAvatar = latestMessage.showAvatar,
                showTime = shouldShowTime(latestMessage.message, newUiMessage.message),
                showDate = shouldShowDate(latestMessage.message, newUiMessage.message),
            )
            listOf(newUiMessage) + latestUiMessage + currentItems.drop(1)
        }
    }

    private fun shouldShowTime(
        latestMessage: TypedMessage,
        newMessage: TypedMessage,
    ): Boolean {
        return abs(latestMessage.time - newMessage.time) >= TimeUnit.MINUTES.toSeconds(3)
                || latestMessage.userHandle != newMessage.userHandle
    }

    private fun shouldShowDate(
        latestMessage: TypedMessage,
        newMessage: TypedMessage,
    ): Boolean {
        val newMessageCal = Calendar.getInstance().apply {
            timeInMillis = TimeUnit.SECONDS.toMillis(newMessage.time)
        }
        val latestMessageCal = Calendar.getInstance().apply {
            timeInMillis = TimeUnit.SECONDS.toMillis(latestMessage.time)
        }
        return newMessageCal.get(Calendar.DATE) != latestMessageCal.get(Calendar.DATE)
    }
}