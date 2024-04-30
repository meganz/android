package mega.privacy.android.app.presentation.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.copynode.CopyRequestState
import mega.privacy.android.app.presentation.copynode.toCopyRequestResult
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CopyChatNodesUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeAttachmentHistoryViewModelTest {

    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val copyChatNodesUseCase: CopyChatNodesUseCase = mock()

    private lateinit var viewModel: NodeAttachmentHistoryViewModel

    @BeforeEach
    fun setup() {
        viewModel = NodeAttachmentHistoryViewModel(
            monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase,
            copyChatNodesUseCase
        )
    }

    @AfterEach
    fun resetMocks() = reset(
        monitorStorageStateEventUseCase,
        isConnectedToInternetUseCase,
        copyChatNodesUseCase
    )

    @Test
    fun `test that copyChatNodes updates _copyResultFlow on success`() = runTest {
        val chatId = 123L
        val messageIds = mutableListOf(456L, 789L)
        val newNodeParent = 321L
        val result = MoveRequestResult.Copy(1, 0)
        whenever(copyChatNodesUseCase.invoke(any(), any(), NodeId(any()))).thenReturn(result)

        viewModel.copyChatNodes(chatId, messageIds, newNodeParent)

        val expectedState = CopyRequestState(result = result.toCopyRequestResult())
        assertThat(expectedState).isEqualTo(viewModel.copyResultFlow.value)
    }

    @Test
    fun `test that copyChatNodes updates _copyResultFlow on failure`() = runTest {
        val chatId = 123L
        val messageIds = mutableListOf(456L, 789L)
        val newNodeParent = 321L
        whenever(copyChatNodesUseCase.invoke(any(), any(), NodeId(any())))
            .thenThrow(ForeignNodeException())

        viewModel.copyChatNodes(chatId, messageIds, newNodeParent)

        assertThat(viewModel.copyResultFlow.value?.error).isInstanceOf(ForeignNodeException::class.java)
    }
}