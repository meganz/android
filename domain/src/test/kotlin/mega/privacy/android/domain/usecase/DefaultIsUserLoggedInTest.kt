package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultIsUserLoggedInTest {
    private lateinit var underTest: IsUserLoggedIn

    private val accountRepository = mock<AccountRepository>()

    @Before
    fun setUp() {
        underTest = DefaultIsUserLoggedIn(accountRepository)
    }

    @Test
    fun `test that return value is true when user is loggedIn`() = runTest {
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        assertThat(underTest.invoke()).isTrue()
    }

    @Test
    fun `test that return value is false when user is not loggedIn`() = runTest {
        whenever(accountRepository.isUserLoggedIn()).thenReturn(false)
        assertThat(underTest.invoke()).isFalse()
    }
}