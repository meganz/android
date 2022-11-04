package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetAccountDetailsTest {
    private lateinit var underTest: GetAccountDetails
    private val accountRepository = mock<AccountRepository>()
    private val isDatabaseEntryStale = mock<IsDatabaseEntryStale>()

    @Before
    fun setUp() {
        underTest = DefaultGetAccountDetails(
            accountsRepository = accountRepository,
            isDatabaseEntryStale = isDatabaseEntryStale
        )
    }

    @Test
    fun `test that account details are refreshed if stale`() = runTest {
        whenever(accountRepository.storageCapacityUsedIsBlank()).thenReturn(true)
        underTest(false)

        verify(accountRepository).requestAccount()
    }

    @Test
    fun `test that account details are refreshed if forced`() = runTest {
        whenever(accountRepository.storageCapacityUsedIsBlank()).thenReturn(false)
        whenever(isDatabaseEntryStale()).thenReturn(false)
        underTest(true)

        verify(accountRepository).requestAccount()
    }

    @Test
    fun `test that account details are not refreshed if not stale or forced`() = runTest {
        whenever(accountRepository.storageCapacityUsedIsBlank()).thenReturn(false)
        whenever(isDatabaseEntryStale()).thenReturn(false)
        underTest(false)

        verify(accountRepository, never()).requestAccount()
    }
}