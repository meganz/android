package mega.privacy.android.app.presentation.meeting.chat.extension

import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.management.ManagementMessage
import mega.privacy.android.domain.entity.chat.messages.meta.MetaMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage

/**
 * Is selectable
 */
val TypedMessage.isSelectable: Boolean
    get() = when (this) {
        is NormalMessage, is MetaMessage, is VoiceClipMessage, is ContactAttachmentMessage, is NodeAttachmentMessage -> true
        else -> false
    }

/**
 * Is selectable
 */
val TypedMessage.canForward: Boolean
    get() = when (this) {
        is RichPreviewMessage -> true // add more types next MR
        else -> false
    }

/**
 * Check if the message has a avatar
 */
val TypedMessage.hasAvatar: Boolean
    get() = when (this) {
        is ManagementMessage -> false
        else -> true
    }