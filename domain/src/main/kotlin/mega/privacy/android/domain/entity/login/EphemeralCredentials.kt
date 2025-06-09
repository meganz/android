package mega.privacy.android.domain.entity.login

/**
 * Data class for storing ephemeral credentials when creating new account.
 *
 * @property email Account email.
 * @property password Account password. only available in the first time when creating an account, we don't persist it.
 * @property session Ephemeral session.
 * @property firstName Account first name.
 * @property lastName Account last name.
 */
data class EphemeralCredentials(
    val email: String?,
    val password: String? = null,
    val session: String?,
    val firstName: String?,
    val lastName: String?,
)
