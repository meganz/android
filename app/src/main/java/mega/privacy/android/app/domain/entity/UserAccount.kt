package mega.privacy.android.app.domain.entity

import mega.privacy.android.app.domain.entity.user.UserId

/**
 * User account
 *
 * @property userId
 * @property email
 * @property isBusinessAccount
 * @property isMasterBusinessAccount
 * @property accountTypeIdentifier
 */
data class UserAccount(
    val userId: UserId?,
    val email: String,
    val isBusinessAccount: Boolean,
    val isMasterBusinessAccount: Boolean,
    val accountTypeIdentifier: Int
)
