package mega.privacy.android.app.presentation.meeting.chat.extension

import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.meta.MetaMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage

val TypedMessage.isSelectable: Boolean
    get() = when (this) {
        is NormalMessage, is MetaMessage, is VoiceClipMessage, is ContactAttachmentMessage, is NodeAttachmentMessage -> true
        else -> false
    }