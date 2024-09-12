package mega.privacy.android.app.presentation.transfers.attach

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.ChatRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.StorageStatePayWallException
import mega.privacy.android.domain.usecase.chat.AttachMultipleNodesUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.chat.GetNodesToAttachUseCase
import mega.privacy.android.domain.usecase.chat.message.AttachContactsUseCase
import mega.privacy.android.domain.usecase.contact.GetContactHandleUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeAttachmentViewModelTest {
    private val getNodesToAttachUseCase: GetNodesToAttachUseCase = mock()
    private val attachMultipleNodesUseCase: AttachMultipleNodesUseCase = mock()
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase = mock()
    private val getContactHandleUseCase: GetContactHandleUseCase = mock()
    private val attachContactsUseCase: AttachContactsUseCase = mock()

    private lateinit var underTest: NodeAttachmentViewModel

    @BeforeAll
    fun setup() {
        underTest = NodeAttachmentViewModel(
            getNodesToAttachUseCase = getNodesToAttachUseCase,
            attachMultipleNodesUseCase = attachMultipleNodesUseCase,
            get1On1ChatIdUseCase = get1On1ChatIdUseCase,
            getContactHandleUseCase = getContactHandleUseCase,
            attachContactsUseCase = attachContactsUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getNodesToAttachUseCase, attachMultipleNodesUseCase, get1On1ChatIdUseCase)
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
    fun `test that attach nodes to chat update state correctly when use case returns success and user handles is empty`() =
        runTest {
            val nodeIds = listOf(NodeId(1), NodeId(2))
            val chatIds = longArrayOf(1, 2)
            underTest.attachNodesToChat(nodeIds, chatIds, longArrayOf())
            verifyNoInteractions(get1On1ChatIdUseCase)
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.event)
                    .isInstanceOf(NodeAttachmentEvent.AttachNodeSuccess::class.java)
                assertThat((state.event as NodeAttachmentEvent.AttachNodeSuccess).chatIds).isEqualTo(
                    chatIds.toList()
                )
            }
        }

    @Test
    fun `test that attach nodes to chat update state correctly when use case returns success and user handles is not empty`() =
        runTest {
            val nodeIds = listOf(NodeId(1), NodeId(2))
            val chatIds = longArrayOf(1, 2)
            val userHandles = longArrayOf(3, 4)
            whenever(get1On1ChatIdUseCase(3)).thenReturn(5)
            whenever(get1On1ChatIdUseCase(4)).thenReturn(6)
            underTest.attachNodesToChat(nodeIds, chatIds, userHandles)
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.event)
                    .isInstanceOf(NodeAttachmentEvent.AttachNodeSuccess::class.java)
                assertThat((state.event as NodeAttachmentEvent.AttachNodeSuccess).chatIds).isEqualTo(
                    listOf(5L, 6L, 1L, 2L)
                )
            }
        }

    @Test
    fun `test that attach nodes to chat by email update state correctly when use case returns success`() =
        runTest {
            val nodeIds = listOf(NodeId(1), NodeId(2))
            val email = "myemail"
            val userHandle = 3L
            val chatId = 4L
            whenever(getContactHandleUseCase(email)).thenReturn(userHandle)
            whenever(get1On1ChatIdUseCase(userHandle)).thenReturn(chatId)
            whenever(attachMultipleNodesUseCase(nodeIds, listOf(chatId))).thenReturn(
                ChatRequestResult.ChatRequestAttachNode(count = nodeIds.size, errorCount = 0)
            )
            underTest.attachNodesToChatByEmail(nodeIds, email)
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.event)
                    .isInstanceOf(NodeAttachmentEvent.AttachNodeSuccess::class.java)
                assertThat((state.event as NodeAttachmentEvent.AttachNodeSuccess).chatIds).isEqualTo(
                    listOf(chatId)
                )
            }
        }

    @Test
    fun `test that attach contacts to chat update state correctly when use case returns success`() =
        runTest {
            val chatIds = longArrayOf(1, 2)
            val userHandles = longArrayOf(3)
            val email = "myemail"
            whenever(get1On1ChatIdUseCase(3)).thenReturn(4L)
            whenever(attachContactsUseCase(any(), any())).thenReturn(Unit)
            underTest.attachContactToChat(email, chatIds, userHandles)
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.event)
                    .isInstanceOf(NodeAttachmentEvent.AttachNodeSuccess::class.java)
                assertThat((state.event as NodeAttachmentEvent.AttachNodeSuccess).chatIds).isEqualTo(
                    listOf(4L, 1L, 2L)
                )
            }
        }
}