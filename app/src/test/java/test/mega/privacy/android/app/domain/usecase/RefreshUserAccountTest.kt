package test.mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.AccountRepository
import mega.privacy.android.app.domain.usecase.DefaultRefreshUserAccount
import mega.privacy.android.app.domain.usecase.RefreshUserAccount
import org.junit.Assert.*

import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class RefreshUserAccountTest {

    private lateinit var underTest: RefreshUserAccount

    private val accountRepository = mock<AccountRepository>()

    @Before
    fun setUp() {
        underTest = DefaultRefreshUserAccount(accountRepository)
    }

    @Test
    fun `test that account fetch is called if it has not been fetched yet`() {
        whenever(accountRepository.hasAccountBeenFetched()).thenReturn(false)

        underTest()

        verify(accountRepository, times(1)).requestAccount()
    }

    @Test
    fun `test that account is not requested if it has already been fetched`() {
        whenever(accountRepository.hasAccountBeenFetched()).thenReturn(true)

        underTest()

        verify(accountRepository, never()).requestAccount()
    }
}