package mega.privacy.android.data.mapper.chat.paging

import mega.privacy.android.data.database.entity.chat.TypedMessageEntity
import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import javax.inject.Inject

/**
 * Typed message entity mapper
 */
class TypedMessageEntityMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param request
     * @param chatId
     * @return TypedMessageEntity
     */
    operator fun invoke(request: CreateTypedMessageRequest, chatId: Long) =
        with(request) {
            TypedMessageEntity(
                chatId = chatId,
                status = status,
                msgId = msgId,
                tempId = tempId,
                msgIndex = msgIndex,
                userHandle = userHandle,
                type = type,
                hasConfirmedReactions = hasConfirmedReactions,
                timestamp = timestamp,
                content = content,
                isEdited = isEdited,
                isDeleted = isDeleted,
                isEditable = isEditable,
                isDeletable = isDeletable,
                isManagementMessage = isManagementMessage,
                handleOfAction = handleOfAction,
                privilege = privilege,
                code = code,
                usersCount = usersCount,
                userHandles = userHandles,
                userNames = userNames,
                userEmails = userEmails,
                handleList = handleList,
                duration = duration,
                retentionTime = retentionTime,
                termCode = termCode,
                rowId = rowId,
                changes = changes,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                shouldShowDate = shouldShowDate,
                isMine = isMine,
                textMessage = textMessage
            )
        }
}