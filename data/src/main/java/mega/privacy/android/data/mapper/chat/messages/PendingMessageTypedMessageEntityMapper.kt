package mega.privacy.android.data.mapper.chat.messages

import mega.privacy.android.data.database.entity.chat.TypedMessageEntity
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatMessageCode
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ChatMessageTermCode
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.pending.SavePendingMessageRequest
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Pending message typed message entity mapper
 *
 * @constructor Create empty Pending message typed message entity mapper
 */
class PendingMessageTypedMessageEntityMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param savePendingMessageRequest
     * @return
     */
    operator fun invoke(
        savePendingMessageRequest: SavePendingMessageRequest,
    ): TypedMessageEntity {
        return TypedMessageEntity(
            msgId = TypedMessageEntity.DEFAULT_ID,
            chatId = savePendingMessageRequest.chatId,
            status = ChatMessageStatus.UNKNOWN,
            tempId = -1,
            msgIndex = -1,
            userHandle = -1,
            type = ChatMessageType.NODE_ATTACHMENT,
            hasConfirmedReactions = false,
            timestamp = Long.MAX_VALUE,
            content = null,
            isEdited = false,
            isDeleted = false,
            isEditable = false,
            isDeletable = false,
            isManagementMessage = false,
            handleOfAction = -1,
            privilege = ChatRoomPermission.Unknown,
            code = ChatMessageCode.UNKNOWN,
            usersCount = -1,
            userHandles = emptyList(),
            userNames = emptyList(),
            userEmails = emptyList(),
            handleList = emptyList(),
            duration = Duration.ZERO,
            retentionTime = -1,
            termCode = ChatMessageTermCode.BY_MODERATOR,
            rowId = -1,
            changes = emptyList(),
            isMine = true,
            shouldShowAvatar = false,
            shouldShowTime = false,
            textMessage = null,
            reactions = emptyList(),
        )
    }
}
