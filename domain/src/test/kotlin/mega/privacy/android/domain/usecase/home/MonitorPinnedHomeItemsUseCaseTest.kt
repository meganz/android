package mega.privacy.android.domain.usecase.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.home.PinnedHomeItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorPinnedHomeItemsUseCaseTest {
    private lateinit var underTest: MonitorPinnedHomeItemsUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorPinnedHomeItemsUseCase(settingsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository)
    }

    @Test
    fun `test that invoke emits the items from the repository`() = runTest {
        val expected = listOf(
            PinnedHomeItem(NodeId(1L), "Clients", isFolder = true, pinnedAt = 10L),
            PinnedHomeItem(NodeId(2L), "notes.txt", isFolder = false, pinnedAt = 11L),
        )
        whenever(settingsRepository.monitorPinnedHomeItems()).thenReturn(
            flow {
                emit(expected)
                awaitCancellation()
            }
        )

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }

    @Test
    fun `test that invoke emits an empty list when there are no pinned items`() = runTest {
        whenever(settingsRepository.monitorPinnedHomeItems()).thenReturn(
            flow {
                emit(emptyList())
                awaitCancellation()
            }
        )

        underTest().test {
            assertThat(awaitItem()).isEmpty()
        }
    }
}
