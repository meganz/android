package mega.privacy.android.data.database.entity.chat

import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.domain.entity.chat.PendingMessageState

/**
 * Pending message entity
 *
 * @property pendingMessageId
 * @property chatId
 * @property type
 * @property uploadTimestamp
 * @property state
 * @property tempIdKarere
 * @property videoDownSampled
 * @property filePath
 * @property nodeHandle
 * @property fingerprint
 * @property name
 * @property transferTag
 */
@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val pendingMessageId: Long? = null,
    val chatId: Long,
    val type: Int,
    val uploadTimestamp: Long,
    val state: PendingMessageState,
    val tempIdKarere: Long,
    val videoDownSampled: String?,
    val filePath: String,
    val nodeHandle: Long,
    val fingerprint: String?,
    val name: String?,
    val transferTag: Int,
)