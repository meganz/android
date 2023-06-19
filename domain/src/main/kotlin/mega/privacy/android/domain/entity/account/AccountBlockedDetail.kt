package mega.privacy.android.domain.entity.account

/**
 * Data class for blocked account details.
 *
 * @property [AccountBlockedType].
 * @property text Message.
 */
data class AccountBlockedDetail(
    val type: AccountBlockedType,
    val text: String,
)
