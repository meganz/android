package mega.privacy.android.domain.entity

/**
 * Share data
 *
 * @property user
 * @property nodeHandle
 * @property access
 * @property timeStamp
 * @property isPending
 */
data class ShareData(
    val user: String?,
    val nodeHandle: Long,
    val access: Int,
    val timeStamp: Long,
    val isPending: Boolean,
)