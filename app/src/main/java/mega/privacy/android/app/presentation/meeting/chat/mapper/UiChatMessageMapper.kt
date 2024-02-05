package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.app.presentation.meeting.chat.model.messages.CallUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.ContactAttachmentUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.InvalidUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.NodeAttachmentUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.AlterParticipantsUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.ChatLinkCreatedUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.ChatLinkRemovedUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.PermissionChangeUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.PrivateModeSetUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.RetentionTimeUpdatedUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.ScheduledMeetingUpdateUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.TitleChangeUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.TruncateHistoryUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.meta.ChatGiphyUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.meta.ChatRichLinkUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.meta.LocationUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.normal.TextLinkUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.normal.TextUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.normal.VoiceClipUiMessage
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.FormatInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.SignatureInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.management.AlterParticipantsMessage
import mega.privacy.android.domain.entity.chat.messages.management.CallMessage
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
     */
    operator fun invoke(
        message: TypedMessage,
        chatId: Long,
    ): UiChatMessage {
        return when (message) {
            is TextMessage -> TextUiMessage(
                message = message,
            )

            is CallMessage -> CallUiMessage(
                message = message,
            )

            is RichPreviewMessage -> ChatRichLinkUiMessage(
                message = message,
            )

            is GiphyMessage -> ChatGiphyUiMessage(
                message = message,
            )

            is AlterParticipantsMessage -> AlterParticipantsUiMessage(
                message = message,
                showDate = message.shouldShowDate,
            )

            is PermissionChangeMessage -> PermissionChangeUiMessage(
                message = message,
                showDate = message.shouldShowDate,
            )

            is TitleChangeMessage -> TitleChangeUiMessage(
                message = message,
                showDate = message.shouldShowDate,
            )

            is TruncateHistoryMessage -> TruncateHistoryUiMessage(
                message = message,
                showDate = message.shouldShowDate,
            )

            is ChatLinkCreatedMessage -> ChatLinkCreatedUiMessage(
                message = message,
                showDate = message.shouldShowDate,
            )

            is ChatLinkRemovedMessage -> ChatLinkRemovedUiMessage(
                message = message,
                showDate = message.shouldShowDate,
            )

            is ScheduledMeetingUpdatedMessage -> ScheduledMeetingUpdateUiMessage(
                message = message,
                showDate = message.shouldShowDate,
            )

            is PrivateModeSetMessage -> PrivateModeSetUiMessage(
                message = message,
                showDate = message.shouldShowDate,
            )

            is RetentionTimeUpdatedMessage -> RetentionTimeUpdatedUiMessage(
                message = message,
                showDate = message.shouldShowDate,
            )

            is ContactAttachmentMessage -> ContactAttachmentUiMessage(
                message = message,
            )

            is LocationMessage -> LocationUiMessage(
                message = message,
            )

            is TextLinkMessage -> TextLinkUiMessage(
                message = message,
            )

            is VoiceClipMessage -> VoiceClipUiMessage(
                message = message,
                chatId = chatId,
            )

            is NodeAttachmentMessage -> NodeAttachmentUiMessage(
                message = message,
                chatId = chatId,
            )

            is InvalidMessage, is InvalidMetaMessage -> mapInvalidMessage(
                message = message,
            )

            else -> InvalidUiMessage.UnrecognizableInvalidUiMessage(
                message = message,
            )
        }
    }

    private fun mapInvalidMessage(
        message: TypedMessage,
    ): InvalidUiMessage {
        return when (message) {
            is SignatureInvalidMessage -> InvalidUiMessage.SignatureInvalidUiMessage(
                message = message,
            )

            is FormatInvalidMessage -> InvalidUiMessage.FormatInvalidUiMessage(
                message = message,
            )

            is InvalidMetaMessage -> InvalidUiMessage.MetaInvalidUiMessage(
                message = message,
            )

            else -> InvalidUiMessage.UnrecognizableInvalidUiMessage(
                message = message,
            )
        }
    }
}