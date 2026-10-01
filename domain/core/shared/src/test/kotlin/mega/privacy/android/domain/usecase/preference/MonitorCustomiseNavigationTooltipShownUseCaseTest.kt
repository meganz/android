package mega.privacy.android.domain.usecase.preference

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorCustomiseNavigationTooltipShownUseCaseTest {
    private lateinit var underTest: MonitorCustomiseNavigationTooltipShownUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorCustomiseNavigationTooltipShownUseCase(
            settingsRepository = settingsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository)
    }

    @ParameterizedTest(name = "shown: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that invoke emits value from repository`(shown: Boolean) = runTest {
        whenever(settingsRepository.monitorCustomiseNavigationTooltipShown())
            .thenReturn(flowOf(shown))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(shown)
            awaitComplete()
        }
    }
}
