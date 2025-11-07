package mega.privacy.android.domain.usecase.setting

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.featureflag.MiscLoadedState
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
    fun `test that true is returned when FlagsReady`() = runTest {
        val stateFlow = MutableStateFlow(MiscLoadedState.FlagsReady)
        accountRepository.stub {
            on { monitorMiscState() } doReturn stateFlow
        }

        underTest().test {
            assertThat(awaitItem()).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that nothing is emitted when NotLoaded`() = runTest {
        val stateFlow = MutableStateFlow(MiscLoadedState.NotLoaded)
        accountRepository.stub {
            on { monitorMiscState() } doReturn stateFlow
        }

        underTest().test {
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }
}
