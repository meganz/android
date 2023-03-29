package mega.privacy.android.data.mapper.shares

import mega.privacy.android.domain.entity.shares.AccessPermission
import nz.mega.sdk.MegaShare
import javax.inject.Inject

/**
 * AccessPermission mapper
 */
internal class AccessPermissionIntMapper @Inject constructor() {
    /**
     * Maps [AccessPermission] to its MegaApi int value
     */
    operator fun invoke(accessPermission: AccessPermission) = when (accessPermission) {
        AccessPermission.READ -> MegaShare.ACCESS_READ
        AccessPermission.READWRITE -> MegaShare.ACCESS_READWRITE
        AccessPermission.FULL -> MegaShare.ACCESS_FULL
        AccessPermission.OWNER -> MegaShare.ACCESS_OWNER
        AccessPermission.UNKNOWN -> MegaShare.ACCESS_UNKNOWN
    }
}