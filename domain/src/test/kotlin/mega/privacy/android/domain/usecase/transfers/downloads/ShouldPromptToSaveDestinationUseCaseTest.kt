package mega.privacy.android.domain.usecase.transfers.downloads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShouldPromptToSaveDestinationUseCaseTest {
    private lateinit var underTest: ShouldPromptToSaveDestinationUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setup() {
        underTest = ShouldPromptToSaveDestinationUseCase(settingsRepository)
    }

    @BeforeEach
    fun resetMocks() = reset(settingsRepository)

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that invoke returns AskDownloadLocation from settings repository`(
        expected: Boolean,
    ) = runTest {
        whenever(settingsRepository.isAskSetDownloadLocation()).thenReturn(expected)
        val actual = underTest()
        assertThat(actual).isEqualTo(expected)
    }

}