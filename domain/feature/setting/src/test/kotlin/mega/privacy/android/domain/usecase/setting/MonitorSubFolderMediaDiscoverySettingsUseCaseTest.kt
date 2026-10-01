package mega.privacy.android.domain.usecase.setting

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorSubFolderMediaDiscoverySettingsUseCaseTest {
    private lateinit var underTest: MonitorSubFolderMediaDiscoverySettingsUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorSubFolderMediaDiscoverySettingsUseCase(
            settingsRepository = settingsRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            settingsRepository,
        )
    }

    @Test
    fun `test that null values return a default of true`() = runTest {
        settingsRepository.stub {
            on { monitorSubfolderMediaDiscoveryEnabled() }.thenReturn(flowOf(null))
        }

        underTest().test {
            Truth.assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
    }
}