package mega.privacy.android.domain.usecase.offline

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
class SetOfflineWarningMessageVisibilityUseCaseTest {
    private lateinit var underTest: SetOfflineWarningMessageVisibilityUseCase
    private val settingsRepository = mock<SettingsRepository>()

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository)
    }

    @BeforeAll
    private fun setUp() {
        underTest = SetOfflineWarningMessageVisibilityUseCase(
            settingsRepository = settingsRepository
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that preference is updated`(input: Boolean) = runTest {
        underTest(input)
        verify(settingsRepository).setOfflineWarningMessageVisibility(input)
    }
}