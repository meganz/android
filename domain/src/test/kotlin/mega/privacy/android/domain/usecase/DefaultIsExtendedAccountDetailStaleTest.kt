package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.TimeSystemRepository
import mega.privacy.android.domain.usecase.impl.DefaultIsExtendedAccountDetailStale
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class DefaultIsExtendedAccountDetailStaleTest {
    private lateinit var underTest: DefaultIsExtendedAccountDetailStale

    private val accountRepository = mock<AccountRepository>()
    private val timeSystemRepository = mock<TimeSystemRepository>()

    @Before
    fun setUp() {
        underTest = DefaultIsExtendedAccountDetailStale(
            accountRepository = accountRepository,
            timeSystemRepository = timeSystemRepository
        )
    }

    @Test
    fun `test that when user get extended account detail timestamp is null then return true`() =
        runTest {
            val current = System.currentTimeMillis()
            whenever(timeSystemRepository.getCurrentTimeInMillis()).thenReturn(current)
            whenever(accountRepository.getExtendedAccountDetailsTimeStampInSeconds()).thenReturn(
                null)
            Truth.assertThat(underTest()).isEqualTo(true)
        }

    @Test
    fun `test that when user get extended account detail timestamp is less than min different then return true`() =
        runTest {
            val current = System.currentTimeMillis()
            val extendedAccountDetailTimeStamp =
                current / 1000L - DefaultIsExtendedAccountDetailStale.EXTENDED_ACCOUNT_DETAILS_MIN_DIFFERENCE * 60L - 60L
            whenever(timeSystemRepository.getCurrentTimeInMillis()).thenReturn(current)
            whenever(accountRepository.getExtendedAccountDetailsTimeStampInSeconds()).thenReturn(
                extendedAccountDetailTimeStamp.toString())
            Truth.assertThat(underTest()).isEqualTo(true)
        }

    @Test
    fun `test that when user get extended account detail timestamp is larger than min different then return false`() =
        runTest {
            val current = System.currentTimeMillis()
            val extendedAccountDetailTimeStamp =
                current / 1000L - DefaultIsExtendedAccountDetailStale.EXTENDED_ACCOUNT_DETAILS_MIN_DIFFERENCE * 60L + 60L
            whenever(timeSystemRepository.getCurrentTimeInMillis()).thenReturn(current)
            whenever(accountRepository.getExtendedAccountDetailsTimeStampInSeconds()).thenReturn(
                extendedAccountDetailTimeStamp.toString())
            Truth.assertThat(underTest()).isEqualTo(false)
        }
}