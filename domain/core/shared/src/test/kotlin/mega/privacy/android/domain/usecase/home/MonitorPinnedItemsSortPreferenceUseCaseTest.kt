package mega.privacy.android.domain.usecase.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.home.PinnedHomeItemsSortField
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorPinnedItemsSortPreferenceUseCaseTest {

    private val settingsRepository: SettingsRepository = mock()
    private lateinit var underTest: MonitorPinnedItemsSortPreferenceUseCase

    @BeforeEach
    fun setUp() {
        reset(settingsRepository)
        underTest = MonitorPinnedItemsSortPreferenceUseCase(settingsRepository)
    }

    @Test
    fun `test that invoke emits the repository sort preference`() = runTest {
        val expected = PinnedHomeItemsSortField.Name to SortDirection.Ascending
        whenever(settingsRepository.monitorPinnedItemsSortPreference())
            .thenReturn(flowOf(expected))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
            awaitComplete()
        }
    }
}
