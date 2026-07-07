package mega.privacy.android.domain.usecase.setting

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorSortingPreferenceUseCaseTest {
    private lateinit var underTest: MonitorSortingPreferenceUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorSortingPreferenceUseCase(settingsRepository = settingsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository)
    }

    @Test
    fun `test that a null value returns a default of PerFolder`() = runTest {
        settingsRepository.stub {
            on { monitorSortingPreference() }.thenReturn(flowOf(null))
        }

        underTest().test {
            assertThat(awaitItem()).isEqualTo(SortingPreference.PerFolder)
            awaitComplete()
        }
    }

    @ParameterizedTest
    @EnumSource(SortingPreference::class)
    fun `test that a stored value is returned`(preference: SortingPreference) = runTest {
        settingsRepository.stub {
            on { monitorSortingPreference() }.thenReturn(flowOf(preference))
        }

        underTest().test {
            assertThat(awaitItem()).isEqualTo(preference)
            awaitComplete()
        }
    }
}
