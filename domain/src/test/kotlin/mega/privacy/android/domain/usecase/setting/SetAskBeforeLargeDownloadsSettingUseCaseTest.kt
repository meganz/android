package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetAskBeforeLargeDownloadsSettingUseCaseTest {
    private lateinit var underTest: SetAskBeforeLargeDownloadsSettingUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest =
            SetAskBeforeLargeDownloadsSettingUseCase(settingsRepository = settingsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            settingsRepository,
        )
    }

    @ParameterizedTest(name = "expected: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the value is set to the repository`(expected: Boolean) = runTest {
        underTest(expected)
        verify(settingsRepository).setAskBeforeLargeDownloads(expected)
    }
}
