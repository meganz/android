package mega.privacy.android.app.presentation.login.onboarding.view

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TourViewModelTest {

    private lateinit var underTest: TourViewModel

    private val monitorThemeModeUseCase: MonitorThemeModeUseCase = mock {
        onBlocking { invoke() } doReturn flowOf(ThemeMode.System)
    }

    @BeforeEach
    fun setUp() {
        underTest = TourViewModel(
            monitorThemeModeUseCase = monitorThemeModeUseCase,
        )
    }

    @Test
    fun `test that ui state is initialized with default theme mode`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem().themeMode).isEqualTo(ThemeMode.System)
        }
    }

    @Test
    fun `test that theme mode is updated when monitor theme mode use case emits new value`() =
        runTest {
            val newThemeMode = ThemeMode.Dark
            val monitorThemeModeUseCase: MonitorThemeModeUseCase = mock {
                onBlocking { invoke() } doReturn flowOf(newThemeMode)
            }

            underTest = TourViewModel(monitorThemeModeUseCase = monitorThemeModeUseCase)

            underTest.uiState.test {
                assertThat(awaitItem().themeMode).isEqualTo(newThemeMode)
            }
        }
}
