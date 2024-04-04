package mega.privacy.android.domain.usecase.setting

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorShowHiddenItemsUseCaseTest {
    private lateinit var underTest: MonitorShowHiddenItemsUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorShowHiddenItemsUseCase(settingsRepository = settingsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            settingsRepository,
        )
    }

    @Test
    fun `test that null values return a default of false`() = runTest {
        settingsRepository.stub {
            on { monitorShowHiddenItems() }.thenReturn(flowOf(true))
        }

        underTest().test {
            assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
    }
}
