package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMonitorStartScreenPreferenceTest {
    private lateinit var underTest: MonitorStartScreenPreference

    private val settingsRepository = mock<SettingsRepository>()

    @Before
    fun setUp() {
        underTest = DefaultMonitorStartScreenPreference(settingsRepository = settingsRepository)
    }

    @Test
    fun `test that a value is returned if set`() = runTest {
        val expected = StartScreen.values()
        whenever(settingsRepository.monitorPreferredStartScreen()).thenReturn(
            expected.asFlow()
        )

        underTest().test {
            expected.forEach {
                assertThat(awaitItem()).isEqualTo(it)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that a default value of Home is returned if no value is set`() = runTest {
        whenever(settingsRepository.monitorPreferredStartScreen()).thenReturn(flowOf(null))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(StartScreen.Home)
            cancelAndIgnoreRemainingEvents()
        }
    }
}