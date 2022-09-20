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
import nz.mega.sdk.MegaRecentActionBucket
import timber.log.Timber
import javax.inject.Inject

/**
 * Get nodes from the recent action bucket
 */
class DefaultGetRecentActionNodes @Inject constructor(
    private val getThumbnail: GetThumbnail,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GetRecentActionNodes {

    /**
     * Get nodes from the recent action bucket
     *
     * @param bucket the recent action that contains the nodes
     * @return the list of NodeItem associated to the bucket
     */
    override suspend fun invoke(bucket: MegaRecentActionBucket): List<NodeItem> =
        withContext(ioDispatcher) {
            val size = bucket.nodes.size()
            val coroutineScope = CoroutineScope(SupervisorJob())
            val deferredNodeItems = mutableListOf<Deferred<NodeItem>>().apply {
                for (i in 0 until size) {
                    bucket.nodes[i]?.let { node ->
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