package mega.privacy.android.data.mapper.shares

import mega.privacy.android.domain.entity.shares.AccessPermission

/**
 * AccessPermission mapper
 */
internal interface AccessPermissionMapper {
    /**
     * Maps access permission raw int value from MegaShare to [AccessPermission]
     */
    operator fun invoke(intRawValue: Int): AccessPermission
}