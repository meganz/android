package mega.privacy.android.data.mapper.viewedlinks

import mega.privacy.android.data.database.entity.ViewedLinkRawItem
import mega.privacy.android.data.mapper.continuewhereleftoff.RecentlyUsedTypeIdMapper
import mega.privacy.android.domain.entity.node.ViewedLink
import javax.inject.Inject

/**
 * Maps [ViewedLinkRawItem] (Room POJO from the multi-join query) to the
 * domain entity [ViewedLink].
 *
 * @property recentlyUsedTypeIdMapper
 */
internal class ViewedLinkRawItemMapper @Inject constructor(
    private val recentlyUsedTypeIdMapper: RecentlyUsedTypeIdMapper,
) {

    /**
     * Maps a single [ViewedLinkRawItem] to a [ViewedLink].
     *
     * @param raw
     */
    operator fun invoke(raw: ViewedLinkRawItem) = ViewedLink(
        nodeHandle = raw.nodeHandle,
        name = raw.fileName,
        linkUrl = raw.linkUrl,
        type = recentlyUsedTypeIdMapper(raw.typeId),
        accessedTimestamp = raw.lastAccessedTimestamp,
    )

    /**
     * Maps a list of [ViewedLinkRawItem] to a list of [ViewedLink].
     *
     * @param items
     */
    operator fun invoke(items: List<ViewedLinkRawItem>) = items.map { invoke(it) }
}
