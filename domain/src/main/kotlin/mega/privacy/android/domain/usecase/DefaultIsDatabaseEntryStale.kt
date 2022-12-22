package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.TimeSystemRepository
import javax.inject.Inject

/**
 * Default is database entry stale
 *
 * @property accountRepository
 * @property timeSystemRepository
 */
internal class DefaultIsDatabaseEntryStale @Inject constructor(
    private val accountRepository: AccountRepository,
    private val timeSystemRepository: TimeSystemRepository,
) : IsDatabaseEntryStale {
    override suspend fun invoke(): Boolean {
        val oldTimestamp = accountRepository.getAccountDetailsTimeStampInSeconds().orEmpty()
        return if (oldTimestamp.isNotBlank()) {
            val timestampMinDifference = calculateTimeDifferenceUntilNow(oldTimestamp)
            return timestampMinDifference > ACCOUNT_DETAILS_MIN_DIFFERENCE
        } else {
            true
        }
    }

    private fun calculateTimeDifferenceUntilNow(timeStamp: String): Long {
        val actualTimestamp = timeSystemRepository.getCurrentTimeInMillis() / 1000L
        val oldTimestamp = timeStamp.toLongOrNull() ?: 0L
        return (actualTimestamp - oldTimestamp) / 60L
    }

    companion object {
        /**
         * Account Details Min Difference
         */
        internal const val ACCOUNT_DETAILS_MIN_DIFFERENCE = 5
    }
}