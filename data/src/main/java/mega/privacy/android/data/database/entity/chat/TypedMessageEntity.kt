package mega.privacy.android.data.database.entity.chat

import androidx.room.Entity
import androidx.room.TypeConverters
import mega.privacy.android.data.database.converter.TypedMessageEntityConverters
import mega.privacy.android.data.database.entity.chat.TypedMessageEntity.Companion.DEFAULT_ID
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatMessageChange
import mega.privacy.android.domain.entity.chat.ChatMessageCode
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ChatMessageTermCode
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.ChatMessageInfo
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import kotlin.time.Duration

/**
 * Entity to store a typed message request.
 *
 * This entity has a composite primary key ["msgId", "pendingMessageId", "isPending"] to avoid id clashes between
 * ordinary messages (where the id comes from the sdk and can also be [DEFAULT_ID])
 * and pending messages (where the id is set locally by autoincrement and can't be [DEFAULT_ID]).
 *
 * @property chatId Chat ID.
 * @property status Status of the message.
 * @property msgId Message ID. It comes from SDK or [DEFAULT_ID] if this is a pending message
 * @property pendingMessageId Pending Message ID or [DEFAULT_ID] if this is NOT a pending message
 * @property tempId Temporary ID.
 * @property msgIndex Message index.
 * @property userHandle User handle.
 * @property type Type of the message.
 * @property hasConfirmedReactions True if the message has confirmed reactions, false otherwise.
 * @property timestamp Timestamp of the message.
 * @property content Content of the message.
 * @property isEdited True if the message is edited, false otherwise.
 * @property isDeleted True if the message is deleted, false otherwise.
 * @property isEditable True if the message is editable, false otherwise.
 * @property isDeletable True if the message is deletable, false otherwise.
 * @property isManagementMessage True if the message is a management message, false otherwise.
 * @property handleOfAction Handle of the action.
 * @property privilege Privilege of the message.
 * @property code Code of the message.
 * @property usersCount Number of users.
 * @property userHandles List of user handles.
 * @property userNames List of user names.
 * @property userEmails List of user emails.
 * @property handleList List of handles.
 * @property duration Duration of the message.
 * @property retentionTime Retention time of the message.
 * @property termCode Term code of the message.
 * @property rowId Row ID.
 * @property changes List of changes.
 * @property isMine True if the message is mine, false otherwise.
 * @property shouldShowAvatar True if the avatar should be shown, false otherwise.
 * @property shouldShowTime True if the time should be shown, false otherwise.
 * @property textMessage Text message.
 * @property reactions list of [Reaction]
 */
@Entity(tableName = "typed_messages", primaryKeys = ["msgId", "pendingMessageId"])
@TypeConverters(TypedMessageEntityConverters::class)
data class TypedMessageEntity(
    override val msgId: Long,
    val pendingMessageId: Long = DEFAULT_ID,
    val chatId: Long,
    override val status: ChatMessageStatus,
    override val tempId: Long,
    override val msgIndex: Int,
    override val userHandle: Long,
    override val type: ChatMessageType,
    override val hasConfirmedReactions: Boolean,
    override val timestamp: Long,
    override val content: String?,
    override val isEdited: Boolean,
    override val isDeleted: Boolean,
    override val isEditable: Boolean,
    override val isDeletable: Boolean,
    override val isManagementMessage: Boolean,
    override val handleOfAction: Long,
    override val privilege: ChatRoomPermission,
    override val code: ChatMessageCode,
    override val usersCount: Long,
    override val userHandles: List<Long>,
    override val userNames: List<String>,
    override val userEmails: List<String>,
    override val handleList: List<Long>,
    override val duration: Duration,
    override val retentionTime: Long,
    override val termCode: ChatMessageTermCode,
    override val rowId: Long,
    override val changes: List<ChatMessageChange>,
    val isMine: Boolean,
    val shouldShowAvatar: Boolean,
    val shouldShowTime: Boolean,
    val textMessage: String?,
    val reactions: List<Reaction>
) : ChatMessageInfo {

    companion object {

        /**
         * Id to be used when there's no id as SQLite doesn't allow nulls in primary keys
         *
         * It should not be used to query pending messages as we can have ordinary messages with this id
         * Auto generated keys are strictly positive, so pending messages don't have this value for pendingMessageId
         */
        internal const val DEFAULT_ID = 0L
    }
}

