package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.preference.ViewModePreference
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class SetViewModePreferenceUseCaseTest {
    private lateinit var underTest: SetViewModePreferenceUseCase
    private val settingsRepository = mock<SettingsRepository>()

    @BeforeEach
    fun setUp() {
        reset(settingsRepository)
        underTest = SetViewModePreferenceUseCase(settingsRepository = settingsRepository)
    }

    @ParameterizedTest
    @EnumSource(ViewModePreference::class)
    fun `test that the preference is updated`(preference: ViewModePreference) = runTest {
        underTest(preference)

        verify(settingsRepository).setViewModePreference(preference)
    }
}
