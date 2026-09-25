package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.management.CallEndedMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import javax.inject.Inject

internal class CreateCallEndedMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override suspend fun invoke(request: CreateTypedMessageInfo) =
        with(request) {
            CallEndedMessage(
                chatId = chatId,
                msgId = messageId,
                time = timestamp,
                isDeletable = isDeletable,
                isEditable = isEditable,
                isMine = isMine,
                userHandle = userHandle,
                termCode = termCode,
                duration = duration,

                reactions = reactions,
                status = status,
                content = content,
            )
        }
}