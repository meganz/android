package mega.privacy.android.app.presentation.meeting.chat.model.messages.header

import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction

/**
 * Header message
 */
abstract class HeaderMessage() : UiChatMessage {
    override val timeSent: Long? = null
    override val id = -1L
    override val displayAsMine = false
    override val shouldDisplayForwardIcon = false
    override val userHandle = -1L
    override val reactions = emptyList<UIReaction>()
    override val isSelectable = false
    override val message: TypedMessage? = null
}