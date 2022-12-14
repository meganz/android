package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultGetExtendedAccountDetailTest {
    private lateinit var underTest: DefaultGetExtendedAccountDetail
    private val accountRepository = mock<AccountRepository>()
    private val isExtendedAccountDetailStale = mock<IsExtendedAccountDetailStale>()

    @Before
    fun setUp() {
        underTest = DefaultGetExtendedAccountDetail(
            repository = accountRepository,
            isExtendedAccountDetailStale = isExtendedAccountDetailStale,
        )
    }

    @Test
    fun `test that invoke correct account repository method`() = runTest {
        underTest(forceRefresh = true, sessions = true, purchases = true, transactions = true)
        verify(accountRepository).resetExtendedAccountDetailsTimestamp()
        verify(accountRepository).getExtendedAccountDetails(
            sessions = true,
            purchases = true,
            transactions = true
        )
    }

    @Test
    fun `test that invoke correct account repository method if isExtendedAccountDetailStale true`() =
        runTest {
            whenever(isExtendedAccountDetailStale()).thenReturn(true)
            underTest(forceRefresh = false, sessions = true, purchases = true, transactions = true)
            verify(accountRepository).resetExtendedAccountDetailsTimestamp()
            verify(accountRepository).getExtendedAccountDetails(
                sessions = true,
                purchases = true,
                transactions = true
            )
        }
}