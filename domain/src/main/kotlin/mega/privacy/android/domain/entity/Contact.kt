package mega.privacy.android.domain.entity

/**
 * Contact
 *
 * @property isVisible
 */
data class Contact(
    val userId: Long,
    val email: String?,
    val nickname: String?,
    val isVisible: Boolean,
    val hasPendingRequest: Boolean,
)
