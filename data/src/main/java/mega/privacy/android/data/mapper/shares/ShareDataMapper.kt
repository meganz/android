package mega.privacy.android.data.mapper.shares

import mega.privacy.android.domain.entity.ShareData
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import javax.inject.Inject

/**
 * [MegaShare] to [ShareData] mapper
 */
internal class ShareDataMapper @Inject constructor(
    private val accessPermissionMapper: AccessPermissionMapper,
) {
    operator fun invoke(share: MegaShare, shareCount: Int = 1) = ShareData(
        user = share.user,
        isPending = share.isPending,
        timeStamp = share.timestamp,
        access = accessPermissionMapper(share.access),
        nodeHandle = share.nodeHandle,
        isVerified = share.isVerified,
        count = shareCount
    )

    operator fun invoke(email: String, node: MegaNode, access: Int, isVerified: Boolean) =
        ShareData(
            user = email,
            nodeHandle = node.handle,
            access = accessPermissionMapper(access),
            timeStamp = 0,
            isPending = false,
            isVerified = isVerified,
            // Verified shares carry a count of 1, unverified ones 0, matching getAllIncomingShares
            count = if (isVerified) 1 else 0,
        )

}