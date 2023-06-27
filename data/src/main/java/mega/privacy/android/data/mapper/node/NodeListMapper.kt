package mega.privacy.android.data.mapper.node

import mega.privacy.android.domain.entity.node.Node
import nz.mega.sdk.MegaNodeList
import javax.inject.Inject

/**
 * Mapper for converting [MegaNodeList] into a list of [Node].
 */
internal class NodeListMapper @Inject constructor(
    private val nodeMapper: NodeMapper,
) {

    suspend operator fun invoke(megaNodeList: MegaNodeList) = with(megaNodeList) {
        (0 until size()).map { nodeMapper(get(it)) }
    }
}