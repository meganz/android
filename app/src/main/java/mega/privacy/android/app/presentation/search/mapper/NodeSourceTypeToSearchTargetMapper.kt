package mega.privacy.android.app.presentation.search.mapper

import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.search.SearchTarget
import javax.inject.Inject

/**
 * Mapper used to map node source types to search targets
 */
class NodeSourceTypeToSearchTargetMapper @Inject constructor() {
    /**
     * invoke
     *
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
