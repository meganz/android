package mega.privacy.android.app.presentation.meeting.chat.mapper

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.model.ui.CallUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.ChatRichLinkUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.InvalidUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.TextUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.UiChatMessage
import mega.privacy.android.domain.entity.chat.messages.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.management.CallMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import javax.inject.Inject

/**
 * Mapper to convert a [TypedMessage] to a [UiChatMessage]
 *
 */
class UiChatMessageMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param message
     * @param isOneToOne
     */
    operator fun invoke(
        message: TypedMessage,
        isOneToOne: Boolean,
        showAvatar: Boolean,
        showTime: Boolean,
        showDate: Boolean,
    ): UiChatMessage {
        return when (message) {
            is TextMessage -> TextUiMessage(
                message = message,
                showAvatar = showAvatar,
                showTime = showTime,
                showDate = showDate
            )

            is CallMessage -> CallUiMessage(
                message = message,
                isOneToOneChat = isOneToOne,
                showDate = showDate
            )

            is RichPreviewMessage -> ChatRichLinkUiMessage(
                message = message,
                showDate = showDate,
                showAvatar = showAvatar,
                showTime = showTime
            )

            is InvalidMessage -> InvalidUiMessage(
                message = message,
                showAvatar = showAvatar,
                showTime = showTime,
                showDate = showDate
            )

            else -> object : UiChatMessage {
                override val contentComposable: @Composable (RowScope.() -> Unit) = {

                }
                override val avatarComposable: @Composable (RowScope.() -> Unit)? = null

                override val message: TypedMessage = message

                override val showAvatar: Boolean = false

                override val showTime: Boolean = true

                override val showDate: Boolean = showDate
            }
        }
    }
}