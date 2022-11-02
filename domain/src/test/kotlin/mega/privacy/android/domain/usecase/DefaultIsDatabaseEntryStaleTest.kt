package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.TimeSystemRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class DefaultIsDatabaseEntryStaleTest {
    private lateinit var underTest: DefaultIsDatabaseEntryStale

    private val accountRepository = mock<AccountRepository>()
    private val timeSystemRepository = mock<TimeSystemRepository>()

    @Before
    fun setUp() {
        underTest = DefaultIsDatabaseEntryStale(
            accountRepository = accountRepository,
            timeSystemRepository = timeSystemRepository
        )
    }

    @Test
    fun `test that when user get account detail timestamp is null then return true`() = runTest {
        whenever(accountRepository.getAccountDetailsTimeStampInSeconds()).thenReturn(null)
        Truth.assertThat(underTest()).isEqualTo(true)
    }

    @Test
    fun `test that when user get account detail timestamp is less than min different then return true`() =
        runTest {
            val current = System.currentTimeMillis()
            val accountDetailTimeStamp =
                current / 1000L - DefaultIsDatabaseEntryStale.ACCOUNT_DETAILS_MIN_DIFFERENCE * 60L - 60L
            whenever(timeSystemRepository.getCurrentTimeInMillis()).thenReturn(current)
            whenever(accountRepository.getAccountDetailsTimeStampInSeconds()).thenReturn(
                accountDetailTimeStamp.toString())
            Truth.assertThat(underTest()).isEqualTo(true)
        }

    @Test
    fun `test that when user get account detail timestamp is larger than min different then return false`() =
        runTest {
            val current = System.currentTimeMillis()
            val accountDetailTimeStamp =
                current / 1000L - DefaultIsDatabaseEntryStale.ACCOUNT_DETAILS_MIN_DIFFERENCE * 60L + 60L
            whenever(timeSystemRepository.getCurrentTimeInMillis()).thenReturn(current)
            whenever(accountRepository.getAccountDetailsTimeStampInSeconds()).thenReturn(
                accountDetailTimeStamp.toString())
            Truth.assertThat(underTest()).isEqualTo(false)
        }
}