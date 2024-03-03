package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.chat.messages.ChatErrorBubble
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage

/**
 * Invalid ui message
 *
 * @property message
 */
sealed class InvalidUiMessage : AvatarMessage() {

    /**
     * Get error message
     *
     * @return the appropriate error message
     */
    @Composable
    abstract fun getErrorMessage(): String

    @Composable
    override fun ContentComposable(
        onLongClick: (TypedMessage) -> Unit,
        interactionEnabled: Boolean
    ) {
        ChatErrorBubble(errorText = getErrorMessage())
    }

    override val showAvatar: Boolean
        get() = message.shouldShowAvatar

    override val displayAsMine: Boolean
        get() = message.isMine
    override val shouldDisplayForwardIcon = false
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
     */
    data class FormatInvalidUiMessage(
        override val message: InvalidMessage,
        override val reactions: List<UIReaction>,
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
     */
    data class SignatureInvalidUiMessage(
        override val message: InvalidMessage,
        override val reactions: List<UIReaction>,
    ) : InvalidUiMessage() {

        @Composable
        override fun getErrorMessage() =
            stringResource(id = R.string.error_message_invalid_signature)
    }

    /**
     * Invalid meta ui message
     *
     * @property message
     * @property showAvatar
     * @property showTime
     */
    data class MetaInvalidUiMessage(
        override val message: TypedMessage,
        override val reactions: List<UIReaction>,
    ) : InvalidUiMessage() {

        @Composable
        override fun getErrorMessage() =
            stringResource(id = R.string.error_meta_message_invalid)
    }

    /**
     * Unrecognizable invalid ui message
     *
     * @property message
     * @property showAvatar
     * @property showTime
     */
    data class UnrecognizableInvalidUiMessage(
        override val message: TypedMessage,
        override val reactions: List<UIReaction>,
    ) : InvalidUiMessage() {

        @Composable
        override fun getErrorMessage() =
            stringResource(id = R.string.error_message_unrecognizable)
    }

}