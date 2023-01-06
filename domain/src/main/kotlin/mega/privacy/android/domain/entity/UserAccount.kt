package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.user.UserId

/**
 * User account
 *
 * @property userId
 * @property email
 * @property fullName
 * @property isBusinessAccount
 * @property isMasterBusinessAccount
 * @property accountTypeIdentifier
 * @property accountTypeString
 */
data class UserAccount constructor(
    val userId: UserId?,
    val email: String,
    val fullName: String?,
    val isBusinessAccount: Boolean,
    val isMasterBusinessAccount: Boolean,
    val accountTypeIdentifier: AccountType?,
    val accountTypeString: String,
)
