package mega.privacy.android.data.mapper.viewedlinks

import mega.privacy.android.domain.entity.node.RecentlyViewedLinkType
import javax.inject.Inject

/**
 * Bidirectional mapper between [RecentlyViewedLinkType] and its database type ID.
 *
 * FileLink and FolderLink share the historical 1/2 type IDs used while the
 * recently_viewed_link table was coupled to recently_used; keeping the same
 * numeric values lets the v121 migration backfill type_id directly from
 * recently_used.type_id without re-mapping.
 */
internal class RecentlyViewedLinkTypeIdMapper @Inject constructor() {

    /**
     * Converts a [RecentlyViewedLinkType] to its database type ID.
     */
    operator fun invoke(type: RecentlyViewedLinkType): Int = when (type) {
        RecentlyViewedLinkType.FileLink -> 1
        RecentlyViewedLinkType.FolderLink -> 2
    }

    /**
     * Converts a database type ID to a [RecentlyViewedLinkType].
     */
    operator fun invoke(typeId: Int): RecentlyViewedLinkType = when (typeId) {
        1 -> RecentlyViewedLinkType.FileLink
        2 -> RecentlyViewedLinkType.FolderLink
        else -> error("Unknown type ID: $typeId")
    }
}
