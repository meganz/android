package mega.privacy.android.domain.entity.login

/**
 * Data class for storing ephemeral credentials when creating new account.
 *
 * @property email Account email.
 * @property password Account password.
 * @property session Ephemeral session.
 * @property firstName Account first name.
 * @property lastName Account last name.
 */
data class EphemeralCredentials(
    val email: String?,
    val password: String?,
    val session: String?,
    val firstName: String?,
    val lastName: String?,
)
