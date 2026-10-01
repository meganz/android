package mega.privacy.android.domain.usecase.preference

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.preference.NavigationItemsPreference
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorNavigationItemsPreferenceUseCaseTest {
    private lateinit var underTest: MonitorNavigationItemsPreferenceUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorNavigationItemsPreferenceUseCase(
            settingsRepository = settingsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository)
    }

    @Test
    fun `test that invoke emits preference from repository`() = runTest {
        val expected = NavigationItemsPreference(
            orderedVisibleItemIds = listOf("home", "photos", "chat"),
        )
        whenever(settingsRepository.monitorNavigationItemsPreference())
            .thenReturn(flowOf(expected))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke emits null when preference is not set`() = runTest {
        whenever(settingsRepository.monitorNavigationItemsPreference())
            .thenReturn(flowOf(null))

        underTest().test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }
}
