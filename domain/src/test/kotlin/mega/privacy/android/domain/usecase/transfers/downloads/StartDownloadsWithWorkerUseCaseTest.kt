package mega.privacy.android.domain.usecase.transfers.downloads

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
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.file.DoesPathHaveSufficientSpaceForNodesUseCase
import mega.privacy.android.domain.usecase.file.GetExternalPathByContentUriUseCase
import mega.privacy.android.domain.usecase.file.IsExternalStorageContentUriUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.util.concurrent.CancellationException

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartDownloadsWithWorkerUseCaseTest {

    private lateinit var underTest: StartDownloadsWithWorkerUseCase

    private val doesPathHaveSufficientSpaceForNodesUseCase: DoesPathHaveSufficientSpaceForNodesUseCase =
        mock()
    private val downloadNodesUseCase: DownloadNodesUseCase = mock()
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase = mock()
    private val fileSystemRepository: FileSystemRepository = mock()
    private val startDownloadsWorkerAndWaitUntilIsStartedUseCase: StartDownloadsWorkerAndWaitUntilIsStartedUseCase =
        mock()
    private val transferRepository = mock<TransferRepository>()
    private val getExternalPathByContentUriUseCase = mock<GetExternalPathByContentUriUseCase>()
    private val isExternalStorageContentUriUseCase = mock<IsExternalStorageContentUriUseCase>()

    @BeforeAll
    fun setup() {
        underTest =
            StartDownloadsWithWorkerUseCase(
                doesPathHaveSufficientSpaceForNodesUseCase = doesPathHaveSufficientSpaceForNodesUseCase,
                downloadNodesUseCase = downloadNodesUseCase,
                fileSystemRepository = fileSystemRepository,
                startDownloadsWorkerAndWaitUntilIsStartedUseCase = startDownloadsWorkerAndWaitUntilIsStartedUseCase,
                cancelCancelTokenUseCase = cancelCancelTokenUseCase,
                transferRepository = transferRepository,
                getExternalPathByContentUriUseCase = getExternalPathByContentUriUseCase,
                isExternalStorageContentUriUseCase = isExternalStorageContentUriUseCase,
            )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            doesPathHaveSufficientSpaceForNodesUseCase,
            downloadNodesUseCase,
            cancelCancelTokenUseCase,
            fileSystemRepository,
            startDownloadsWorkerAndWaitUntilIsStartedUseCase,
            transferRepository,
            getExternalPathByContentUriUseCase,
        )
        commonStub()
    }

    private suspend fun commonStub() {
        whenever(fileSystemRepository.isSDCardPath(any())).thenReturn(false)
        whenever(fileSystemRepository.isContentUri(any())).thenReturn(false)
        whenever(doesPathHaveSufficientSpaceForNodesUseCase(any(), any())).thenReturn(true)
        whenever(isExternalStorageContentUriUseCase(any())).thenReturn(false)
        whenever(getExternalPathByContentUriUseCase(DESTINATION_PATH_FOLDER)).thenReturn(
            DESTINATION_PATH_FOLDER
        )
    }

    @Test
    fun `test that file system create destination folder is launched`() = runTest {
        val nodes = nodeIds.map { mock<TypedFileNode>() }
        underTest(nodes, DESTINATION_PATH_FOLDER, false).test {
            cancelAndIgnoreRemainingEvents()
        }
        verify(fileSystemRepository).createDirectory(DESTINATION_PATH_FOLDER + File.separator)
    }

    @Test
    fun `test that not sufficient space event is emitted when there is no sufficient space`() =
        runTest {
            whenever(doesPathHaveSufficientSpaceForNodesUseCase(any(), any())).thenReturn(false)
            underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
                assertThat(awaitItem()).isEqualTo(MultiTransferEvent.InsufficientSpace)
                awaitComplete()
            }
        }

    @Test
    fun `test that single events are filtered out`() = runTest {
        val mockFinish = mock<MultiTransferEvent.SingleTransferEvent> {
            on { scanningFinished } doReturn true
        }
        mockFlow(
            flowOf(
                mock<MultiTransferEvent.SingleTransferEvent>(),
                mockFinish,
            )
        )
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
            assertThat(awaitItem()).isEqualTo(mockFinish)
            awaitComplete()
        }
    }

    @Test
    fun `test that flow completes after a delay when finish processing transfers is emitted`() =
        runTest {
            val mockFinish = mock<MultiTransferEvent.SingleTransferEvent> {
                on { scanningFinished } doReturn true
            }
            mockFlow(
                flow {
                    emit(mockFinish)
                    emit(mockFinish)
                    delay(5000)
                    emit(mockFinish) //should not be received
                    awaitCancellation()
                }
            )
            underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
                assertThat(awaitItem()).isEqualTo(mockFinish)
                assertThat(awaitItem()).isEqualTo(mockFinish)
                awaitComplete()
            }
        }

    @Test
    fun `test that flow completes immediately when all transfers updated transfers is emitted`() =
        runTest {
            val mockFinish = mock<MultiTransferEvent.SingleTransferEvent> {
                on { scanningFinished } doReturn true
            }
            val mockUpdated = mock<MultiTransferEvent.SingleTransferEvent> {
                on { scanningFinished } doReturn true
                on { allTransfersUpdated } doReturn true
            }
            mockFlow(
                flow {
                    emit(mockFinish)
                    yield() //to wait for the worker to start
                    emit(mockUpdated)
                    emit(mockUpdated) //should not be received
                    awaitCancellation()
                }
            )
            underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
                assertThat(awaitItem()).isEqualTo(mockFinish)
                assertThat(awaitItem()).isEqualTo(mockUpdated)
                awaitComplete()
            }
        }

    @Test
    fun `test that download worker is started when start download finish correctly`() = runTest {
        mockFlow(
            flow {
                emit(mock<MultiTransferEvent.SingleTransferEvent> {
                    on { scanningFinished } doReturn true
                })
                awaitCancellation()
            }
        )
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).collect()
        verify(startDownloadsWorkerAndWaitUntilIsStartedUseCase).invoke()
    }

    @Test
    fun `test that flow is not finished until the worker is started`() = runTest {
        var workerStarted = false
        mockFlow(
            flow {
                emit(mock<MultiTransferEvent.SingleTransferEvent> {
                    on { scanningFinished } doReturn true
                })
                awaitCancellation()
            }
        )
        whenever(startDownloadsWorkerAndWaitUntilIsStartedUseCase()).then(
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
        verify(startDownloadsWorkerAndWaitUntilIsStartedUseCase).invoke()
    }

    @Test
    fun `test that cancel token is canceled when flow is canceled`() = runTest {
        mockFlow(
            flow {
                delay(1000)
                emit(mock<MultiTransferEvent.SingleTransferEvent> {
                    on { scanningFinished } doReturn true
                })
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
        class TestError : RuntimeException("test")

        val expectedError = TestError()
        mockFlow(
            flow {
                throw (expectedError)
            }
        )
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
            assertThat(awaitError()).isInstanceOf(TestError::class.java)
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
        ).thenAnswer { emptyFlow<MultiTransferEvent>() }
        whenever(fileSystemRepository.isSDCardPath(DESTINATION_PATH_FOLDER)).thenReturn(true)
        whenever(transferRepository.getOrCreateSDCardTransfersCacheFolder())
            .thenReturn(File(cachePath))
        underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
            awaitComplete()
        }
        verify(downloadNodesUseCase)
            .invoke(any(), eq(cachePath.plus(File.separator)), anyOrNull(), any())
    }

    @Test
    fun `test that destination is set by getExternalPathByContentUriUseCase when download points to external storage path`() =
        runTest {
            val cachePath = "cachePath"
            whenever(
                downloadNodesUseCase(any(), any(), anyOrNull(), any())
            ).thenAnswer { emptyFlow<MultiTransferEvent>() }
            whenever(isExternalStorageContentUriUseCase(DESTINATION_PATH_FOLDER)).thenReturn(true)
            whenever(getExternalPathByContentUriUseCase(DESTINATION_PATH_FOLDER))
                .thenReturn(cachePath)
            underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
                awaitComplete()
            }
            verify(downloadNodesUseCase)
                .invoke(any(), eq(cachePath + File.separator), anyOrNull(), any())
        }

    @Test
    fun `test that app data is set with correct target path and uri when download points to sd`() =
        runTest {
            val cachePath = "cachePath/"
            val expectedAppData =
                TransferAppData.SdCardDownload(DESTINATION_PATH_FOLDER, DESTINATION_PATH_FOLDER)
            whenever(
                downloadNodesUseCase(any(), any(), anyOrNull(), any())
            ).thenAnswer { emptyFlow<MultiTransferEvent>() }
            whenever(fileSystemRepository.isSDCardPath(DESTINATION_PATH_FOLDER)).thenReturn(true)
            whenever(transferRepository.getOrCreateSDCardTransfersCacheFolder())
                .thenReturn(File(cachePath))
            underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
                awaitComplete()
            }
            verify(downloadNodesUseCase)
                .invoke(any(), anyOrNull(), eq(expectedAppData), any())
        }

    @Test
    fun `test that folder transfer update events are not filtered out`() =
        runTest {
            val expected = MultiTransferEvent.SingleTransferEvent(
                transferEvent = TransferEvent.FolderTransferUpdateEvent(
                    mock(),
                    TransferStage.STAGE_SCANNING,
                    0, 0, 0, null, null,
                ), 0, 0
            )
            whenever(
                downloadNodesUseCase(any(), any(), anyOrNull(), any())
            ).thenAnswer {
                flowOf(expected)
            }
            underTest(mockNodes(), DESTINATION_PATH_FOLDER, false).test {
                assertThat(awaitItem()).isEqualTo(expected)
                awaitComplete()
            }
        }

    private fun mockNodes() = nodeIds.map { mock<TypedFileNode>() }

    private suspend fun mockFlow(events: Flow<MultiTransferEvent>) {
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