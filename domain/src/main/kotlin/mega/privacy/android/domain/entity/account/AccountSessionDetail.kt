package mega.privacy.android.domain.entity.account

/**
 * Account session detail
 *
 * @property mostRecentSessionTimeStamp
 * @property createSessionTimeStamp
 */
data class AccountSessionDetail(
    val mostRecentSessionTimeStamp: Long,
    val createSessionTimeStamp: Long,
)