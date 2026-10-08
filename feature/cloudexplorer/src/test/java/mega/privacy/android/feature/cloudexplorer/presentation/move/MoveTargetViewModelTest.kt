package mega.privacy.android.feature.cloudexplorer.presentation.move

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNavigationStack
import mega.privacy.android.domain.usecase.node.GetNodeNavigationStackUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class MoveTargetViewModelTest {

    private val getNodeNavigationStackUseCase = mock<GetNodeNavigationStackUseCase>()

    @AfterEach
    fun resetMocks() {
        reset(getNodeNavigationStackUseCase)
    }

    private fun buildViewModel() = MoveTargetViewModel(
        getNodeNavigationStackUseCase = getNodeNavigationStackUseCase,
        args = MoveTargetViewModel.Args(targetId = TARGET_ID, movedNodeIds = MOVED_IDS),
    )

    @Test
    fun `test that isInsideMovedNodes is true when the target is inside the moved nodes`() =
        runTest {
            whenever(getNodeNavigationStackUseCase(TARGET_ID)).thenReturn(
                NodeNavigationStack(stack = listOf(NodeId(10L), MOVED_IDS.first(), TARGET_ID))
            )

            buildViewModel().isInsideMovedNodes.test {
                assertThat(expectMostRecentItem()).isTrue()
            }
        }

    @Test
    fun `test that isInsideMovedNodes is true when the target is one of the moved nodes`() =
        runTest {
            val movedFolder = MOVED_IDS.last()
            whenever(getNodeNavigationStackUseCase(movedFolder)).thenReturn(
                NodeNavigationStack(stack = listOf(NodeId(10L), movedFolder))
            )
            val underTest = MoveTargetViewModel(
                getNodeNavigationStackUseCase = getNodeNavigationStackUseCase,
                args = MoveTargetViewModel.Args(targetId = movedFolder, movedNodeIds = MOVED_IDS),
            )

            underTest.isInsideMovedNodes.test {
                assertThat(expectMostRecentItem()).isTrue()
            }
        }

    @Test
    fun `test that isInsideMovedNodes is false when the target is outside the moved nodes`() =
        runTest {
            whenever(getNodeNavigationStackUseCase(TARGET_ID)).thenReturn(
                NodeNavigationStack(stack = listOf(NodeId(10L), TARGET_ID))
            )

            buildViewModel().isInsideMovedNodes.test {
                assertThat(expectMostRecentItem()).isFalse()
            }
        }

    @Test
    fun `test that isInsideMovedNodes is false when the check fails`() = runTest {
        whenever(getNodeNavigationStackUseCase(TARGET_ID)).thenThrow(RuntimeException("boom"))

        buildViewModel().isInsideMovedNodes.test {
            assertThat(expectMostRecentItem()).isFalse()
        }
    }

    @Test
    fun `test that isInsideMovedNodes is initially true until the check completes`() {
        assertThat(buildViewModel().isInsideMovedNodes.value).isTrue()
    }

    private companion object {
        val TARGET_ID = NodeId(3L)
        val MOVED_IDS = listOf(NodeId(1L), NodeId(2L))
    }
}
