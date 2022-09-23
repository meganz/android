package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GetRecentActionNodes {

    /**
     * Transform a [MegaNodeList] into a list of [NodeItem]
     *
     * @param nodes the nodes to convert
     * @return a list of node item resulting from the conversion
     */
    override suspend fun invoke(nodes: MegaNodeList): List<NodeItem> =
        withContext(ioDispatcher) {
            val size = nodes.size()
            val coroutineScope = CoroutineScope(SupervisorJob())
            val deferredNodeItems = mutableListOf<Deferred<NodeItem>>().apply {
                for (i in 0 until size) {
                    nodes[i]?.let { node ->
                        add(
                            coroutineScope.async {
                                NodeItem(
                                    node = node,
                                    thumbnail = getThumbnail(node.handle),
                                    index = -1,
                                    isVideo = node.isVideo(),
                                    modifiedDate = node.modificationTime.toString(),
                                )
                            }
                        )
                    }
                }
            }

            val nodesList = ArrayList<NodeItem>().apply {
                deferredNodeItems.forEach {
                    try {
                        add(it.await())
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }

            }

            return@withContext nodesList
        }
}