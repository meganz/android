package mega.privacy.android.app.presentation.meeting.chat.extension

import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.MetaMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.normal.ChatLinkMessage
import mega.privacy.android.domain.entity.chat.messages.normal.FileLinkMessage
import mega.privacy.android.domain.entity.chat.messages.normal.FolderLinkMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextLinkMessage

/**
 * Is selectable
 */
val TypedMessage.isSelectable: Boolean
    get() = when (this) {
        is NormalMessage, is MetaMessage, is VoiceClipMessage, is ContactAttachmentMessage, is NodeAttachmentMessage -> true
        else -> false
    }

/**
 * can forward
 */
val TypedMessage.canForward: Boolean
    get() = when (this) {
        is RichPreviewMessage, is GiphyMessage, is LocationMessage, is ChatLinkMessage,
        is TextLinkMessage, is FolderLinkMessage, is FileLinkMessage, is ContactAttachmentMessage,
        is NodeAttachmentMessage, is VoiceClipMessage,
        -> true

        else -> false
    }

/**
 * Can long click
 */
val TypedMessage.canLongClick: Boolean
    get() = when (this) {
        is RichPreviewMessage, is GiphyMessage, is LocationMessage, is NormalMessage,
        is ContactAttachmentMessage, is NodeAttachmentMessage, is VoiceClipMessage,
        -> true

        else -> false
    }