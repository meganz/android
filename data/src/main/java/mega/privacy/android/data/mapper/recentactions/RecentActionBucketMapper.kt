package mega.privacy.android.data.mapper.recentactions

import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.extension.mapAsync
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRecentActionBucket
import javax.inject.Inject

/**
 * The mapper class for converting [MegaRecentActionBucket] and list of [MegaNode] to [RecentActionBucketUnTyped]
 */
internal class RecentActionBucketMapper @Inject constructor(private val nodeMapper: NodeMapper) {
    suspend operator fun invoke(
        identifier: String,
        megaRecentActionBucket: MegaRecentActionBucket,
        megaNodes: List<MegaNode>,
    ) = RecentActionBucketUnTyped(
        identifier = identifier,
        timestamp = megaRecentActionBucket.timestamp,
        userEmail = megaRecentActionBucket.userEmail,
        parentNodeId = NodeId(megaRecentActionBucket.parentHandle),
        isUpdate = megaRecentActionBucket.isUpdate,
        isMedia = megaRecentActionBucket.isMedia,
        nodes = megaNodes.mapAsync { nodeMapper(it) },
    )
}