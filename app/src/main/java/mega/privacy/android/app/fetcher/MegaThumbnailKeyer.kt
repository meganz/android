package mega.privacy.android.app.fetcher

import coil.key.Keyer
import coil.request.Options
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Mega thumbnail keyer to build the key for MegaNode thumbnail in the memory cache
 *
 */
internal object MegaThumbnailKeyer : Keyer<NodeId> {
    override fun key(data: NodeId, options: Options): String = "${data.longValue}-${options.size}"
}