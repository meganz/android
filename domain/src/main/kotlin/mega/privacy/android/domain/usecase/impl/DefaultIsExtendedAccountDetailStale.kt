package mega.privacy.android.domain.usecase.impl

import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.TimeSystemRepository
import mega.privacy.android.domain.usecase.IsExtendedAccountDetailStale
import javax.inject.Inject

/**
 * Default is extended account detail stale
 *
 * @property accountRepository
 * @property timeSystemRepository
 */
internal class DefaultIsExtendedAccountDetailStale @Inject constructor(
    private val accountRepository: AccountRepository,
    private val timeSystemRepository: TimeSystemRepository,
) : IsExtendedAccountDetailStale {
    override suspend fun invoke(): Boolean {
        val oldTimestamp = accountRepository.getExtendedAccountDetailsTimeStampInSeconds().orEmpty()
        val timestampMinDifference = calculateTimeDifferenceUntilNow(oldTimestamp)
        return timestampMinDifference > EXTENDED_ACCOUNT_DETAILS_MIN_DIFFERENCE
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
        internal const val EXTENDED_ACCOUNT_DETAILS_MIN_DIFFERENCE = 30
    }
}