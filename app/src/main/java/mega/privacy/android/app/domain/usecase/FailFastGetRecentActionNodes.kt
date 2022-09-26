package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.mapper.FileTypeInfoMapper
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetThumbnail
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRecentActionBucket
import timber.log.Timber
import javax.inject.Inject

/**
 * Get nodes from the recent action bucket
 */
class FailFastGetRecentActionNodes @Inject constructor(
    private val getThumbnail: GetThumbnail,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
) : GetRecentActionNodes {

    /**
     * Get nodes from the recent action bucket
     *
     * @param bucket the recent action that contains the nodes
     * @return the list of NodeItem associated to the bucket
     */
    override suspend fun invoke(nodes: MegaNodeList): List<NodeItem> {
        return withContext(ioDispatcher) {
            List(nodes.size()) { nodes[it] }
                .map { node ->
                    async {
                        createNodeItem(node)
                    }
                }.awaitAll()
        }
    }

    private suspend fun createNodeItem(
        node: MegaNode,
    ) = kotlin.runCatching {
        val typeInfo = fileTypeInfoMapper(node)
        NodeItem(
            node = node,
            thumbnail = getThumbnail(node.handle),
            index = -1,
            isVideo = typeInfo is VideoFileTypeInfo,
            modifiedDate = node.modificationTime.toString(),
        )
    }.onFailure {
        Timber.e(it)
    }.getOrThrow()
}