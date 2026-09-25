package mega.privacy.android.domain.usecase.meeting.raisehandtospeak

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetRaiseToHandSuggestionShownUseCaseTest {
    private val settingsRepository: SettingsRepository = mock()

    private lateinit var underTest: SetRaiseToHandSuggestionShownUseCase

    @BeforeEach
    fun setup() {
        underTest = SetRaiseToHandSuggestionShownUseCase(settingsRepository)
    }

    @Test
    fun `test that value is set when invoked`() = runTest {
        underTest.invoke()

        verify(settingsRepository).setRaiseToHandSuggestionShown()
    }
}
