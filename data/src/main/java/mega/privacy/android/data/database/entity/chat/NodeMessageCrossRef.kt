package mega.privacy.android.data.database.entity.chat

import androidx.room.Entity
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Node message cross ref
 *
 * @property messageId
 * @property id
 */
@Entity(
    tableName = "node_message_cross_ref",
    primaryKeys = ["messageId", "id"],
)
data class NodeMessageCrossRef(
    val messageId: Long,
    val id: NodeId,
)