package mega.privacy.android.shared.nodes.mapper

import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.search.SearchTarget
import javax.inject.Inject

/**
 * Maps a [NodeSourceType] to the [SearchTarget] used when searching that source.
 */
class NodeSourceTypeToSearchTargetMapper @Inject constructor() {
    /**
     * @param nodeSourceType source type for nodes leading to search
     */
    operator fun invoke(nodeSourceType: NodeSourceType): SearchTarget =
        when (nodeSourceType) {
            NodeSourceType.INCOMING_SHARES -> SearchTarget.INCOMING_SHARE
            NodeSourceType.OUTGOING_SHARES -> SearchTarget.OUTGOING_SHARE
            NodeSourceType.LINKS -> SearchTarget.LINKS_SHARE
            else -> SearchTarget.ROOT_NODES
        }
}
