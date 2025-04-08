package mega.privacy.android.data.database.entity.chat

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.domain.entity.chat.PendingMessageState

/**
 * Pending message entity
 *
 * @property pendingMessageId
 * @property transferUniqueId
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
 * @property originalUriPath
 */
@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val pendingMessageId: Long? = null,
    @ColumnInfo("transferUniqueId") val transferUniqueId: Long?,
    val chatId: Long,
    val type: Int,
    val uploadTimestamp: Long,
    val state: PendingMessageState,
    val tempIdKarere: Long,
    val videoDownSampled: String?,
    @ColumnInfo(name = "filePath")
    val filePath: String,
    val nodeHandle: Long,
    val fingerprint: String?,
    val name: String?,
    @ColumnInfo(name = "original_uri_path", defaultValue = "")
    val originalUriPath: String,
)