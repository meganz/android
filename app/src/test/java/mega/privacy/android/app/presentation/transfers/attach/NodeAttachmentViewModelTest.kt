package mega.privacy.android.app.presentation.transfers.attach

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.StorageStatePayWallException
import mega.privacy.android.domain.usecase.chat.AttachMultipleNodesUseCase
import mega.privacy.android.domain.usecase.chat.GetNodesToAttachUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeAttachmentViewModelTest {
    private val getNodesToAttachUseCase: GetNodesToAttachUseCase = mock()
    private val attachMultipleNodesUseCase: AttachMultipleNodesUseCase = mock()

    private lateinit var underTest: NodeAttachmentViewModel

    @BeforeAll
    fun setup() {
        underTest = NodeAttachmentViewModel(getNodesToAttachUseCase, attachMultipleNodesUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(getNodesToAttachUseCase, attachMultipleNodesUseCase)
    }

    @Test
    fun `test that start attach nodes update state correctly`() = runTest {
        val nodeIds = listOf(NodeId(1), NodeId(2))
        underTest.startAttachNodes(nodeIds)
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.event).isInstanceOf(NodeAttachmentEvent.AttachNode::class.java)
            assertThat((state.event as NodeAttachmentEvent.AttachNode).nodeIds).isEqualTo(nodeIds)
        }
    }

    @Test
    fun `test that get nodes to attach throw ShowOverDiskQuotaPaywall event`() = runTest {
        val nodeIds = listOf(NodeId(1), NodeId(2))
        whenever(getNodesToAttachUseCase(nodeIds)).thenAnswer {
            throw StorageStatePayWallException()
        }
        underTest.getNodesToAttach(nodeIds)
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.event).isInstanceOf(NodeAttachmentEvent.ShowOverDiskQuotaPaywall::class.java)
        }
    }

    @Test
    fun `test that get nodes to attach update state correctly when use case returns success`() =
        runTest {
            val nodeIds = listOf(NodeId(1), NodeId(2))
            val copiedNodeIds = listOf(NodeId(3), NodeId(4))
            whenever(getNodesToAttachUseCase(nodeIds)).thenReturn(copiedNodeIds)
            underTest.getNodesToAttach(nodeIds)
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.event)
                    .isInstanceOf(NodeAttachmentEvent.SelectChat::class.java)
                assertThat((state.event as NodeAttachmentEvent.SelectChat).nodeIds).isEqualTo(
                    copiedNodeIds
                )
            }
        }

    @Test
    fun `test that attach nodes to chat update state correctly when use case returns success`() =
        runTest {
            val nodeIds = listOf(NodeId(1), NodeId(2))
            val chatIds = longArrayOf(1, 2)
            underTest.attachNodesToChat(nodeIds, chatIds)
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.event)
                    .isInstanceOf(NodeAttachmentEvent.AttachNodeSuccess::class.java)
                assertThat((state.event as NodeAttachmentEvent.AttachNodeSuccess).chatIds).isEqualTo(
                    chatIds.toList()
                )
            }
        }
}