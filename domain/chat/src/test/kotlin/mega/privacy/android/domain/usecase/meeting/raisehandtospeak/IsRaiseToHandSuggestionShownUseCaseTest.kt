package mega.privacy.android.domain.usecase.meeting.raisehandtospeak

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsRaiseToHandSuggestionShownUseCaseTest {
    private val settingsRepository: SettingsRepository = mock()

    private lateinit var underTest: IsRaiseToHandSuggestionShownUseCase

    @BeforeEach
    fun setup() {
        underTest = IsRaiseToHandSuggestionShownUseCase(settingsRepository)
    }

    @ParameterizedTest(name = "expected: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that correct value is returned when invoked`(expected: Boolean) = runTest {
        whenever(settingsRepository.isRaiseToHandSuggestionShown()).thenReturn(expected)
        Truth.assertThat(underTest.invoke()).isEqualTo(expected)
    }

    @Test
    fun `test that false is returned if there is no settings when  invoked`() =
        runTest {
            whenever(settingsRepository.isRaiseToHandSuggestionShown()).thenReturn(null)
            Truth.assertThat(underTest.invoke()).isFalse()
        }
}
