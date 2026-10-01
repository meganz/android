package mega.privacy.android.domain.usecase.preference

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetCustomiseNavigationTooltipShownUseCaseTest {
    private lateinit var underTest: SetCustomiseNavigationTooltipShownUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetCustomiseNavigationTooltipShownUseCase(
            settingsRepository = settingsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository)
    }

    @Test
    fun `test that invoke calls repository`() = runTest {
        underTest()

        verify(settingsRepository).setCustomiseNavigationTooltipShown()
    }
}
