package mega.privacy.android.domain.entity.account

/**
 * Account inactivity information derived from the last purge event, used to drive the
 * account inactivity banner.
 *
 * @property inactivityMonths the number of months the account has been inactive.
 * @property purgeTimestamp the Unix timestamp (seconds) of the purge, to pass back when the user
 *                          acknowledges (dismisses) the banner.
 */
data class AccountInactivity(
    val inactivityMonths: Int,
    val purgeTimestamp: Long,
)
