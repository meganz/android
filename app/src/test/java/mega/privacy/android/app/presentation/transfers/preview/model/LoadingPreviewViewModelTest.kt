package mega.privacy.android.app.presentation.transfers.preview.model

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.transfers.preview.view.LoadingPreviewInfo
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.transfers.NoTransferToShowException
import mega.privacy.android.domain.exception.transfers.TransferNotFoundException
import mega.privacy.android.domain.usecase.transfers.GetTransferByUniqueIdUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.previews.BroadcastTransferTagToCancelUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LoadingPreviewViewModelTest {

    private lateinit var underTest: LoadingPreviewViewModel

    private val getTransferByUniqueIdUseCase = mock<GetTransferByUniqueIdUseCase>()
    private val monitorTransferEventsUseCase = mock<MonitorTransferEventsUseCase> {
        onBlocking { invoke() } doReturn emptyFlow()
    }
    private val broadcastTransferTagToCancelUseCase = mock<BroadcastTransferTagToCancelUseCase>()
    private val fileTypeIconMapper = mock<FileTypeIconMapper>()
    private val appScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    private val transferUniqueId = 12L
    private val fileName = "test.txt"
    private var savedStateHandle = SavedStateHandle()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        reset(
            getTransferByUniqueIdUseCase,
            monitorTransferEventsUseCase,
            broadcastTransferTagToCancelUseCase,
            fileTypeIconMapper,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initTest() {
        underTest = LoadingPreviewViewModel(
            getTransferByUniqueIdUseCase = getTransferByUniqueIdUseCase,
            monitorTransferEventsUseCase = monitorTransferEventsUseCase,
            broadcastTransferTagToCancelUseCase = broadcastTransferTagToCancelUseCase,
            fileTypeIconMapper = fileTypeIconMapper,
            savedStateHandle = savedStateHandle,
            appScope = appScope,
        )
    }

    private fun initSavedStateHandle(
        transferUniqueId: Long? = null,
        transferPath: String? = null,
    ) {
        savedStateHandle = SavedStateHandle.Companion.invoke(
            route = LoadingPreviewInfo(
                transferUniqueId = transferUniqueId,
                transferPath = transferPath,
            )
        )
    }

    private suspend fun commonStub(
        transfer: Transfer?,
        flow: Flow<TransferEvent> = emptyFlow(),
    ) {
        whenever(getTransferByUniqueIdUseCase(transferUniqueId)) doReturn transfer
        whenever(monitorTransferEventsUseCase()).thenReturn(flow)
    }

    @Test
    fun `test initial state`() = runTest {
        initSavedStateHandle()

        initTest()

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.fileName).isNull()
            assertThat(actual.fileTypeResId).isNull()
            assertThat(actual.progress).isEqualTo(Progress(0f))
            assertThat(actual.previewFilePathToOpen).isNull()
            assertThat(actual.error).isInstanceOf(NoTransferToShowException::class.java)
            assertThat(actual.transferEvent).isInstanceOf(StateEventWithContentConsumed::class.java)
        }

        verifyNoInteractions(getTransferByUniqueIdUseCase)
        verifyNoInteractions(fileTypeIconMapper)
        verifyNoInteractions(monitorTransferEventsUseCase)
        verifyNoInteractions(broadcastTransferTagToCancelUseCase)
    }

    @Test
    fun `test that when transfer is not found but file is, state is updated with path`() =
        runTest {
            val file = File.createTempFile("test", fileName)
            file.createNewFile()
            val progress = Progress(1f)

            initSavedStateHandle(
                transferUniqueId = transferUniqueId,
                transferPath = file.absolutePath,
            )
            commonStub(transfer = null)

            initTest()
            advanceUntilIdle()

            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.progress).isEqualTo(progress)
                assertThat(actual.previewFilePathToOpen).isEqualTo(file.absolutePath)
            }
        }

    @Test
    fun `test that when transfer is found, then file name and file typeRes are set but not the error`() =
        runTest {
            val extension = "txt"
            val fileName = "test.$extension"
            val fileTypeResId = 1234
            val transfer = mock<Transfer> {
                on { this.fileName } doReturn fileName
            }

            initSavedStateHandle(transferUniqueId = transferUniqueId)
            commonStub(transfer = transfer)
            whenever(fileTypeIconMapper(extension)) doReturn fileTypeResId

            initTest()

            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.fileName).isEqualTo(fileName)
                assertThat(actual.fileTypeResId).isEqualTo(fileTypeResId)
                assertThat(actual.error).isNull()
            }
        }

    @Test
    fun `test that when transfer nor file are found, then file name and file typeRes are not set but error is`() =
        runTest {
            initSavedStateHandle(transferUniqueId = transferUniqueId)
            commonStub(transfer = null)

            initTest()

            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.fileName).isNull()
                assertThat(actual.fileTypeResId).isNull()
                assertThat(actual.error).isInstanceOf(TransferNotFoundException::class.java)
            }

            verifyNoInteractions(fileTypeIconMapper)
        }

    @Test
    fun `test that update event updates state with correct progress and does not update preview file path`() =
        runTest {
            val progress = Progress(0.5f)
            val transfer = mock<Transfer> {
                on { this.uniqueId } doReturn transferUniqueId
                on { this.fileName } doReturn fileName
                on { this.progress } doReturn progress
            }
            val event = mock<TransferEvent.TransferUpdateEvent> {
                on { this.transfer } doReturn transfer
            }

            initSavedStateHandle(transferUniqueId = transferUniqueId)
            commonStub(transfer = transfer, flow = flowOf(event))

            initTest()

            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.progress).isEqualTo(progress)
                assertThat(actual.previewFilePathToOpen).isNull()
            }
        }

    @Test
    fun `test that finish event updates state with preview file path and correct progress`() =
        runTest {
            val localPath = "localPath"
            val progress = Progress(1f)
            val transfer = mock<Transfer> {
                on { this.uniqueId } doReturn transferUniqueId
                on { this.fileName } doReturn fileName
                on { this.localPath } doReturn localPath
            }
            val event = mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer } doReturn transfer
            }

            initSavedStateHandle(transferUniqueId = transferUniqueId)
            commonStub(transfer = transfer, flow = flowOf(event))

            initTest()

            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.progress).isEqualTo(progress)
                assertThat(actual.previewFilePathToOpen).isEqualTo(localPath)
            }
        }

    @Test
    fun `test that temporary error event, with quota exceeded exception, updates state with error`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.uniqueId } doReturn transferUniqueId
                on { this.fileName } doReturn fileName
            }
            val error = mock<QuotaExceededMegaException>()
            val event = mock<TransferEvent.TransferTemporaryErrorEvent> {
                on { this.transfer } doReturn transfer
                on { this.error } doReturn error
            }

            initSavedStateHandle(transferUniqueId = transferUniqueId)
            commonStub(transfer = transfer, flow = flowOf(event))

            initTest()

            underTest.uiState.test {
                assertThat(awaitItem().error).isEqualTo(error)
            }
        }

    @Test
    fun `test that temporary error event, with NO quota exceeded exception, does not update state with error`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.uniqueId } doReturn transferUniqueId
                on { this.fileName } doReturn fileName
            }
            val error = mock<MegaException>()
            val event = mock<TransferEvent.TransferTemporaryErrorEvent> {
                on { this.transfer } doReturn transfer
                on { this.error } doReturn error
            }

            initSavedStateHandle(transferUniqueId = transferUniqueId)
            commonStub(transfer = transfer, flow = flowOf(event))

            initTest()

            underTest.uiState.test {
                assertThat(awaitItem().error).isNull()
            }
        }

    @Test
    fun `test that finish event, with any exception, updates state with event error`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.uniqueId } doReturn transferUniqueId
                on { this.fileName } doReturn fileName
            }
            val error = mock<MegaException>()
            val event = mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer } doReturn transfer
                on { this.error } doReturn error
            }

            initSavedStateHandle(transferUniqueId = transferUniqueId)
            commonStub(transfer = transfer, flow = flowOf(event))

            initTest()

            underTest.uiState.test {
                assertThat(awaitItem().error).isEqualTo(error)
            }
        }

    @Test
    fun `test that finish event, with cancelled transfer, updates state with NoTransferToShowException error`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.uniqueId } doReturn transferUniqueId
                on { this.fileName } doReturn fileName
                on { this.state } doReturn TransferState.STATE_CANCELLED
                on { this.isCancelled } doReturn true
            }
            val error = mock<MegaException>()
            val event = mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer } doReturn transfer
                on { this.error } doReturn error
            }

            initSavedStateHandle(transferUniqueId = transferUniqueId)
            commonStub(transfer = transfer, flow = flowOf(event))

            initTest()

            underTest.uiState.test {
                assertThat(awaitItem().error).isInstanceOf(NoTransferToShowException::class.java)
            }
        }

    @Test
    fun `test that consumeTransferEvent updates state with null`() =
        runTest {
            initTest()

            underTest.consumeTransferEvent()

            underTest.uiState.test {
                assertThat(awaitItem().transferEvent)
                    .isInstanceOf(StateEventWithContentConsumed::class.java)
            }
        }

    @Test
    fun `test that BroadcastTransferTagToCancelUseCase is invoked if transfer tag to cancel is received and state is updated with error if not transfer unique id`() =
        runTest {
            val transferTagToCancel = 1234

            whenever(broadcastTransferTagToCancelUseCase(transferTagToCancel)) doReturn Unit

            initTest()

            underTest.onNewIntent(transferTagToCancel = transferTagToCancel)

            underTest.uiState.test {
                assertThat(awaitItem().error)
                    .isInstanceOf(NoTransferToShowException::class.java)
            }

            verify(broadcastTransferTagToCancelUseCase).invoke(transferTagToCancel)
        }

    @Test
    fun `test that BroadcastTransferTagToCancelUseCase is invoked if transfer tag to cancel is received but state is NOT updated with error when there is transfer unique id`() =
        runTest {
            val transferTagToCancel = 1234
            val transfer = mock<Transfer> {
                on { this.fileName } doReturn fileName
            }

            initSavedStateHandle(transferUniqueId = transferUniqueId)
            commonStub(transfer = transfer)

            whenever(broadcastTransferTagToCancelUseCase(transferTagToCancel)) doReturn Unit

            initTest()

            underTest.onNewIntent(transferTagToCancel = transferTagToCancel)

            underTest.uiState.test {
                assertThat(awaitItem().error).isNull()
            }

            verify(broadcastTransferTagToCancelUseCase).invoke(transferTagToCancel)
        }
}