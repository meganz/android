package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class SetSortingPreferenceUseCaseTest {
    private lateinit var underTest: SetSortingPreferenceUseCase
    private val settingsRepository = mock<SettingsRepository>()

    @BeforeEach
    fun setUp() {
        reset(settingsRepository)
        underTest = SetSortingPreferenceUseCase(settingsRepository = settingsRepository)
    }

    @ParameterizedTest
    @EnumSource(SortingPreference::class)
    fun `test that the preference is updated`(preference: SortingPreference) = runTest {
        underTest(preference)

        verify(settingsRepository).setSortingPreference(preference)
    }
}
