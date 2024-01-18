package mega.privacy.android.domain.usecase.transfers.chatuploads

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.UploadFilesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartChatUploadsWithWorkerUseCaseTest {
    private lateinit var underTest: StartChatUploadsWithWorkerUseCase

    private val uploadFilesUseCase = mock<UploadFilesUseCase>()
    private val getMyChatsFilesFolderIdUseCase = mock<GetMyChatsFilesFolderIdUseCase>()
    private val cancelCancelTokenUseCase = mock<CancelCancelTokenUseCase>()
    private val startChatUploadsWorkerUseCase = mock<StartChatUploadsWorkerUseCase>()
    private val isChatUploadsWorkerStartedUseCase = mock<IsChatUploadsWorkerStartedUseCase>()

    @BeforeAll
    fun setup() {
        underTest = StartChatUploadsWithWorkerUseCase(
            uploadFilesUseCase,
            getMyChatsFilesFolderIdUseCase,
            startChatUploadsWorkerUseCase,
            isChatUploadsWorkerStartedUseCase,
            cancelCancelTokenUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            uploadFilesUseCase,
            getMyChatsFilesFolderIdUseCase,
            startChatUploadsWorkerUseCase,
            isChatUploadsWorkerStartedUseCase,
            cancelCancelTokenUseCase,
        )
        commonStub()
    }

    private suspend fun commonStub() {
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(1L))
    }

    @Test
    fun `test that files are filtered and send to upload files use case`() = runTest {

        val files: List<File> = (0..10).map { mockFile() }
        val folders: List<File> = (0..10).map { mockFolder() }
        underTest((files + folders), 1L).test {
            verify(uploadFilesUseCase).invoke(eq(files), NodeId(any()), any(), any(), any())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that filtered folders emit TransferNotStarted event`() = runTest {

        val files: List<File> = (0..2).map {
            mock { _ ->
                on { isFile }.thenReturn(true)
            }
        }
        val folders: List<File> = (0..10).map {
            mock { _ ->
                on { isFile }.thenReturn(false)
            }
        }
        underTest((files + folders), 1L).test {
            val notStartedEvents = cancelAndConsumeRemainingEvents()
                .filterIsInstance<Event.Item<MultiTransferEvent>>()
                .map { it.value }
                .filterIsInstance<MultiTransferEvent.TransferNotStarted<*>>()
            assertThat(notStartedEvents.size).isEqualTo(folders.size)
        }
    }

    @Test
    fun `test that getMyChatsFilesFolderUseCase result is set as destination`() = runTest {
        val chatFilesFolderId = NodeId(2L)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(chatFilesFolderId)
        underTest(listOf(mock()), 1L).test {
            verify(uploadFilesUseCase).invoke(
                any(),
                NodeId(eq(chatFilesFolderId.longValue)),
                any(),
                any(),
                any()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that chat upload app data is set`() = runTest {
        val pendingMessageId = 1L
        underTest(listOf(mock()), pendingMessageId).test {
            verify(uploadFilesUseCase).invoke(
                any(),
                NodeId(any()),
                eq(TransferAppData.ChatUpload(pendingMessageId)),
                any(),
                any()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that download worker is started when start download finish correctly`() = runTest {
        mockFlow(
            flowOf(
                MultiTransferEvent.ScanningFoldersFinished,
            )
        )
        underTest(listOf(mock()), 1L).collect()
        verify(startChatUploadsWorkerUseCase).invoke()
    }

    @Test
    fun `test that flow is not finished until the worker is started`() = runTest {
        var workerStarted = false
        mockFlow(
            flow {
                emit(MultiTransferEvent.ScanningFoldersFinished)
                awaitCancellation()
            }
        )
        whenever(isChatUploadsWorkerStartedUseCase()).then(
            AdditionalAnswers.answersWithDelay(
                10
            ) {
                workerStarted = true
            })
        underTest(listOf(mockFile()), 1L).test {
            val a = awaitItem()
            println(a)
            awaitComplete()
            assertThat(workerStarted).isTrue()
        }
        verify(isChatUploadsWorkerStartedUseCase).invoke()
    }

    private fun mockFile() = mock<File> {
        on { isFile }.thenReturn(true)
        on { isDirectory }.thenReturn(false)
    }

    private fun mockFolder() = mock<File> {
        on { isFile }.thenReturn(false)
        on { isDirectory }.thenReturn(true)
    }

    private fun mockFlow(flow: Flow<MultiTransferEvent>) {
        whenever(uploadFilesUseCase(any(), NodeId(any()), anyOrNull(), any(), any()))
            .thenReturn(flow)
    }
}