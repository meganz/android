package mega.privacy.android.domain.usecase.transfers

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.DownloadNodesEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.file.DoesPathHaveSufficientSpaceForNodesUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodesUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.IsDownloadsWorkerStartedUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.StartDownloadUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.StartDownloadWorkerUseCase
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
    private val startDownloadWorkerUseCase: StartDownloadWorkerUseCase = mock()
    private val isDownloadsWorkerStartedUseCase: IsDownloadsWorkerStartedUseCase =
        mock()

    @BeforeAll
    fun setup() {
        underTest =
            StartDownloadUseCase(
                doesPathHaveSufficientSpaceForNodesUseCase = doesPathHaveSufficientSpaceForNodesUseCase,
                downloadNodesUseCase = downloadNodesUseCase,
                cancelCancelTokenUseCase = cancelCancelTokenUseCase,
                fileSystemRepository = fileSystemRepository,
                startDownloadWorkerUseCase = startDownloadWorkerUseCase,
                isDownloadsWorkerStartedUseCase = isDownloadsWorkerStartedUseCase,
            )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            doesPathHaveSufficientSpaceForNodesUseCase,
            downloadNodesUseCase,
            cancelCancelTokenUseCase,
            fileSystemRepository,
            startDownloadWorkerUseCase,
            isDownloadsWorkerStartedUseCase,
        )
        whenever(fileSystemRepository.isSDCardPath(any())).thenReturn(false)
    }

    @Test
    fun `test that file system create destination folder is launched`() = runTest {
        whenever(doesPathHaveSufficientSpaceForNodesUseCase(any(), any())).thenReturn(false)
        val nodes = nodeIds.map { mock<TypedFileNode>() }
        underTest(nodes, DESTINATION_PATH_FOLDER, false).test {
            cancelAndIgnoreRemainingEvents()
        }
        verify(fileSystemRepository).createDirectory(DESTINATION_PATH_FOLDER)
    }

    @Test
    fun `test that not sufficient space event is emitted when there is no sufficient space`() =
        runTest {
            whenever(doesPathHaveSufficientSpaceForNodesUseCase(any(), any())).thenReturn(false)
            underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
                assertThat(awaitItem()).isEqualTo(DownloadNodesEvent.NotSufficientSpace)
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
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
            assertThat(awaitItem()).isEqualTo(DownloadNodesEvent.FinishProcessingTransfers)
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
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
            assertThat(awaitItem()).isEqualTo(DownloadNodesEvent.FinishProcessingTransfers)
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
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).collect()
        verify(startDownloadWorkerUseCase).invoke()
    }

    @Test
    fun `test that flow is not finished until the worker is started`() = runTest {
        var workerStarted = false
        mockFlow(
            flow {
                emit(DownloadNodesEvent.FinishProcessingTransfers)
                awaitCancellation()
            }
        )
        whenever(isDownloadsWorkerStartedUseCase()).then(
            AdditionalAnswers.answersWithDelay(
                10
            ) {
                workerStarted = true
            })
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
            awaitItem()
            awaitComplete()
            assertThat(workerStarted).isTrue()
        }
        verify(isDownloadsWorkerStartedUseCase).invoke()
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
                underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).collect()
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
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
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
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
            cancelAndIgnoreRemainingEvents()
        }
        verify(cancelCancelTokenUseCase).invoke()
    }

    @Test
    fun `test that destination is set to cache path when download points to sd`() = runTest {
        val cachePath = "cachePath"
        whenever(
            downloadNodesUseCase(any(), any(), anyOrNull(), any())
        ).thenAnswer { emptyFlow<DownloadNodesEvent>() }
        whenever(doesPathHaveSufficientSpaceForNodesUseCase(any(), any())).thenReturn(true)
        whenever(fileSystemRepository.isSDCardPath(DESTINATION_PATH_FOLDER)).thenReturn(true)
        whenever(fileSystemRepository.getOrCreateSDCardCacheFolder()).thenReturn(File(cachePath))
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
            awaitComplete()
        }
        verify(downloadNodesUseCase)
            .invoke(any(), eq(cachePath.plus(File.separator)), anyOrNull(), any())
    }

    @Test
    fun `test that app data is set with original path when download points to sd`() = runTest {
        val cachePath = "cachePath"
        val expectedAppData = TransferAppData.SdCardDownload(DESTINATION_PATH_FOLDER, null)
        whenever(
            downloadNodesUseCase(any(), any(), anyOrNull(), any())
        ).thenAnswer { emptyFlow<DownloadNodesEvent>() }
        whenever(doesPathHaveSufficientSpaceForNodesUseCase(any(), any())).thenReturn(true)
        whenever(fileSystemRepository.isSDCardPath(DESTINATION_PATH_FOLDER)).thenReturn(true)
        whenever(fileSystemRepository.getOrCreateSDCardCacheFolder()).thenReturn(File(cachePath))
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
            awaitComplete()
        }
        verify(downloadNodesUseCase)
            .invoke(any(), anyOrNull(), eq(expectedAppData), any())
    }

    private fun mockNodes() = nodeIds.map { mock<TypedFileNode>() }

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