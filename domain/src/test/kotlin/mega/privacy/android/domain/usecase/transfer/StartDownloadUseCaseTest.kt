package mega.privacy.android.domain.usecase.transfer

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.DownloadNodesEvent
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.downloads.DownloadNodesUseCase
import mega.privacy.android.domain.usecase.file.DoesPathHaveSufficientSpaceForNodesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.CancellationException

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartDownloadUseCaseTest {

    private lateinit var underTest: StartDownloadUseCase

    private val doesPathHaveSufficientSpaceForNodesUseCase: DoesPathHaveSufficientSpaceForNodesUseCase =
        mock()
    private val downloadNodesUseCase: DownloadNodesUseCase = mock()
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase = mock()
    private val fileSystemRepository: FileSystemRepository = mock()
    private val transferRepository: TransferRepository = mock()

    @BeforeAll
    fun setup() {
        underTest =
            StartDownloadUseCase(
                doesPathHaveSufficientSpaceForNodesUseCase = doesPathHaveSufficientSpaceForNodesUseCase,
                downloadNodesUseCase = downloadNodesUseCase,
                cancelCancelTokenUseCase = cancelCancelTokenUseCase,
                fileSystemRepository = fileSystemRepository,
                transferRepository = transferRepository,
            )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            doesPathHaveSufficientSpaceForNodesUseCase,
            downloadNodesUseCase,
            cancelCancelTokenUseCase,
            fileSystemRepository,
            transferRepository,
        )
    }

    @Test
    fun `test that file system create destination folder is launched`() = runTest {
        whenever(doesPathHaveSufficientSpaceForNodesUseCase(any(), any())).thenReturn(false)
        val nodes = nodeIds.map { mock<Node>() }
        underTest(nodes, DESTINATION_PATH_FOLDER, null, false).test {
            cancelAndIgnoreRemainingEvents()
        }
        verify(fileSystemRepository).createDirectory(DESTINATION_PATH_FOLDER)
    }

    @Test
    fun `test that not sufficient space event is emitted when there is no sufficient space`() =
        runTest {
            whenever(doesPathHaveSufficientSpaceForNodesUseCase(any(), any())).thenReturn(false)
            underTest(mockNodes(), DESTINATION_PATH_FOLDER, null, false).test {
                Truth.assertThat(awaitItem()).isEqualTo(DownloadNodesEvent.NotSufficientSpace)
                awaitComplete()
            }
        }

    @Test
    fun `test that single events are filtered out`() = runTest {
        mockFlow(
            flowOf(
                mock<DownloadNodesEvent.SingleTransferEvent>(),
                DownloadNodesEvent.FinishProcessingTransfers,
            )
        )
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, null, false).test {
            Truth.assertThat(awaitItem()).isEqualTo(DownloadNodesEvent.FinishProcessingTransfers)
            awaitComplete()
        }
    }

    @Test
    fun `test that flow completes when finish processing transfers is emitted`() = runTest {
        mockFlow(
            flowOf(
                DownloadNodesEvent.FinishProcessingTransfers,
                mock<DownloadNodesEvent.SingleTransferEvent>(),
            )
        )
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, null, false).test {
            Truth.assertThat(awaitItem()).isEqualTo(DownloadNodesEvent.FinishProcessingTransfers)
            awaitComplete()
        }
    }

    @Test
    fun `test that download worker is started when start download finish correctly`() = runTest {
        mockFlow(
            flowOf(
                DownloadNodesEvent.FinishProcessingTransfers,
            )
        )
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, null, false).collect()
        verify(transferRepository).startDownloadWorker()
    }

    @Test
    fun `test that cancel token is canceled when flow is canceled`() = runTest {
        mockFlow(
            flow {
                delay(1000)
                emit(DownloadNodesEvent.FinishProcessingTransfers)
            }
        )
        val job =
            this.launch {
                underTest(mockNodes(), DESTINATION_PATH_FOLDER, null, false).collect()
            }
        yield() // to be sure that the job is started.
        job.cancel(CancellationException())
        job.join()
        verify(cancelCancelTokenUseCase).invoke()
    }

    @Test
    fun `test that an exception is thrown when download node throws an exception`() = runTest {
        mockFlow(
            flow {
                throw (RuntimeException())
            }
        )
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, null, false).test {
            awaitError()
        }
    }

    @Test
    fun `test that cancel token is canceled when flow completes with an exception`() = runTest {
        mockFlow(
            flow {
                throw (RuntimeException())
            }
        )
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, null, false).test {
            cancelAndIgnoreRemainingEvents()
        }
        verify(cancelCancelTokenUseCase).invoke()
    }

    private fun mockNodes() = nodeIds.map { mock<Node>() }

    private suspend fun mockFlow(events: Flow<DownloadNodesEvent>) {
        whenever(doesPathHaveSufficientSpaceForNodesUseCase(any(), any())).thenReturn(true)
        whenever(
            downloadNodesUseCase(any(), any(), anyOrNull(), any())
        ).thenAnswer { events }
    }


    companion object {
        private val nodeIds = (0L..10L).map { NodeId(it) }
        private const val DESTINATION_PATH_FOLDER = "root/parent/destination"
    }
}