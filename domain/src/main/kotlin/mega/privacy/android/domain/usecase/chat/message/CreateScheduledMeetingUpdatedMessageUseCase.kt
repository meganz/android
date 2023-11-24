package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.management.ScheduledMeetingUpdatedMessage
import javax.inject.Inject


internal class CreateScheduledMeetingUpdatedMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = ScheduledMeetingUpdatedMessage(
        message.msgId,
        message.timestamp,
        isMine = isMine
    )
}