package mega.privacy.android.domain.entity

/**
 * Contact
 *
 * @property userId
 * @property email
 * @property nickname
 * @property firstName
 * @property lastName
 * @property hasPendingRequest
 * @property isVisible
 */
data class Contact @JvmOverloads constructor(
    val userId: Long,
    val email: String?,
    val nickname: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val hasPendingRequest: Boolean = false,
    val isVisible: Boolean = false,
)