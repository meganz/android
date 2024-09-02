package mega.privacy.android.domain.usecase.transfers.uploads

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileForUploadUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartUploadsWithWorkerUseCaseTest {

    private lateinit var underTest: StartUploadsWithWorkerUseCase

    private val uploadFilesUseCase = mock<UploadFilesUseCase>()
    private val startUploadsWorkerAndWaitUntilIsStartedUseCase =
        mock<StartUploadsWorkerAndWaitUntilIsStartedUseCase>()
    private val monitorStorageStateEventUseCase = mock<MonitorStorageStateEventUseCase>()
    private val getFileForUploadUseCase = mock<GetFileForUploadUseCase>()
    private val cancelCancelTokenUseCase = mock<CancelCancelTokenUseCase>()

    private val path = "path"
    private val urisWithNames = mapOf(path to null)
    private val destinationId = NodeId(1L)
    private val file = mock<File> {
        on { path }.thenReturn(path)
    }

    @BeforeAll
    fun setup() {
        underTest = StartUploadsWithWorkerUseCase(
            uploadFilesUseCase,
            startUploadsWorkerAndWaitUntilIsStartedUseCase,
            monitorStorageStateEventUseCase,
            getFileForUploadUseCase,
            cancelCancelTokenUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            uploadFilesUseCase,
            startUploadsWorkerAndWaitUntilIsStartedUseCase,
            monitorStorageStateEventUseCase,
            getFileForUploadUseCase,
            cancelCancelTokenUseCase,
        )
        wheneverBlocking { monitorStorageStateEventUseCase() } doReturn MutableStateFlow(
            StorageStateEvent(1L, "", 0L, "", EventType.Unknown, StorageState.Unknown)
        )
    }

    @Test
    fun `test that TransferNotStarted event is emitted if storage state is PayWall`() = runTest {
        val storageStateFlow = MutableStateFlow(
            StorageStateEvent(
                1L,
                "",
                0L,
                "",
                EventType.Unknown,
                StorageState.Unknown
            )
        )
        whenever(monitorStorageStateEventUseCase()).thenReturn(storageStateFlow)
        storageStateFlow.emit(
            StorageStateEvent(
                1L,
                "",
                0L,
                "",
                EventType.Unknown,
                StorageState.PayWall
            )
        )
        underTest(urisWithNames, destinationId, false).test {
            val notStartedEvents = cancelAndConsumeRemainingEvents()
                .filterIsInstance<Event.Item<MultiTransferEvent>>()
                .map { it.value }
                .filterIsInstance<MultiTransferEvent.TransferNotStarted<*>>()
            assertThat(notStartedEvents.size).isEqualTo(1)
        }
    }

    @Test
    fun `test that the file is send to upload files use case`() = runTest {
        whenever(getFileForUploadUseCase(path, false)).thenReturn(file)
        underTest(urisWithNames, destinationId, false).test {
            verify(uploadFilesUseCase).invoke(
                eq(mapOf(file to null)), NodeId(eq(destinationId.longValue)), eq(null), any()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the is high priority is send to upload files use case`(
        expected: Boolean,
    ) = runTest {
        whenever(getFileForUploadUseCase(path, false)).thenReturn(file)
        underTest(urisWithNames, destinationId, expected).test {
            verify(uploadFilesUseCase).invoke(
                eq(mapOf(file to null)),
                NodeId(eq(destinationId.longValue)),
                eq(null),
                isHighPriority = eq(expected),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun `test that destinationId is used as destination`() = runTest {
        whenever(getFileForUploadUseCase(path, false)).thenReturn(file)
        underTest(urisWithNames, destinationId, false).test {
            verify(uploadFilesUseCase).invoke(
                eq(mapOf(file to null)),
                NodeId(eq(destinationId.longValue)),
                eq(null),
                any(),
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that worker is started when start upload finish correctly`() = runTest {
        whenever(getFileForUploadUseCase(path, false)).thenReturn(file)
        mockFlow(
            flow {
                emit(mock<MultiTransferEvent.SingleTransferEvent> {
                    on { scanningFinished } doReturn true
                })
                awaitCancellation()
            }
        )
        underTest(urisWithNames, destinationId, false).collect()
        verify(startUploadsWorkerAndWaitUntilIsStartedUseCase).invoke()
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
        whenever(startUploadsWorkerAndWaitUntilIsStartedUseCase()).then(
            AdditionalAnswers.answersWithDelay(
                10
            ) {
                workerStarted = true
            })
        underTest(urisWithNames, destinationId, false).test {
            awaitItem()
            awaitComplete()
            assertThat(workerStarted).isTrue()
        }
        verify(startUploadsWorkerAndWaitUntilIsStartedUseCase).invoke()
    }

    private fun mockFlow(flow: Flow<MultiTransferEvent>) {
        whenever(uploadFilesUseCase(any(), NodeId(any()), anyOrNull(), any()))
            .thenReturn(flow)
    }
}