package mega.privacy.android.data.mapper.shares

import mega.privacy.android.domain.entity.shares.AccessPermission
import nz.mega.sdk.MegaShare
import javax.inject.Inject

/**
 * AccessPermission mapper
 */
internal class AccessPermissionMapper @Inject constructor() {
    /**
     * Maps access permission raw int value from MegaShare to [AccessPermission]
     */
    operator fun invoke(intRawValue: Int) = when (intRawValue) {
        MegaShare.ACCESS_READ -> AccessPermission.READ
        MegaShare.ACCESS_READWRITE -> AccessPermission.READWRITE
        MegaShare.ACCESS_FULL -> AccessPermission.FULL
        MegaShare.ACCESS_OWNER -> AccessPermission.OWNER
        else -> AccessPermission.UNKNOWN
    }
}