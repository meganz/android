package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.recentactions.NodeInfoForRecentActionsMapper
import mega.privacy.android.data.mapper.recentactions.RecentActionBucketMapper
import mega.privacy.android.data.mapper.recentactions.RecentActionsMapper
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.extension.mapAsync
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.RecentActionsRepository
import nz.mega.sdk.MegaRecentActionBucket
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [RecentActionsRepository]
 */
internal class DefaultRecentActionsRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val recentActionsMapper: RecentActionsMapper,
    private val recentActionBucketMapper: RecentActionBucketMapper,
    private val nodeInfoForRecentActionsMapper: NodeInfoForRecentActionsMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RecentActionsRepository {

    override suspend fun getRecentActions(
        excludeSensitives: Boolean,
    ) = withContext(ioDispatcher) {
        runCatching {
            val list = getMegaRecentAction(
                excludeSensitives = excludeSensitives
            ).map {
                megaApiGateway.copyBucket(it).let { bucket ->
                    bucket to megaApiGateway.getNodesFromMegaNodeList(bucket.nodes)
                }
            }.mapAsync {
                recentActionBucketMapper(
                    megaRecentActionBucket = it.first,
                    megaNodes = it.second
                )
            }
            return@withContext list
        }.onFailure {
            Timber.e(it)
        }
        return@withContext emptyList<RecentActionBucketUnTyped>()
    }

    override suspend fun getNodeInfo(nodeId: NodeId) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
            nodeInfoForRecentActionsMapper(
                megaNode = it,
                isPendingShare = megaApiGateway.isPendingShare(it)
            )
        }
    }

    private suspend fun getMegaRecentAction(
        excludeSensitives: Boolean,
    ): List<MegaRecentActionBucket> =
        withContext(ioDispatcher) {
            val result = suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("getRecentActionsAsync") {
                    megaApiGateway.copyBucketList(it.recentActions)
                }
                megaApiGateway.getRecentActionsAsync(DAYS, MAX_NODES, excludeSensitives, listener)
            }
            recentActionsMapper(result)
        }

    companion object {
        /**
         * Default and recommended value for getting recent actions in the last days.
         */
        private const val DAYS = 30L

        /**
         * Default and recommended value for getting recent actions for a maximum value of nodes.
         */
        private const val MAX_NODES = 500L
    }
}
