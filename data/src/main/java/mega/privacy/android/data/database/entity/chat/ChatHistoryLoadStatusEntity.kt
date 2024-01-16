package mega.privacy.android.data.database.entity.chat

import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus

/**
 * Entity to store the status of the chat history load.
 *
 * @property chatId Chat ID.
 * @property status Status of the chat history load.
 */
@Entity(tableName = "chat_history_load_status")
data class ChatHistoryLoadStatusEntity(
    @PrimaryKey val chatId: Long,
    val status: ChatHistoryLoadStatus,
)