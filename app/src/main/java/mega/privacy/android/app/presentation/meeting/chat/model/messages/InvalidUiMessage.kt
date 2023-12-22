package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.extension.canForward
import mega.privacy.android.app.presentation.meeting.chat.view.ChatAvatar
import mega.privacy.android.core.ui.controls.chat.messages.ChatErrorBubble
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage

/**
 * Invalid ui message
 *
 * @property message
 */
sealed class InvalidUiMessage : AvatarMessage() {
    internal abstract val message: TypedMessage

    /**
     * Get error message
     *
     * @return the appropriate error message
     */
    @Composable
    abstract fun getErrorMessage(): String

    override val contentComposable: @Composable (RowScope.() -> Unit) = {
        ChatErrorBubble(errorText = getErrorMessage())
    }

    override val avatarComposable: @Composable RowScope.() -> Unit = {
        if (showAvatar) {
            ChatAvatar(modifier = Modifier.align(Alignment.Bottom), handle = message.userHandle)
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
    }

    override val displayAsMine: Boolean
        get() = message.isMine
    override val canForward: Boolean
        get() = message.canForward
    override val timeSent: Long
        get() = message.time

    override val userHandle: Long
        get() = message.userHandle

    override val id: Long
        get() = message.msgId

    /**
     * Format invalid ui message
     *
     * @property message
     * @property showAvatar
     * @property showTime
     * @property showDate
     */
    data class FormatInvalidUiMessage(
        override val message: InvalidMessage,
        override val showAvatar: Boolean,
        override val showTime: Boolean,
        override val showDate: Boolean,
    ) : InvalidUiMessage() {
        @Composable
        override fun getErrorMessage() =
            stringResource(id = R.string.error_message_invalid_format)
    }

    /**
     * Signature invalid ui message
     *
     * @property message
     * @property showAvatar
     * @property showTime
     * @property showDate
     */
    data class SignatureInvalidUiMessage(
        override val message: InvalidMessage,
        override val showAvatar: Boolean,
        override val showTime: Boolean,
        override val showDate: Boolean,
    ) : InvalidUiMessage() {

        @Composable
        override fun getErrorMessage() =
            stringResource(id = R.string.error_message_invalid_signature)
    }

    /**
     * Unrecognizable invalid ui message
     *
     * @property message
     * @property showAvatar
     * @property showTime
     * @property showDate
     */
    data class UnrecognizableInvalidUiMessage(
        override val message: TypedMessage,
        override val showAvatar: Boolean,
        override val showTime: Boolean,
        override val showDate: Boolean,
    ) : InvalidUiMessage() {

        @Composable
        override fun getErrorMessage() =
            stringResource(id = R.string.error_message_unrecognizable)
    }
}