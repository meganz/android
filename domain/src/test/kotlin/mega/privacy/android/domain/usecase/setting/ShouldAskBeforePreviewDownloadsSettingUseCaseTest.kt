package mega.privacy.android.domain.usecase.setting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShouldAskBeforePreviewDownloadsSettingUseCaseTest {
    private lateinit var underTest: ShouldAskBeforePreviewDownloadsSettingUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest =
            ShouldAskBeforePreviewDownloadsSettingUseCase(settingsRepository = settingsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            settingsRepository,
        )
    }

    @ParameterizedTest(name = "expected: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the repository value is returned`(expected: Boolean) = runTest {
        whenever(settingsRepository.monitorAskBeforePreviewDownloads()).thenReturn(flowOf(expected))
        assertThat(underTest()).isEqualTo(expected)
    }
}
