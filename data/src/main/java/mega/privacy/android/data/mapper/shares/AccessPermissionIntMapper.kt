package mega.privacy.android.data.mapper.shares

import mega.privacy.android.domain.entity.shares.AccessPermission

/**
 * AccessPermission mapper
 */
internal interface AccessPermissionIntMapper {
    /**
     * Maps [AccessPermission] to its MegaApi int value
     */
    operator fun invoke(accessPermission: AccessPermission): Int
}