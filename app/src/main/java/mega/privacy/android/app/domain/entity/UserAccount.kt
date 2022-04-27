package mega.privacy.android.app.domain.entity

/**
 * User account
 *
 * @property email
 * @property isBusinessAccount
 * @property isMasterBusinessAccount
 * @property accountTypeIdentifier
 */
data class UserAccount(
    val email: String,
    val isBusinessAccount: Boolean,
    val isMasterBusinessAccount: Boolean,
    val accountTypeIdentifier: Int
)
