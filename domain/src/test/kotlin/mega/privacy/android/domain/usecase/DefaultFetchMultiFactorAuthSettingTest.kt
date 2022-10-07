package mega.privacy.android.domain.usecase


import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultFetchMultiFactorAuthSettingTest {
    private lateinit var underTest: FetchMultiFactorAuthSetting
    private val accountRepository = mock<AccountRepository>()

    @Before
    fun setUp() {
        underTest = DefaultFetchMultiFactorAuthSetting(accountRepository = accountRepository)
    }


    @Test
    fun `test initial value`() = runTest {
        whenever(accountRepository.isMultiFactorAuthEnabled()).thenReturn(true)
        whenever(accountRepository.monitorMultiFactorAuthChanges()).thenReturn(flowOf())

        underTest().test {
            assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test updates are returned`() = runTest {
        whenever(accountRepository.isMultiFactorAuthEnabled()).thenReturn(true)
        whenever(accountRepository.monitorMultiFactorAuthChanges()).thenReturn(flowOf(false, true))

        underTest().test {
            assertThat(awaitItem()).isTrue()
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that subsequent changes are returned when an exception from the api is thrown`() =
        runTest {
            whenever(accountRepository.isMultiFactorAuthEnabled()).thenThrow(MegaException(-1,
                null))
            whenever(accountRepository.monitorMultiFactorAuthChanges()).thenReturn(flowOf(true))

            underTest().test {
                assertThat(awaitItem()).isFalse()
                assertThat(awaitItem()).isTrue()
                awaitComplete()
            }
        }
}