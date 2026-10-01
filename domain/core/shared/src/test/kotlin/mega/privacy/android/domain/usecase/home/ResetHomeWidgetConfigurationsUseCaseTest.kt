package mega.privacy.android.domain.usecase.home

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ResetHomeWidgetConfigurationsUseCaseTest {
    private lateinit var underTest: ResetHomeWidgetConfigurationsUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeEach
    fun setUp() {
        underTest = ResetHomeWidgetConfigurationsUseCase(
            settingsRepository = settingsRepository,
        )
    }

    @Test
    fun `test that invoke calls reset home screen widget configurations on the repository`() = runTest {
        underTest()

        verify(settingsRepository).resetHomeScreenWidgetConfigurations()
    }
}
