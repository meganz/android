package mega.privacy.android.domain.usecase.shares

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorContactInSharesCountUseCaseTest {
    private lateinit var underTest: MonitorContactInSharesCountUseCase

    private val nodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
    private val getInSharesUseCase = mock<GetInSharesUseCase>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()

    private val email = "contact@mega.nz"

    @BeforeAll
    fun setUp() {
        underTest = MonitorContactInSharesCountUseCase(
            getInSharesUseCase = getInSharesUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getInSharesUseCase, monitorNodeUpdatesUseCase)
        whenever(monitorNodeUpdatesUseCase()).thenReturn(nodeUpdatesFlow)
    }

    @Test
    fun `test that invoke emits the current in shares count initially`() = runTest {
        whenever(getInSharesUseCase(email)).thenReturn(listOf(mock<FolderNode>()))

        underTest(email).test {
            assertThat(awaitItem()).isEqualTo(1)
        }
    }

    @Test
    fun `test that invoke refreshes the count when an incoming share update is received`() =
        runTest {
            whenever(getInSharesUseCase(email)).thenReturn(emptyList())

            underTest(email).test {
                assertThat(awaitItem()).isEqualTo(0)
                whenever(getInSharesUseCase(email))
                    .thenReturn(listOf(mock<FolderNode>(), mock<FolderNode>()))
                nodeUpdatesFlow.emit(incomingShareUpdate())
                assertThat(awaitItem()).isEqualTo(2)
            }
        }

    @Test
    fun `test that invoke refreshes the count when a node removal update is received`() = runTest {
        whenever(getInSharesUseCase(email)).thenReturn(listOf(mock<FolderNode>()))

        underTest(email).test {
            assertThat(awaitItem()).isEqualTo(1)
            whenever(getInSharesUseCase(email)).thenReturn(emptyList())
            nodeUpdatesFlow.emit(removalUpdate())
            assertThat(awaitItem()).isEqualTo(0)
        }
    }

    @Test
    fun `test that invoke ignores node updates that are not incoming shares or removals`() =
        runTest {
            whenever(getInSharesUseCase(email)).thenReturn(listOf(mock<FolderNode>()))

            underTest(email).test {
                assertThat(awaitItem()).isEqualTo(1)
                nodeUpdatesFlow.emit(unrelatedUpdate())
                expectNoEvents()
            }
        }

    private fun incomingShareUpdate(): NodeUpdate {
        val node = mock<Node> {
            on { isIncomingShare } doReturn true
        }
        return NodeUpdate(changes = mapOf(node to listOf(NodeChanges.New)))
    }

    private fun removalUpdate(): NodeUpdate {
        val node = mock<Node> {
            on { isIncomingShare } doReturn false
        }
        return NodeUpdate(changes = mapOf(node to listOf(NodeChanges.Remove)))
    }

    private fun unrelatedUpdate(): NodeUpdate {
        val node = mock<Node> {
            on { isIncomingShare } doReturn false
        }
        return NodeUpdate(changes = mapOf(node to listOf(NodeChanges.Name)))
    }
}
