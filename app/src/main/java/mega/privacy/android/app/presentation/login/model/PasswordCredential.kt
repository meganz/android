package mega.privacy.android.app.presentation.login.model

/**
 * Credential pair of a successful password login, offered to the user's password manager.
 *
 * @property email    Account email used to log in.
 * @property password Password used to log in.
 */
data class PasswordCredential(
    val email: String,
    val password: String,
) {
    override fun toString() = "PasswordCredential(email=$email, password=***)"
}
