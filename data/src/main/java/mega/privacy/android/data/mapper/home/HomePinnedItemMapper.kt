package mega.privacy.android.data.mapper.home

import mega.privacy.android.data.database.entity.HomePinnedItemEntity
import mega.privacy.android.domain.entity.home.PinnedHomeItem
import mega.privacy.android.domain.entity.node.NodeId
import javax.inject.Inject

/**
 * Mapper for [PinnedHomeItem] between data and domain layers
 */
internal class HomePinnedItemMapper @Inject constructor() {

    /**
     * Map from data entity to domain model
     */
    operator fun invoke(entity: HomePinnedItemEntity): PinnedHomeItem =
        PinnedHomeItem(
            nodeId = NodeId(entity.nodeHandle),
            name = entity.nodeName,
            isFolder = entity.isFolder,
            pinnedAt = entity.pinnedAt,
        )

    /**
     * Map from domain model to data entity
     */
    operator fun invoke(domain: PinnedHomeItem): HomePinnedItemEntity =
        HomePinnedItemEntity(
            nodeHandle = domain.nodeId.longValue,
            nodeName = domain.name,
            isFolder = domain.isFolder,
            pinnedAt = domain.pinnedAt,
        )
}
