package mega.privacy.android.data.database.entity.chat

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Meta typed message request
 *
 * @property typedMessageEntity Typed message request entity
 * @property nodeList List of nodes
 * @property richPreviewEntity Rich preview entity
 * @property chatGeolocationEntity Chat geolocation entity
 * @property giphyEntity Giphy entity
 */
data class MetaTypedMessageEntity(
    @Embedded val typedMessageEntity: TypedMessageEntity,
    @Relation(
        parentColumn = "msgId",
        entityColumn = "messageId",
        entity = ChatNodeEntity::class
    )
    val nodeList: List<ChatNodeEntity>,
    @Relation(
        parentColumn = "msgId",
        entityColumn = "messageId",
        entity = RichPreviewEntity::class
    )
    val richPreviewEntity: RichPreviewEntity?,
    @Relation(
        parentColumn = "msgId",
        entityColumn = "messageId",
        entity = ChatGeolocationEntity::class
    )
    val chatGeolocationEntity: ChatGeolocationEntity?,
    @Relation(
        parentColumn = "msgId",
        entityColumn = "messageId",
        entity = GiphyEntity::class
    )
    val giphyEntity: GiphyEntity?,
)