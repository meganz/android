package mega.privacy.android.data.database.entity.chat

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Meta typed message request
 *
 * @property typedMessageEntity Typed message request entity
 * @property nodeList List of nodes
 * @property richPreviewEntity Rich preview entity
 * @property geolocationEntity Chat geolocation entity
 * @property giphyEntity Giphy entity
 */
data class MetaTypedMessageEntity(
    @Embedded val typedMessageEntity: TypedMessageEntity,
    @Relation(
        parentColumn = "messageId",
        entityColumn = "id",
        associateBy = Junction(NodeMessageCrossRef::class)
    )
    val nodeList: List<ChatNodeEntity>,
    @Relation(
        parentColumn = "messageId",
        entityColumn = "messageId",
        entity = RichPreviewEntity::class
    )
    val richPreviewEntity: RichPreviewEntity?,
    @Relation(
        parentColumn = "messageId",
        entityColumn = "messageId",
        entity = ChatGeolocationEntity::class
    )
    val geolocationEntity: ChatGeolocationEntity?,
    @Relation(
        parentColumn = "messageId",
        entityColumn = "messageId",
        entity = GiphyEntity::class
    )
    val giphyEntity: GiphyEntity?,
)