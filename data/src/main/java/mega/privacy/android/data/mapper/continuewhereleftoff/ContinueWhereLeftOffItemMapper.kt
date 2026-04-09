package mega.privacy.android.data.mapper.continuewhereleftoff

import mega.privacy.android.data.database.entity.RecentlyUsedEntity
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import javax.inject.Inject

/**
 * Maps [RecentlyUsedEntity] to [ContinueWhereLeftOffItem].
 */
internal class ContinueWhereLeftOffItemMapper @Inject constructor(
    private val recentlyUsedTypeIdMapper: RecentlyUsedTypeIdMapper,
) {
    operator fun invoke(entity: RecentlyUsedEntity): ContinueWhereLeftOffItem =
        ContinueWhereLeftOffItem(
            nodeHandle = entity.nodeHandle,
            type = recentlyUsedTypeIdMapper(entity.typeId),
            title = entity.fileName,
            lastAccessedTimestamp = entity.lastAccessedTimestamp,
        )
}
