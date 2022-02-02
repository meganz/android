package mega.privacy.android.app.domain.entity

data class UserAccount(
    val email: String,
    val isBusinessAccount: Boolean,
    val isMasterBusinessAccount: Boolean,
    val accountTypeIdentifier: Int
)
