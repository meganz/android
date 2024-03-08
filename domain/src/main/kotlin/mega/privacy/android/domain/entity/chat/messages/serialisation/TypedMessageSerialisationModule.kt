package mega.privacy.android.domain.entity.chat.messages.serialisation

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingVoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.FormatInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.SignatureInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.UnrecognizableInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.management.AlterParticipantsMessage
import mega.privacy.android.domain.entity.chat.messages.management.CallEndedMessage
import mega.privacy.android.domain.entity.chat.messages.management.CallStartedMessage
import mega.privacy.android.domain.entity.chat.messages.management.ChatLinkCreatedMessage
import mega.privacy.android.domain.entity.chat.messages.management.ChatLinkRemovedMessage
import mega.privacy.android.domain.entity.chat.messages.management.PermissionChangeMessage
import mega.privacy.android.domain.entity.chat.messages.management.PrivateModeSetMessage
import mega.privacy.android.domain.entity.chat.messages.management.RetentionTimeUpdatedMessage
import mega.privacy.android.domain.entity.chat.messages.management.ScheduledMeetingUpdatedMessage
import mega.privacy.android.domain.entity.chat.messages.management.TitleChangeMessage
import mega.privacy.android.domain.entity.chat.messages.management.TruncateHistoryMessage
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.InvalidMetaMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextLinkMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage

/**
 * TypedMessageSerialisationModule
 */
val typedMessageSerialisationModule = SerializersModule {
    polymorphic(TypedMessage::class) {
        subclass(ContactAttachmentMessage::class)
        subclass(VoiceClipMessage::class)
        subclass(NodeAttachmentMessage::class)
        subclass(FormatInvalidMessage::class)
        subclass(SignatureInvalidMessage::class)
        subclass(UnrecognizableInvalidMessage::class)
        subclass(AlterParticipantsMessage::class)
        subclass(ChatLinkCreatedMessage::class)
        subclass(ChatLinkRemovedMessage::class)
        subclass(PermissionChangeMessage::class)
        subclass(PrivateModeSetMessage::class)
        subclass(RetentionTimeUpdatedMessage::class)
        subclass(ScheduledMeetingUpdatedMessage::class)
        subclass(TitleChangeMessage::class)
        subclass(TruncateHistoryMessage::class)
        subclass(CallEndedMessage::class)
        subclass(CallStartedMessage::class)
        subclass(GiphyMessage::class)
        subclass(InvalidMetaMessage::class)
        subclass(LocationMessage::class)
        subclass(RichPreviewMessage::class)
        subclass(TextLinkMessage::class)
        subclass(TextMessage::class)
        subclass(PendingFileAttachmentMessage::class)
        subclass(PendingVoiceClipMessage::class)
    }
}