package mega.privacy.android.data.database.entity.chat

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import mega.privacy.android.data.database.converter.ChatNodeEntityConverters
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Entity to store a chat node.
 *
 * @property id ID.
 * @property messageId Message ID.
 * @property name Name.
 * @property parentId Parent ID.
 * @property base64Id Base 64 ID.
 * @property label Label.
 * @property isFavourite True if the node is favourite, false otherwise.
 * @property exportedData Exported data.
 * @property isTakenDown True if the node is taken down, false otherwise.
 * @property isIncomingShare True if the node is an incoming share, false otherwise.
 * @property isNodeKeyDecrypted True if the node key is decrypted, false otherwise.
 * @property creationTime Creation time.
 * @property serializedData Serialized data.
 * @property isAvailableOffline True if the node is available offline, false otherwise.
 * @property versionCount Version count.
 */
@Entity(tableName = "chat_node")
@TypeConverters(ChatNodeEntityConverters::class)
data class ChatNodeEntity(
    @PrimaryKey override val id: NodeId,
    val messageId: Long,
    override val name: String,
    override val parentId: NodeId,
    override val base64Id: String,
    override val restoreId: NodeId?,
    override val label: Int,
    override val isFavourite: Boolean,
    @Embedded override val exportedData: ExportedData?,
    override val isTakenDown: Boolean,
    override val isIncomingShare: Boolean,
    override val isNodeKeyDecrypted: Boolean,
    override val creationTime: Long,
    override val serializedData: String?,
    override val isAvailableOffline: Boolean,
    override val versionCount: Int,
    override val size: Long,
    override val modificationTime: Long,
    override val type: FileTypeInfo,
    override val thumbnailPath: String?,
    override val previewPath: String?,
    override val fullSizePath: String?,
    override val fingerprint: String?,
    override val originalFingerprint: String?,
    override val hasThumbnail: Boolean,
    override val hasPreview: Boolean,
) : FileNode

