package mega.privacy.android.data.mapper.shares

import mega.privacy.android.domain.entity.shares.AccessPermission
import nz.mega.sdk.MegaShare.ACCESS_FULL
import nz.mega.sdk.MegaShare.ACCESS_OWNER
import nz.mega.sdk.MegaShare.ACCESS_READ
import nz.mega.sdk.MegaShare.ACCESS_READWRITE
import javax.inject.Inject

/**
 * AccessPermissionMapper implementation
 */
internal class AccessPermissionMapperImpl @Inject constructor() : AccessPermissionMapper {
    override fun invoke(intRawValue: Int) =
        when (intRawValue) {
            ACCESS_READ -> AccessPermission.READ
            ACCESS_READWRITE -> AccessPermission.READWRITE
            ACCESS_FULL -> AccessPermission.FULL
            ACCESS_OWNER -> AccessPermission.OWNER
            else -> AccessPermission.UNKNOWN
        }
}