package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.shares.AccessPermission

/**
 * Share data
 *
 * @property user email of the user
 * @property userFullName full name of the user, optional
 * @property nodeHandle
 * @property access
 * @property timeStamp
 * @property isPending
 * @param isVerified
 * @property count
 * @property isContactCredentialsVerified if the credentials of the contact are verified
 * @property isUnverifiedDistinctNode
 *
 * @constructor Create empty Share data
 */
data class ShareData(
    val user: String?,
    val userFullName: String? = null,
    val nodeHandle: Long,
    val access: AccessPermission,
    val timeStamp: Long,
    val isPending: Boolean,
    val isVerified: Boolean,
    val isContactCredentialsVerified: Boolean = false,
    val count: Int
) {
    val isUnverifiedDistinctNode = !isVerified && count == 0
}