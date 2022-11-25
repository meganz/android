package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultGetExtendedAccountDetailTest {
    private lateinit var underTest: GetExtendedAccountDetail
    private val accountRepository = mock<AccountRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetExtendedAccountDetail(
            repository = accountRepository,
        )
    }

    @Test
    fun `test that invoke correct account repository method`() = runTest {
        underTest(sessions = true, purchases = true, transactions = true)
        verify(accountRepository).resetExtendedAccountDetailsTimestamp()
        verify(accountRepository).getExtendedAccountDetails(
            sessions = true,
            purchases = true,
            transactions = true
        )
    }
}