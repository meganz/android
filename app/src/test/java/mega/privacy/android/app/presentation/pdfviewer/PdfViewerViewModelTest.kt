package mega.privacy.android.app.presentation.pdfviewer

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionWithActionResult
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.pdf.LastPageViewedInPdf
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.file.GetDataBytesFromUrlUseCase
import mega.privacy.android.domain.usecase.node.CheckChatNodesNameCollisionAndCopyUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionWithActionUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.pdf.GetLastPageViewedInPdfUseCase
import mega.privacy.android.domain.usecase.pdf.SetOrUpdateLastPageViewedInPdfUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferOverQuotaUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import mega.privacy.android.shared.resources.R as sharedResR

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(InstantTaskExecutorExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PdfViewerViewModelTest {

    private lateinit var underTest: PdfViewerViewModel

    private val appScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())
    private val checkNodesNameCollisionWithActionUseCase =
        mock<CheckNodesNameCollisionWithActionUseCase>()
    private val checkChatNodesNameCollisionAndCopyUseCase =
        mock<CheckChatNodesNameCollisionAndCopyUseCase>()
    private val getDataBytesFromUrlUseCase: GetDataBytesFromUrlUseCase = mock()
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase = mock()
    private val isAvailableOfflineUseCase = mock<IsAvailableOfflineUseCase>()
    private val getChatFileUseCase = mock<GetChatFileUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
        on {
            invoke()
        }.thenReturn(flowOf(AccountDetail()))
    }
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase> {
        onBlocking {
            invoke()
        }.thenReturn(false)
    }
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>() {
        onBlocking {
            invoke(any())
        }.thenReturn(false)
    }
    private var savedStateHandle = mock<SavedStateHandle>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val getLastPageViewedInPdfUseCase = mock<GetLastPageViewedInPdfUseCase>()
    private val setOrUpdateLastPageViewedInPdfUseCase =
        mock<SetOrUpdateLastPageViewedInPdfUseCase>()
    private val broadcastTransferOverQuotaUseCase = mock<BroadcastTransferOverQuotaUseCase>()

    @BeforeEach
    fun setUp() {
        initTest()
    }

    private fun initTest() {
        underTest = PdfViewerViewModel(
            appScope = appScope,
            getDataBytesFromUrlUseCase = getDataBytesFromUrlUseCase,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            checkNodesNameCollisionWithActionUseCase = checkNodesNameCollisionWithActionUseCase,
            checkChatNodesNameCollisionAndCopyUseCase = checkChatNodesNameCollisionAndCopyUseCase,
            isAvailableOfflineUseCase = isAvailableOfflineUseCase,
            getChatFileUseCase = getChatFileUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            savedStateHandle = savedStateHandle,
            getLastPageViewedInPdfUseCase = getLastPageViewedInPdfUseCase,
            setOrUpdateLastPageViewedInPdfUseCase = setOrUpdateLastPageViewedInPdfUseCase,
            broadcastTransferOverQuotaUseCase = broadcastTransferOverQuotaUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            checkNodesNameCollisionWithActionUseCase,
            checkChatNodesNameCollisionAndCopyUseCase,
            getChatFileUseCase,
            isAvailableOfflineUseCase,
            getLastPageViewedInPdfUseCase,
            setOrUpdateLastPageViewedInPdfUseCase,
            broadcastTransferOverQuotaUseCase,
        )
    }

    @Test
    internal fun `test that copy complete snack bar message is shown when chat node is imported to different directory`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            val chatId = 1000L
            val messageId = 2000L
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = newParentNode,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )
            underTest.importChatNode(
                chatId = chatId,
                messageId = messageId,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.snackBarMessage)
                    .isEqualTo(R.string.context_correctly_copied)
            }
        }

    @Test
    internal fun `test that copy error snack bar message is shown when chat node is not imported to different directory`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            val chatId = 1000L
            val messageId = 2000L
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = newParentNode,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )
            underTest.importChatNode(
                chatId = chatId,
                messageId = messageId,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.snackBarMessage)
                    .isEqualTo(R.string.context_no_copied)
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when import failed`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            val chatId = 1000L
            val messageId = 2000L
            val runtimeException = RuntimeException("Import node failed")
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = newParentNode,
                )
            ).thenThrow(runtimeException)

            underTest.importChatNode(
                chatId = chatId,
                messageId = messageId,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.nodeCopyError).isEqualTo(runtimeException)
            }
        }

    @Test
    internal fun `test move complete snack bar message is shown when node is moved to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.snackBarMessage).isEqualTo(sharedResR.string.node_moved_success_message)
            }
        }


    @Test
    internal fun `test move error snack bar message is shown when node is not moved to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.snackBarMessage).isEqualTo(R.string.context_no_moved)
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when move failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val runtimeException = RuntimeException("Move node failed")
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ).thenThrow(runtimeException)
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.nodeMoveError)
                    .isInstanceOf(RuntimeException::class.java)
            }
        }

    @Test
    internal fun `test copy complete snack bar message is shown when node is copied to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.snackBarMessage)
                    .isEqualTo(R.string.context_correctly_copied)
            }
        }

    @Test
    internal fun `test copy error snack bar message is shown when node is not copied to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.snackBarMessage)
                    .isEqualTo(R.string.context_no_copied)
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when copy failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val runtimeException = RuntimeException("Copy node failed")
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ).thenThrow(runtimeException)
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.nodeCopyError)
                    .isInstanceOf(RuntimeException::class.java)
            }
        }

    @Test
    internal fun `test that snackbar message is shown when chat file is already available offline`() =
        runTest {
            val chatId = 1000L
            val messageId = 2000L
            val chatFile = mock<ChatDefaultFile>()
            whenever(getChatFileUseCase(chatId, messageId)).thenReturn(chatFile)
            whenever(isAvailableOfflineUseCase(chatFile)).thenReturn(true)

            underTest.saveChatNodeToOffline(chatId, messageId)
            advanceUntilIdle()

            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.snackBarMessage)
                    .isEqualTo(R.string.file_already_exists)
            }
        }

    @Test
    internal fun `test that startChatOfflineDownloadEvent event is triggered when chat file is not available offline`() =
        runTest {
            val chatId = 1000L
            val messageId = 2000L
            val chatFile = mock<ChatDefaultFile>()
            whenever(getChatFileUseCase(chatId, messageId)).thenReturn(chatFile)
            whenever(isAvailableOfflineUseCase(chatFile)).thenReturn(false)

            underTest.saveChatNodeToOffline(chatId, messageId)
            advanceUntilIdle()

            underTest.uiState.test {
                val actual = awaitItem()
                val event = actual.startChatOfflineDownloadEvent
                assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                val content = (event as StateEventWithContentTriggered).content
                assertThat(content).isEqualTo(chatFile)
            }
        }

    @Test
    internal fun `test that lastPageViewed is updated if handle exists`() = runTest {
        val expectedPage = 5L
        val handle = 123456789L
        savedStateHandle = SavedStateHandle(mapOf("HANDLE" to handle))

        whenever(getLastPageViewedInPdfUseCase(handle)).thenReturn(expectedPage)

        initTest()
        advanceUntilIdle()

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.lastPageViewed).isEqualTo(expectedPage)
        }
    }

    @Test
    internal fun `test that lastPageViewed is 1 if handle is invalid`() = runTest {
        savedStateHandle = SavedStateHandle()

        initTest()
        advanceUntilIdle()

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.lastPageViewed).isEqualTo(1L)
        }

        verifyNoInteractions(getLastPageViewedInPdfUseCase)
    }

    @Test
    internal fun `test that lastPageViewed is updated when setOrUpdateLastPageViewedInPdfUseCase is called`() =
        runTest {
            val lastPageViewed = 5L
            val handle = 123456789L
            val lastPageViewedInPdf = LastPageViewedInPdf(
                nodeHandle = handle,
                lastPageViewed = lastPageViewed
            )
            savedStateHandle = SavedStateHandle(mapOf("HANDLE" to handle))

            initTest()
            advanceUntilIdle()

            underTest.setOrUpdateLastPageViewed(lastPageViewed)

            verify(setOrUpdateLastPageViewedInPdfUseCase).invoke(lastPageViewedInPdf)
        }

    @Test
    internal fun `test that lastPageViewed is not updated when setOrUpdateLastPageViewedInPdfUseCase is called with invalid handle`() =
        runTest {
            val lastPageViewed = 5L
            savedStateHandle = SavedStateHandle()

            initTest()
            advanceUntilIdle()

            underTest.setOrUpdateLastPageViewed(lastPageViewed)

            verifyNoInteractions(setOrUpdateLastPageViewedInPdfUseCase)
        }

    @Test
    internal fun `test that lastPageViewed is updated in state when setOrUpdateLastPageViewedInPdfUseCase is called`() =
        runTest {
            val lastPageViewed = 5L
            val handle = 123456789L
            savedStateHandle = SavedStateHandle(mapOf("HANDLE" to handle))

            initTest()
            advanceUntilIdle()

            underTest.setOrUpdateLastPageViewed(lastPageViewed)
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.lastPageViewed).isEqualTo(lastPageViewed)
            }
        }

    @Test
    internal fun `test that lastPageViewed is updated in state when setOrUpdateLastPageViewedInPdfUseCase is called with invalid handle`() =
        runTest {
            val lastPageViewed = 5L
            savedStateHandle = SavedStateHandle()

            initTest()
            advanceUntilIdle()

            underTest.setOrUpdateLastPageViewed(lastPageViewed)
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.lastPageViewed).isEqualTo(lastPageViewed)
            }
        }


    @Test
    internal fun `test that setPdfUriData updates the state with the provided URI`() = runTest {
        val uri = mock<Uri>()

        initTest()
        advanceUntilIdle()

        underTest.setPdfUriData(uri)

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.pdfUriData).isEqualTo(uri)
        }
    }

    @Test
    internal fun `test that broadcastTransferOverQuota invokes correctly`() = runTest {
        underTest.broadcastTransferOverQuota()
        advanceUntilIdle()

        verify(broadcastTransferOverQuotaUseCase).invoke(true)
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}