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
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
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
class UiChatMessageMapper @Inject constructor(
    private val uiReactionListMapper: UiReactionListMapper,
) {
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
                reactions = uiReactionListMapper(message.reactions),
            )

            is CallMessage -> CallUiMessage(
                message = message,
                reactions = uiReactionListMapper(message.reactions),
            )

            is RichPreviewMessage -> ChatRichLinkUiMessage(
                message = message,
                reactions = uiReactionListMapper(message.reactions),
            )

            is GiphyMessage -> ChatGiphyUiMessage(
                message = message,
                reactions = uiReactionListMapper(message.reactions),
            )

            is AlterParticipantsMessage -> AlterParticipantsUiMessage(
                message = message,
                showDate = message.shouldShowDate,
                reactions = uiReactionListMapper(message.reactions),
            )

            is PermissionChangeMessage -> PermissionChangeUiMessage(
                message = message,
                showDate = message.shouldShowDate,
                reactions = uiReactionListMapper(message.reactions),
            )

            is TitleChangeMessage -> TitleChangeUiMessage(
                message = message,
                showDate = message.shouldShowDate,
                reactions = uiReactionListMapper(message.reactions),
            )

            is TruncateHistoryMessage -> TruncateHistoryUiMessage(
                message = message,
                showDate = message.shouldShowDate,
                reactions = uiReactionListMapper(message.reactions),
            )

            is ChatLinkCreatedMessage -> ChatLinkCreatedUiMessage(
                message = message,
                showDate = message.shouldShowDate,
                reactions = uiReactionListMapper(message.reactions),
            )

            is ChatLinkRemovedMessage -> ChatLinkRemovedUiMessage(
                message = message,
                showDate = message.shouldShowDate,
                reactions = uiReactionListMapper(message.reactions),
            )

            is ScheduledMeetingUpdatedMessage -> ScheduledMeetingUpdateUiMessage(
                message = message,
                showDate = message.shouldShowDate,
                reactions = uiReactionListMapper(message.reactions),
            )

            is PrivateModeSetMessage -> PrivateModeSetUiMessage(
                message = message,
                showDate = message.shouldShowDate,
                reactions = uiReactionListMapper(message.reactions),
            )

            is RetentionTimeUpdatedMessage -> RetentionTimeUpdatedUiMessage(
                message = message,
                showDate = message.shouldShowDate,
                reactions = uiReactionListMapper(message.reactions),
            )

            is ContactAttachmentMessage -> ContactAttachmentUiMessage(
                message = message,
                reactions = uiReactionListMapper(message.reactions),
            )

            is LocationMessage -> LocationUiMessage(
                message = message,
                reactions = uiReactionListMapper(message.reactions),
            )

            is TextLinkMessage -> TextLinkUiMessage(
                message = message,
                reactions = uiReactionListMapper(message.reactions),
            )

            is VoiceClipMessage -> VoiceClipUiMessage(
                message = message,
                chatId = chatId,
                reactions = uiReactionListMapper(message.reactions),
            )

            is NodeAttachmentMessage -> NodeAttachmentUiMessage(
                message = message,
                chatId = chatId,
                reactions = uiReactionListMapper(message.reactions)
            )

            is InvalidMessage, is InvalidMetaMessage -> mapInvalidMessage(
                message = message,
                reactions = uiReactionListMapper(message.reactions),
            )

            else -> InvalidUiMessage.UnrecognizableInvalidUiMessage(
                message = message,
                reactions = uiReactionListMapper(message.reactions),
            )
        }
    }

    private fun mapInvalidMessage(
        message: TypedMessage,
        reactions: List<UIReaction>,
    ): InvalidUiMessage {
        return when (message) {
            is SignatureInvalidMessage -> InvalidUiMessage.SignatureInvalidUiMessage(
                message = message,
                reactions = reactions,
            )

            is FormatInvalidMessage -> InvalidUiMessage.FormatInvalidUiMessage(
                message = message,
                reactions = reactions,
            )

            is InvalidMetaMessage -> InvalidUiMessage.MetaInvalidUiMessage(
                message = message,
                reactions = reactions,
            )

            else -> InvalidUiMessage.UnrecognizableInvalidUiMessage(
                message = message,
                reactions = reactions,
            )
        }
    }
}