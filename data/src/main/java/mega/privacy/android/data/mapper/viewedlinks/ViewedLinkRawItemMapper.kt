package mega.privacy.android.data.mapper.viewedlinks

import mega.privacy.android.data.database.entity.ViewedLinkRawItem
import mega.privacy.android.domain.entity.node.ViewedLink
import javax.inject.Inject

/**
 * Maps [ViewedLinkRawItem] (Room POJO from the recently_viewed_link query) to
 * the domain entity [ViewedLink].
 *
 * @property recentlyViewedLinkTypeIdMapper
 */
internal class ViewedLinkRawItemMapper @Inject constructor(
    private val recentlyViewedLinkTypeIdMapper: RecentlyViewedLinkTypeIdMapper,
) {

    /**
     * Maps a single [ViewedLinkRawItem] to a [ViewedLink].
     *
     * @param raw
     */
    operator fun invoke(raw: ViewedLinkRawItem) = ViewedLink(
        nodeHandle = raw.nodeHandle,
        name = raw.nodeName,
        linkUrl = raw.linkUrl,
        type = recentlyViewedLinkTypeIdMapper(raw.typeId),
        accessedTimestamp = raw.lastAccessedTimestamp,
    )

    /**
     * Maps a list of [ViewedLinkRawItem] to a list of [ViewedLink].
     *
     * @param items
     */
    operator fun invoke(items: List<ViewedLinkRawItem>) = items.map { invoke(it) }
}
