package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMonitorHideRecentActivityTest {
    private lateinit var underTest: MonitorHideRecentActivity

    private val settingsRepository = mock<SettingsRepository>()

    @Before
    fun setUp() {
        underTest = DefaultMonitorHideRecentActivity(settingsRepository = settingsRepository)
    }

    @Test
    fun `test that null values return a default of false`() = runTest {
        settingsRepository.stub {
            on { monitorHideRecentActivity() }.thenReturn(flowOf(null))
        }

        underTest().test {
            assertThat(awaitItem()).isFalse()
            awaitComplete()
        }
    }
}