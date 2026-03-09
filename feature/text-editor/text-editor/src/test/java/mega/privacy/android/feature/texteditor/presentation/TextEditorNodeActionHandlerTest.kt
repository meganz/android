package mega.privacy.android.feature.texteditor.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorNodeActionRequest
import mega.privacy.android.navigation.contract.TransferHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TextEditorNodeActionHandlerTest {

    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val transferHandler: TransferHandler = mock()

    private lateinit var underTest: TextEditorNodeActionHandler

    @BeforeEach
    fun setUp() {
        reset(getNodeByIdUseCase, transferHandler)
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
        }
        underTest = TextEditorNodeActionHandler(getNodeByIdUseCase = getNodeByIdUseCase)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that handle Download when node exists calls setTransferEvent with StartDownloadNode`() = runTest {
        val node = mock<TypedNode>()
        runBlocking {
            whenever(getNodeByIdUseCase(NodeId(42L))).thenReturn(node)
        }
        underTest.handle(
            scope = this,
            request = TextEditorNodeActionRequest.Download(42L),
            transferHandler = transferHandler,
        )
        advanceUntilIdle()

        verify(transferHandler).setTransferEvent(
            argThat { event ->
                event is TransferTriggerEvent.StartDownloadNode &&
                    event.nodes == listOf(node) &&
                    event.withStartMessage == true
            },
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that handle Download when node is null does not call setTransferEvent`() = runTest {
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
        }
        underTest.handle(
            scope = this,
            request = TextEditorNodeActionRequest.Download(99L),
            transferHandler = transferHandler,
        )
        advanceUntilIdle()

        verify(transferHandler, never()).setTransferEvent(any())
    }

    @Test
    fun `test that handle GetLink does not call setTransferEvent`() = runBlocking {
        underTest.handle(
            scope = this,
            request = TextEditorNodeActionRequest.GetLink(1L),
            transferHandler = transferHandler,
        )
        verify(transferHandler, never()).setTransferEvent(any())
    }

    @Test
    fun `test that handle Share does not call setTransferEvent`() = runBlocking {
        underTest.handle(
            scope = this,
            request = TextEditorNodeActionRequest.Share(1L),
            transferHandler = transferHandler,
        )
        verify(transferHandler, never()).setTransferEvent(any())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that handle Download invokes getNodeByIdUseCase with correct NodeId`() = runTest {
        val node = mock<TypedNode>()
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(node)
        }
        underTest.handle(
            scope = this,
            request = TextEditorNodeActionRequest.Download(123L),
            transferHandler = transferHandler,
        )
        advanceUntilIdle()

        verify(getNodeByIdUseCase).invoke(NodeId(123L))
    }
}
