package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.app.presentation.meeting.chat.model.messages.ContactAttachmentUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.InvalidUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.AlterParticipantsUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.CallUiMessage
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
import mega.privacy.android.app.presentation.meeting.chat.model.messages.normal.TextUiMessage
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
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
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
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
        showAvatar: Boolean,
        showTime: Boolean,
        showDate: Boolean,
    ): UiChatMessage {
        return when (message) {
            is TextMessage -> TextUiMessage(
                message = message,
                showAvatar = showAvatar,
                showTime = showTime,
                showDate = showDate
            )

            is CallMessage -> CallUiMessage(
                message = message,
                showDate = showDate
            )

            is RichPreviewMessage -> ChatRichLinkUiMessage(
                message = message,
                showDate = showDate,
                showAvatar = showAvatar,
                showTime = showTime
            )

            is GiphyMessage -> ChatGiphyUiMessage(
                message = message,
                showDate = showDate,
                showAvatar = showAvatar,
                showTime = showTime
            )

            is AlterParticipantsMessage -> AlterParticipantsUiMessage(
                message = message,
                showDate = showDate,
            )

            is PermissionChangeMessage -> PermissionChangeUiMessage(
                message = message,
                showDate = showDate
            )

            is TitleChangeMessage -> TitleChangeUiMessage(
                message = message,
                showDate = showDate
            )

            is TruncateHistoryMessage -> TruncateHistoryUiMessage(
                message = message,
                showDate = showDate
            )

            is ChatLinkCreatedMessage -> ChatLinkCreatedUiMessage(
                message = message,
                showDate = showDate
            )

            is ChatLinkRemovedMessage -> ChatLinkRemovedUiMessage(
                message = message,
                showDate = showDate
            )

            is ScheduledMeetingUpdatedMessage -> ScheduledMeetingUpdateUiMessage(
                message = message,
                showDate = showDate
            )

            is PrivateModeSetMessage -> PrivateModeSetUiMessage(
                message = message,
                showDate = showDate
            )

            is RetentionTimeUpdatedMessage -> RetentionTimeUpdatedUiMessage(
                message = message,
                showDate = showDate
            )

            is ContactAttachmentMessage -> ContactAttachmentUiMessage(
                message = message,
                showDate = showDate,
                showAvatar = showAvatar,
                showTime = showTime
            )

            is LocationMessage -> LocationUiMessage(
                message = message,
                showDate = showDate,
                showTime = showTime,
                showAvatar = showAvatar
            )

            is InvalidMessage -> mapInvalidMessage(message, showAvatar, showTime, showDate)

            else -> InvalidUiMessage.UnrecognizableInvalidUiMessage(
                message = message,
                showAvatar = showAvatar,
                showTime = showTime,
                showDate = showDate,
            )
        }
    }

    private fun mapInvalidMessage(
        message: InvalidMessage,
        showAvatar: Boolean,
        showTime: Boolean,
        showDate: Boolean,
    ): InvalidUiMessage {
        return when (message) {
            is SignatureInvalidMessage -> InvalidUiMessage.SignatureInvalidUiMessage(
                message = message,
                showAvatar = showAvatar,
                showTime = showTime,
                showDate = showDate
            )

            is FormatInvalidMessage -> InvalidUiMessage.FormatInvalidUiMessage(
                message = message,
                showAvatar = showAvatar,
                showTime = showTime,
                showDate = showDate
            )

            else -> InvalidUiMessage.UnrecognizableInvalidUiMessage(
                message = message,
                showAvatar = showAvatar,
                showTime = showTime,
                showDate = showDate
            )
        }
    }
}