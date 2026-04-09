package mega.privacy.android.domain.usecase.texteditor

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetShowLineNumbersPreferenceUseCaseTest {

    private val settingsRepository: SettingsRepository = mock()

    private val underTest = SetShowLineNumbersPreferenceUseCase(settingsRepository)

    @Test
    fun `test that preference is set to true when invoke is called with true`() = runTest {
        underTest(true)

        verify(settingsRepository).setBooleanPreference(KEY_SHOW_LINE_NUMBERS, true)
    }

    @Test
    fun `test that preference is set to false when invoke is called with false`() = runTest {
        underTest(false)

        verify(settingsRepository).setBooleanPreference(KEY_SHOW_LINE_NUMBERS, false)
    }
}
