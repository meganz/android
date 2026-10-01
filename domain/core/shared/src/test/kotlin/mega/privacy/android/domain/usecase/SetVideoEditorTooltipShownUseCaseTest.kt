package mega.privacy.android.domain.usecase

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SetVideoEditorTooltipShownUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetVideoEditorTooltipShownUseCaseTest {

    private lateinit var underTest: SetVideoEditorTooltipShownUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetVideoEditorTooltipShownUseCase(
            settingsRepository = settingsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository)
    }

    @Test
    fun `test that invoke marks the tooltip as shown via the settings repository`() = runTest {
        underTest()
        verify(settingsRepository).setVideoEditorTooltipShown(true)
    }
}
