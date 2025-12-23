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
import java.time.Instant
import java.time.ZoneId
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

    private val systemZoneId = ZoneId.systemDefault()

    override suspend fun getRecentActions(
        excludeSensitives: Boolean,
        maxBucketCount: Int,
    ) = withContext(ioDispatcher) {
        runCatching {
            val list = getMegaRecentAction(excludeSensitives = excludeSensitives)
                .take(maxBucketCount)
                .map { bucket ->
                    bucket to megaApiGateway.getNodesFromMegaNodeList(bucket.nodes)
                }.mapAsync {
                    val (identifier, dateTimestamp) = generateIdentifier(it.first)
                    recentActionBucketMapper(
                        identifier = identifier,
                        dateTimestamp = dateTimestamp,
                        megaRecentActionBucket = it.first,
                        megaNodes = it.second
                    )
                }
            return@withContext list
        }.onFailure {
            Timber.e(it)
        }
        return@withContext emptyList()
    }

    override suspend fun getNodeInfo(nodeId: NodeId) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
            nodeInfoForRecentActionsMapper(
                megaNode = it,
                isPendingShare = megaApiGateway.isPendingShare(it)
            )
        }
    }

    override suspend fun getRecentActionBucketByIdentifier(
        bucketIdentifier: String,
        excludeSensitives: Boolean,
    ): RecentActionBucketUnTyped? = withContext(ioDispatcher) {
        val (matchingBucket, identifier, dateTimestamp) = getMegaRecentAction(excludeSensitives = excludeSensitives)
            .firstNotNullOfOrNull { bucket ->
                val (identifier, dateTimestamp) = generateIdentifier(bucket)
                if (identifier == bucketIdentifier) Triple(
                    bucket,
                    identifier,
                    dateTimestamp
                ) else null
            } ?: return@withContext null
        val megaNodes = megaApiGateway.getNodesFromMegaNodeList(matchingBucket.nodes)
        recentActionBucketMapper(
            identifier = identifier,
            dateTimestamp = dateTimestamp,
            megaRecentActionBucket = matchingBucket,
            megaNodes = megaNodes
        )
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
            recentActionsMapper(result).map {
                megaApiGateway.copyBucket(it)
            }
        }

    /**
     * Generate identifier from bucket properties
     * For example: M:true|U:false|D:1766472783|UE:ht@mega.co.nz|PNH:100124500130291
     * @return Pair of identifier and dateTimestamp
     */
    private fun generateIdentifier(bucket: MegaRecentActionBucket): Pair<String, Long> {
        // Each bucket is created based on date, so timestamp is converted to date only
        val dateTimestamp = Instant.ofEpochSecond(bucket.timestamp)
            .atZone(systemZoneId)
            .toLocalDate()
            .atStartOfDay(systemZoneId)
            .toEpochSecond()

        val identifier = buildString {
            append("M:").append(bucket.isMedia)
            append("|U:").append(bucket.isUpdate)
            append("|D:").append(dateTimestamp)
            append("|UE:").append(bucket.userEmail)
            append("|PNH:").append(bucket.parentHandle)
        }
        return identifier to dateTimestamp
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
