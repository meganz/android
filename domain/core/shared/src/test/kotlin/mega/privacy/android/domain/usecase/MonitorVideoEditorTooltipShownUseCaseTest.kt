package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [MonitorVideoEditorTooltipShownUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorVideoEditorTooltipShownUseCaseTest {

    private lateinit var underTest: MonitorVideoEditorTooltipShownUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorVideoEditorTooltipShownUseCase(
            settingsRepository = settingsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository)
    }

    @Test
    fun `test that invoke returns true when the tooltip has been shown`() = runTest {
        whenever(settingsRepository.monitorVideoEditorTooltipShown())
            .thenReturn(flowOf(true))

        assertThat(underTest().first()).isTrue()
    }

    @Test
    fun `test that invoke returns false when the tooltip has not been shown`() = runTest {
        whenever(settingsRepository.monitorVideoEditorTooltipShown())
            .thenReturn(flowOf(false))

        assertThat(underTest().first()).isFalse()
    }
}
