package mega.privacy.android.domain.usecase.texteditor

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetShowLineNumbersPreferenceUseCaseTest {

    private val settingsRepository: SettingsRepository = mock()

    private val underTest = GetShowLineNumbersPreferenceUseCase(settingsRepository)

    @Test
    fun `test that invoke returns true when stored preference is true`() = runTest {
        whenever(
            settingsRepository.monitorBooleanPreference(
                KEY_SHOW_LINE_NUMBERS,
                false,
            )
        ).thenReturn(flowOf(true))

        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that invoke returns false when stored preference is false`() = runTest {
        whenever(
            settingsRepository.monitorBooleanPreference(
                KEY_SHOW_LINE_NUMBERS,
                false,
            )
        ).thenReturn(flowOf(false))

        assertThat(underTest()).isFalse()
    }
}
