package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Suppress the inactivity banner for the given purge timestamp for the current session.
 *
 * Used when the user dismisses the banner, so it does not reappear when navigating back within the
 * session (the server acknowledgement handles cross-session/device suppression).
 */
class SuppressPurgeTimestampUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Suppresses the inactivity banner for [purgeTimestamp].
     *
     * @param purgeTimestamp the purge timestamp (seconds) to suppress.
     */
    operator fun invoke(purgeTimestamp: Long) =
        accountRepository.setSuppressedPurgeTimestamp(purgeTimestamp)
}
