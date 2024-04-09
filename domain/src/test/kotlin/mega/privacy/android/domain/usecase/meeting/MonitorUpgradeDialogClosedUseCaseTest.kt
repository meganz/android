package mega.privacy.android.domain.usecase.meeting

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorUpgradeDialogClosedUseCaseTest {

    private val chatRepository = mock<ChatRepository>()
    private lateinit var underTest: MonitorUpgradeDialogClosedUseCase

    @BeforeEach
    internal fun setUp() {
        underTest = MonitorUpgradeDialogClosedUseCase(chatRepository)
    }

    @Test
    fun `test that it emits value when invoked `() = runTest {
        whenever(chatRepository.monitorUpgradeDialogClosed()).thenReturn(flowOf(Unit))
        underTest().test {
            Truth.assertThat(awaitItem()).isEqualTo(Unit)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
