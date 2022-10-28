package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetThumbnail
import nz.mega.sdk.MegaNodeList
import timber.log.Timber
import javax.inject.Inject

/**
 * Transform a [MegaNodeList] into a list of [NodeItem]
 */
class DefaultGetRecentActionNodes @Inject constructor(
    private val getThumbnail: GetThumbnail,
    private val getNodeByHandle: GetNodeByHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GetRecentActionNodes {

    /**
     * Transform a List of [Node] into a list of [NodeItem]
     *
     * @param nodes the nodes to convert
     * @return a list of node item resulting from the conversion
     */
    override suspend fun invoke(nodes: List<Node>): List<NodeItem> =
        withContext(ioDispatcher) {
            List(nodes.size) { nodes[it] }
                .map { node ->
                    async {
                        ensureActive()
                        createNodeItem(node)
                    }
                }.awaitAll()
                .filterNotNull()
        }

    /**
     * Create a single [NodeItem] from [Node]
     *
     * @param node
     * @return the corresponding [NodeItem], null if an error occurred
     */
    private suspend fun createNodeItem(node: Node): NodeItem? =
        runCatching {
            if (node is FileNode) {
                val megaNode = getNodeByHandle.invoke(node.id.id)
                NodeItem(
                    node = megaNode,
                    thumbnail = getThumbnail(node.id.id),
                    index = -1,
                    isVideo = node.type is VideoFileTypeInfo,
                    modifiedDate = node.modificationTime.toString(),
                )
            } else {
                return@runCatching null
            }
        }.onFailure {
            Timber.e(it)
        }.getOrNull()

}
