package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.ShareData
import nz.mega.sdk.MegaShare

/**
 * [MegaShare] to [ShareData] mapper
 */
typealias MegaShareMapper = (@JvmSuppressWildcards MegaShare) -> @JvmSuppressWildcards ShareData

internal fun toShareModel(share: MegaShare) = ShareData(
    user = share.user,
    isPending = share.isPending,
    timeStamp = share.timestamp,
    access = share.access,
    nodeHandle = share.nodeHandle,
    isVerified = share.isVerified,
)