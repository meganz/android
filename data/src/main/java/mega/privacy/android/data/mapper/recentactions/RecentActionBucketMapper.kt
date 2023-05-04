package mega.privacy.android.data.mapper.recentactions

import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import nz.mega.sdk.MegaRecentActionBucket
import javax.inject.Inject

/**
 * The mapper class for converting [MegaRecentActionBucket] to [RecentActionBucket]
 */
internal class RecentActionBucketMapper @Inject constructor(private val nodeMapper: NodeMapper) {
    suspend operator fun invoke(
        megaRecentActionBucket: MegaRecentActionBucket,
    ) = RecentActionBucketUnTyped(
        timestamp = megaRecentActionBucket.timestamp,
        userEmail = megaRecentActionBucket.userEmail,
        parentHandle = megaRecentActionBucket.parentHandle,
        isUpdate = megaRecentActionBucket.isUpdate,
        isMedia = megaRecentActionBucket.isMedia,
        nodes = (0 until megaRecentActionBucket.nodes.size()).map {
            nodeMapper(
                megaRecentActionBucket.nodes.get(it),
            )
        },
    )
}