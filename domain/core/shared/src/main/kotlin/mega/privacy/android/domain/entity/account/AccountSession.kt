package mega.privacy.android.domain.entity.account

/**
 * Account session of the current logged in account.
 *
 * @property email    User's email.
 * @property session  User's session.
 * @property myHandle User's handle.
 */
data class AccountSession(
    val email: String? = null,
    val session: String? = null,
    val myHandle: Long = -1,
)
