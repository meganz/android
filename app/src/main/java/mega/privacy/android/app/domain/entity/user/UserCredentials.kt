package mega.privacy.android.app.domain.entity.user

/**
 * User credentials
 *
 * @property email
 * @property session
 * @property firstName
 * @property lastName
 * @property myHandle
 */
data class UserCredentials(
    val email: String,
    val session: String,
    val firstName: String,
    val lastName: String,
    val myHandle: String,
)
