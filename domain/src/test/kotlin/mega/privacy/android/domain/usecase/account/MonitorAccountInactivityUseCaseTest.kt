package mega.privacy.android.domain.usecase.account

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.account.AccountInactivity
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorAccountInactivityUseCaseTest {

    private lateinit var underTest: MonitorAccountInactivityUseCase

    private val accountRepository = mock<AccountRepository>()

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
        whenever(accountRepository.monitorAccountInactivity()).thenReturn(flowOf(null))
        whenever(accountRepository.monitorSuppressedPurgeTimestamp()).thenReturn(flowOf(null))
    }

    @Test
    fun `test that invoke emits the inactivity provided by the repository`() = runTest {
        whenever(accountRepository.monitorAccountInactivity()).thenReturn(flowOf(inactivity))
        underTest = createUseCase(backgroundScope)

        underTest().test {
            assertThat(awaitItem()).isNull()
            assertThat(awaitItem()?.purgeTimestamp).isEqualTo(PURGE_TS)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke emits null when the repository emits no inactivity`() = runTest {
        whenever(accountRepository.monitorAccountInactivity()).thenReturn(flowOf(null))
        underTest = createUseCase(backgroundScope)

        underTest().test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the banner is hidden when the matching purge timestamp is suppressed`() =
        runTest {
            val suppressed = MutableStateFlow<Long?>(null)
            whenever(accountRepository.monitorSuppressedPurgeTimestamp()).thenReturn(suppressed)
            whenever(accountRepository.monitorAccountInactivity()).thenReturn(flowOf(inactivity))
            underTest = createUseCase(backgroundScope)

            underTest().test {
                assertThat(awaitItem()).isNull()
                assertThat(awaitItem()).isNotNull()

                suppressed.value = PURGE_TS

                assertThat(awaitItem()).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun createUseCase(scope: CoroutineScope) = MonitorAccountInactivityUseCase(
        accountRepository = accountRepository,
        scope = scope,
    )

    private companion object {
        private const val PURGE_TS = 1_000_000L

        private val inactivity = AccountInactivity(
            inactivityMonths = 3,
            purgeTimestamp = PURGE_TS,
        )
    }
}
