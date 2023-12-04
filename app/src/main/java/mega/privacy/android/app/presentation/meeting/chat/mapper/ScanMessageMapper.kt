package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.app.presentation.meeting.chat.model.ui.UiChatMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import javax.inject.Inject

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
            showAvatar = !newMessage.isMine && (newMessage.userHandle != latestMessage?.message?.userHandle || latestMessage.avatarComposable == null)
        )
        return if (latestMessage == null) {
            listOf(newUiMessage)
        } else {
            val latestUiMessage = uiChatMessageMapper(
                message = latestMessage.message,
                isOneToOne = isOneToOne,
                showAvatar = latestMessage.showAvatar
            )
            listOf(newUiMessage) + latestUiMessage + currentItems.drop(1)
        }
    }
}