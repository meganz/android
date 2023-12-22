package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

import mega.privacy.android.app.presentation.meeting.chat.extension.canForward
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.domain.entity.chat.messages.management.ManagementMessage

/**
 * Management ui chat message
 */
abstract class ManagementUiChatMessage : UiChatMessage {
    /**
     * Message
     */
    abstract val message: ManagementMessage

    override val avatarComposable = null

    override val showAvatar: Boolean = false

    override val showTime: Boolean = true

    override val showDate: Boolean = true

    override val displayAsMine = false

    override val canForward: Boolean
        get() = message.canForward

    override val timeSent: Long
        get() = message.time

    override val userHandle: Long?
        get() = message.userHandle

    override val id: Long
        get() = message.msgId
}