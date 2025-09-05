package mega.privacy.android.domain.usecase.setting

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class MonitorMiscLoadedUseCaseTest {
    private lateinit var underTest: MonitorMiscLoadedUseCase

    private val accountRepository = mock<AccountRepository>()

    @BeforeEach
    fun setUp() {
        underTest = MonitorMiscLoadedUseCase(
            accountRepository = accountRepository
        )
    }

    @Test
    fun `test that true causes use case to emit`() = runTest {
        accountRepository.stub {
            on { monitorMiscLoaded() } doReturn flow {
                emit(true)
                awaitCancellation()
            }
        }

        underTest().test {
            assertThat(cancelAndConsumeRemainingEvents()).hasSize(1)
        }
    }

    @Test
    fun `test that false causes use case to not emit`() = runTest {
        accountRepository.stub {
            on { monitorMiscLoaded() } doReturn flow {
                emit(false)
                awaitCancellation()
            }
        }

        underTest().test {
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }
}
