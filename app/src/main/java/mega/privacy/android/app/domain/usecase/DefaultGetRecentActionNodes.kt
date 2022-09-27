package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.mapper.FileTypeInfoMapper
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetThumbnail
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import timber.log.Timber
import javax.inject.Inject

/**
 * Transform a [MegaNodeList] into a list of [NodeItem]
 */
class DefaultGetRecentActionNodes @Inject constructor(
    private val getThumbnail: GetThumbnail,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
) : GetRecentActionNodes {

    /**
     * Transform a [MegaNodeList] into a list of [NodeItem]
     *
     * @param nodes the nodes to convert
     * @return a list of node item resulting from the conversion
     */
    override suspend fun invoke(nodes: MegaNodeList): List<NodeItem> =
        withContext(ioDispatcher) {
            List(nodes.size()) { nodes[it] }
                .map { node ->
                    async {
                        createNodeItem(node)
                    }
                }.awaitAll()
                .filterNotNull()
        }

    /**
     * Create a single [NodeItem] from [MegaNode]
     *
     * @param node
     * @return the corresponding [NodeItem], null if an error occured
     */
    private suspend fun createNodeItem(node: MegaNode): NodeItem? =
        kotlin.runCatching {
            NodeItem(
                node = node,
                thumbnail = getThumbnail(node.handle),
                index = -1,
                isVideo = fileTypeInfoMapper(node) is VideoFileTypeInfo,
                modifiedDate = node.modificationTime.toString(),
            )
        }.onFailure {
            Timber.e(it)
        }.getOrNull()
}