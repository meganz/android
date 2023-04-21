package mega.privacy.android.data.mapper.shares

import mega.privacy.android.domain.entity.ShareData
import nz.mega.sdk.MegaShare
import javax.inject.Inject

/**
 * [MegaShare] to [ShareData] mapper
 */
internal class ShareDataMapper @Inject constructor(
    private val accessPermissionMapper: AccessPermissionMapper,
) {
    operator fun invoke(share: MegaShare) = ShareData(
        user = share.user,
        isPending = share.isPending,
        timeStamp = share.timestamp,
        access = accessPermissionMapper(share.access),
        nodeHandle = share.nodeHandle,
        isVerified = share.isVerified,
    )
}