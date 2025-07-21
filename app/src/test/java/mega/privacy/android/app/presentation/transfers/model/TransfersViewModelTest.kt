package mega.privacy.android.app.presentation.transfers.model

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.extensions.asHotFlow
import mega.privacy.android.app.extensions.moveElement
import mega.privacy.android.app.presentation.transfers.EXTRA_TAB
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.chat.ChatUploadNotRetriedException
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.message.pendingmessages.RetryChatUploadUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferBeforeByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferToFirstByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferToLastByTagUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorInProgressTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransfersByIdUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteFailedOrCancelledTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.completed.MonitorCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransfersQueueUseCase
import okhttp3.internal.immutableListOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransfersViewModelTest {

    private lateinit var underTest: TransfersViewModel

    private val monitorInProgressTransfersUseCase = mock<MonitorInProgressTransfersUseCase>()
    private val monitorStorageStateEventUseCase = mock<MonitorStorageStateEventUseCase>()
    private val monitorTransferOverQuotaUseCase = mock<MonitorTransferOverQuotaUseCase>()
    private val monitorPausedTransfersUseCase = mock<MonitorPausedTransfersUseCase>()
    private val pauseTransferByTagUseCase = mock<PauseTransferByTagUseCase>()
    private val pauseTransfersQueueUseCase = mock<PauseTransfersQueueUseCase>()
    private val cancelTransfersUseCase = mock<CancelTransfersUseCase>()
    private val monitorCompletedTransfersUseCase = mock<MonitorCompletedTransfersUseCase>()
    private val moveTransferBeforeByTagUseCase = mock<MoveTransferBeforeByTagUseCase>()
    private val moveTransferToFirstByTagUseCase = mock<MoveTransferToFirstByTagUseCase>()
    private val moveTransferToLastByTagUseCase = mock<MoveTransferToLastByTagUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val retryChatUploadUseCase = mock<RetryChatUploadUseCase>()
    private val deleteFailedOrCancelledTransfersUseCase =
        mock<DeleteFailedOrCancelledTransfersUseCase>()
    private val deleteCompletedTransfersUseCase = mock<DeleteCompletedTransfersUseCase>()
    private val deleteCompletedTransfersByIdUseCase = mock<DeleteCompletedTransfersByIdUseCase>()
    private val cancelTransferByTagUseCase = mock<CancelTransferByTagUseCase>()

    private val originalPath = "originalPath"
    private val chatAppData = listOf<TransferAppData>(TransferAppData.ChatUpload(1))
    private val originalUriPathAppData =
        listOf<TransferAppData>(TransferAppData.OriginalUriPath(UriPath(originalPath)))
    private val failedOfflineDownload = mock<CompletedTransfer> {
        on { id } doReturn 1
        on { state } doReturn TransferState.STATE_FAILED
        on { timestamp } doReturn 1L
        on { handle } doReturn 1L
        on { type } doReturn TransferType.DOWNLOAD
        on { isOffline } doReturn true
    }
    private val cancelledDownload = mock<CompletedTransfer> {
        on { id } doReturn 2
        on { state } doReturn TransferState.STATE_CANCELLED
        on { timestamp } doReturn 2L
        on { handle } doReturn 2L
        on { type } doReturn TransferType.DOWNLOAD
        on { path } doReturn "downloadLocation"
    }
    private val cancelledChatUpload = mock<CompletedTransfer> {
        on { id } doReturn 3
        on { state } doReturn TransferState.STATE_CANCELLED
        on { timestamp } doReturn 3L
        on { type } doReturn TransferType.GENERAL_UPLOAD
        on { this.appData } doReturn chatAppData
        on { this.originalPath } doReturn originalPath
    }
    private val failedUpload = mock<CompletedTransfer> {
        on { id } doReturn 4
        on { state } doReturn TransferState.STATE_FAILED
        on { timestamp } doReturn 4L
        on { parentHandle } doReturn 4L
        on { type } doReturn TransferType.GENERAL_UPLOAD
        on { this.appData } doReturn originalUriPathAppData
        on { this.originalPath } doReturn originalPath
    }
    private val typedNode = mock<TypedNode>()
    private val offlineStartEvent = TransferTriggerEvent.StartDownloadForOffline(
        node = typedNode,
        withStartMessage = false
    )
    private val downloadRetryEvent = TransferTriggerEvent.RetryDownloadNode(
        node = typedNode,
        downloadLocation = "downloadLocation",
    )
    private val uploadStartEvent = TransferTriggerEvent.StartUpload.Files(
        mapOf(originalPath to null),
        NodeId(failedUpload.parentHandle)
    )

    @BeforeEach
    fun resetMocks() {
        reset(
            pauseTransferByTagUseCase,
            pauseTransfersQueueUseCase,
            moveTransferBeforeByTagUseCase,
            moveTransferToFirstByTagUseCase,
            moveTransferToLastByTagUseCase,
            getNodeByIdUseCase,
            retryChatUploadUseCase,
            deleteFailedOrCancelledTransfersUseCase,
            deleteCompletedTransfersUseCase,
            deleteCompletedTransfersByIdUseCase,
            cancelTransferByTagUseCase,
            cancelTransfersUseCase,
            monitorCompletedTransfersUseCase,
        )
        wheneverBlocking { monitorInProgressTransfersUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorStorageStateEventUseCase() } doReturn MutableStateFlow(
            StorageStateEvent(1L, StorageState.Unknown)
        )
        wheneverBlocking { monitorTransferOverQuotaUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorPausedTransfersUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorCompletedTransfersUseCase() }.thenReturn(emptyFlow())
    }

    private fun initTestClass() {
        val savedStateHandle = SavedStateHandle(mapOf(EXTRA_TAB to 0))
        underTest = TransfersViewModel(
            ioDispatcher = UnconfinedTestDispatcher(),
            monitorInProgressTransfersUseCase = monitorInProgressTransfersUseCase,
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            monitorTransferOverQuotaUseCase = monitorTransferOverQuotaUseCase,
            monitorPausedTransfersUseCase = monitorPausedTransfersUseCase,
            pauseTransferByTagUseCase = pauseTransferByTagUseCase,
            pauseTransfersQueueUseCase = pauseTransfersQueueUseCase,
            cancelTransfersUseCase = cancelTransfersUseCase,
            monitorCompletedTransfersUseCase = monitorCompletedTransfersUseCase,
            moveTransferBeforeByTagUseCase = moveTransferBeforeByTagUseCase,
            moveTransferToFirstByTagUseCase = moveTransferToFirstByTagUseCase,
            moveTransferToLastByTagUseCase = moveTransferToLastByTagUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            retryChatUploadUseCase = retryChatUploadUseCase,
            deleteFailedOrCancelledTransfersUseCase = deleteFailedOrCancelledTransfersUseCase,
            deleteCompletedTransfersUseCase = deleteCompletedTransfersUseCase,
            deleteCompletedTransfersByIdUseCase = deleteCompletedTransfersByIdUseCase,
            cancelTransferByTagUseCase = cancelTransferByTagUseCase,
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun `test that MonitorInProgressTransfersUseCase updates state with active transfers`() =
        runTest {
            val flow = MutableSharedFlow<Map<Long, InProgressTransfer>>()
            val transfer1 = mock<InProgressTransfer.Upload> {
                on { priority } doReturn BigInteger.ONE
            }
            val transfer2 = mock<InProgressTransfer.Download> {
                on { priority } doReturn BigInteger.TWO
            }
            val map = mapOf(1L to transfer1, 2L to transfer2)

            whenever(monitorInProgressTransfersUseCase()).thenReturn(flow)

            initTestClass()

            underTest.uiState.map { it.activeTransfers }.test {
                assertThat(awaitItem()).isEmpty()
                flow.emit(map)
                advanceUntilIdle()
                assertThat(awaitItem()).containsExactly(transfer1, transfer2)
            }
        }

    @ParameterizedTest(name = " when isStorageOverQuota: {0}")
    @EnumSource(StorageState::class)
    fun `test that MonitorStorageStateEventUseCase updates state with storage state`(
        storageState: StorageState,
    ) = runTest {
        val flow = MutableStateFlow(
            StorageStateEvent(
                1L,
                storageState = StorageState.Unknown,
            )
        )

        whenever(monitorStorageStateEventUseCase()).thenReturn(flow)

        initTestClass()

        underTest.uiState.test {
            var actual = awaitItem()
            assertThat(actual.isStorageOverQuota).isFalse()
            assertThat(actual.quotaWarning).isNull()
            flow.emit(
                StorageStateEvent(
                    1L,
                    storageState = storageState,
                )
            )
            advanceUntilIdle()

            if (storageState == StorageState.Red || storageState == StorageState.PayWall) {
                actual = awaitItem()
                assertThat(actual.isStorageOverQuota).isTrue()
                assertThat(actual.quotaWarning).isEqualTo(QuotaWarning.Storage)
                flow.emit(
                    StorageStateEvent(
                        1L,
                        storageState = StorageState.Green,
                    )
                )

                actual = awaitItem()
                assertThat(actual.isStorageOverQuota).isFalse()
                assertThat(actual.quotaWarning).isNull()
            }
        }
    }

    @Test
    fun `test that MonitorTransferOverQuotaUseCase updates state with transfer over quota`() =
        runTest {
            val flow = MutableStateFlow(false)

            whenever(monitorTransferOverQuotaUseCase()).thenReturn(flow)

            initTestClass()

            underTest.uiState.test {
                var actual = awaitItem()
                assertThat(actual.isTransferOverQuota).isFalse()
                assertThat(actual.quotaWarning).isNull()
                flow.emit(true)
                actual = awaitItem()
                assertThat(actual.isTransferOverQuota).isTrue()
                assertThat(actual.quotaWarning).isEqualTo(QuotaWarning.Transfer)
                flow.emit(false)
                actual = awaitItem()
                assertThat(actual.isTransferOverQuota).isFalse()
                assertThat(actual.quotaWarning).isNull()
            }
        }

    @Test
    fun `test that MonitorTransferOverQuotaUseCase and MonitorStorageStateEventUseCase updates state with quotaWarning`() =
        runTest {
            val transferQuotaFlow = MutableStateFlow(false)
            val storageQuotaflow = MutableStateFlow(
                StorageStateEvent(
                    1L,
                    storageState = StorageState.Unknown,
                )
            )

            whenever(monitorTransferOverQuotaUseCase()).thenReturn(transferQuotaFlow)
            whenever(monitorStorageStateEventUseCase()).thenReturn(storageQuotaflow)

            initTestClass()

            underTest.uiState.map { it.quotaWarning }.test {
                assertThat(awaitItem()).isNull()
                transferQuotaFlow.emit(true)
                assertThat(awaitItem()).isEqualTo(QuotaWarning.Transfer)
                storageQuotaflow.emit(
                    StorageStateEvent(
                        1L,
                        storageState = StorageState.Red,
                    )
                )
                assertThat(awaitItem()).isEqualTo(QuotaWarning.StorageAndTransfer)
                transferQuotaFlow.emit(false)
                assertThat(awaitItem()).isEqualTo(QuotaWarning.Storage)
                underTest.onConsumeQuotaWarning()
                assertThat(awaitItem()).isNull()
            }
        }

    @Test
    fun `test that MonitorPausedTransfersUseCase updates state with areTransfersPaused`() =
        runTest {
            val flow = MutableStateFlow(false)

            whenever(monitorPausedTransfersUseCase()).thenReturn(flow)

            initTestClass()

            underTest.uiState.map { it.areTransfersPaused }.test {
                assertThat(awaitItem()).isFalse()
                flow.emit(true)
                assertThat(awaitItem()).isTrue()
                flow.emit(false)
                assertThat(awaitItem()).isFalse()
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that playOrPauseTransfer invokes correctly`(
        isPaused: Boolean,
    ) = runTest {
        val flow = MutableSharedFlow<Map<Long, InProgressTransfer>>()
        val tag = 1
        val transfer1 = mock<InProgressTransfer.Upload> {
            on { this.tag } doReturn tag
            on { priority } doReturn BigInteger.ONE
            on { this.isPaused } doReturn isPaused
        }
        val transfer2 = mock<InProgressTransfer.Download> {
            on { this.tag } doReturn 2
            on { priority } doReturn BigInteger.TWO
        }
        val map = mapOf(1L to transfer1, 2L to transfer2)

        whenever(monitorInProgressTransfersUseCase()).thenReturn(flow)

        initTestClass()
        flow.emit(map)
        advanceUntilIdle()
        underTest.playOrPauseTransfer(tag)

        verify(pauseTransferByTagUseCase).invoke(tag, !isPaused)
    }

    @Test
    fun `test that playOrPauseTransfer does not invoke anything if there is no transfer with the tag received`() =
        runTest {
            initTestClass()
            underTest.playOrPauseTransfer(1)

            verifyNoInteractions(pauseTransferByTagUseCase)
        }

    @Test
    fun `test that resumeTransfers invokes PauseTransfersQueueUseCase with pause = false and updates state correctly`() =
        runTest {
            whenever(pauseTransfersQueueUseCase(false)).thenReturn(false)

            initTestClass()
            underTest.resumeTransfersQueue()

            underTest.uiState.map { it.areTransfersPaused }.test {
                assertThat(awaitItem()).isFalse()
            }
            verify(pauseTransfersQueueUseCase).invoke(false)
        }

    @Test
    fun `test that pauseTransfers invokes PauseTransfersQueueUseCase with pause = true and updates state correctly`() =
        runTest {
            whenever(pauseTransfersQueueUseCase(true)).thenReturn(true)

            initTestClass()
            underTest.pauseTransfersQueue()

            underTest.uiState.map { it.areTransfersPaused }.test {
                assertThat(awaitItem()).isTrue()
            }
            verify(pauseTransfersQueueUseCase).invoke(true)
        }

    @Test
    fun `test that cancelAllTransfers invokes CancelTransfersUseCase`() = runTest {
        whenever(cancelTransfersUseCase()).thenReturn(Unit)

        initTestClass()
        underTest.cancelAllTransfers()

        verify(cancelTransfersUseCase).invoke()
    }

    @Test
    fun `test that MonitorCompletedTransfersUseCase updates state with completed and failed transfers`() =
        runTest {
            val flow = MutableSharedFlow<List<CompletedTransfer>>()
            val transfer1 = mock<CompletedTransfer> {
                on { state } doReturn TransferState.STATE_COMPLETED
                on { timestamp } doReturn 1L
            }
            val transfer2 = mock<CompletedTransfer> {
                on { state } doReturn TransferState.STATE_FAILED
                on { timestamp } doReturn 2L
            }
            val transfer3 = mock<CompletedTransfer> {
                on { state } doReturn TransferState.STATE_COMPLETED
                on { timestamp } doReturn 3L
            }
            val transfer4 = mock<CompletedTransfer> {
                on { state } doReturn TransferState.STATE_CANCELLED
                on { timestamp } doReturn 4L
            }
            val list = listOf(transfer1, transfer4, transfer3, transfer2)
            val expectedCompleted = immutableListOf(transfer3, transfer1)
            val expectedFailed = immutableListOf(transfer4, transfer2)

            whenever(monitorCompletedTransfersUseCase()).thenReturn(flow)

            initTestClass()

            underTest.uiState.test {
                var actual = awaitItem()
                assertThat(actual.completedTransfers).isEmpty()
                assertThat(actual.failedTransfers).isEmpty()
                flow.emit(list)
                advanceUntilIdle()
                actual = awaitItem()
                assertThat(actual.completedTransfers).isEqualTo(expectedCompleted)
                assertThat(actual.failedTransfers).isEqualTo(expectedFailed)
            }
        }

    @Test
    fun `test that MonitorCompletedTransfersUseCase updates state with completed but not failed transfers`() =
        runTest {
            val flow = MutableSharedFlow<List<CompletedTransfer>>()
            val transfer1 = mock<CompletedTransfer> {
                on { state } doReturn TransferState.STATE_COMPLETED
                on { timestamp } doReturn 1L
            }
            val transfer2 = mock<CompletedTransfer> {
                on { state } doReturn TransferState.STATE_COMPLETED
                on { timestamp } doReturn 2L
            }
            val list = listOf(transfer1, transfer2)
            val expectedCompleted = immutableListOf(transfer2, transfer1)

            whenever(monitorCompletedTransfersUseCase()).thenReturn(flow)

            initTestClass()

            underTest.uiState.test {
                var actual = awaitItem()
                assertThat(actual.completedTransfers).isEmpty()
                assertThat(actual.failedTransfers).isEmpty()
                flow.emit(list)
                advanceUntilIdle()
                actual = awaitItem()
                assertThat(actual.completedTransfers).isEqualTo(expectedCompleted)
                assertThat(actual.failedTransfers).isEmpty()
            }
        }

    @Test
    fun `test that MonitorCompletedTransfersUseCase updates state with failed but not completed transfers`() =
        runTest {
            val flow = MutableSharedFlow<List<CompletedTransfer>>()
            val transfer1 = mock<CompletedTransfer> {
                on { state } doReturn TransferState.STATE_CANCELLED
                on { timestamp } doReturn 1L
            }
            val transfer2 = mock<CompletedTransfer> {
                on { state } doReturn TransferState.STATE_FAILED
                on { timestamp } doReturn 2L
            }
            val list = listOf(transfer1, transfer2)
            val expectedFailed = immutableListOf(transfer2, transfer1)

            whenever(monitorCompletedTransfersUseCase()).thenReturn(flow)

            initTestClass()

            underTest.uiState.test {
                var actual = awaitItem()
                assertThat(actual.completedTransfers).isEmpty()
                assertThat(actual.failedTransfers).isEmpty()
                flow.emit(list)
                advanceUntilIdle()
                actual = awaitItem()
                assertThat(actual.completedTransfers).isEmpty()
                assertThat(actual.failedTransfers).isEqualTo(expectedFailed)
            }
        }

    @Test
    fun `test that active transfers in ui state are reordered but not with use cases when onActiveTransfersReorderPreview is invoked`() =
        runTest {
            val initialActiveTransfers = (1..10).map { index ->
                mock<InProgressTransfer.Download> {
                    on { this.priority } doReturn index.toBigInteger()
                    on { this.tag } doReturn index
                }
            }
            val doubleChange = Triple(0, 1, 5)
            val expected = initialActiveTransfers.toMutableList()
                .moveElement(doubleChange.first, doubleChange.third)
            val map = initialActiveTransfers.associateBy { it.tag.toLong() }
            whenever(monitorInProgressTransfersUseCase()).thenReturn(
                map.asHotFlow()
            )
            initTestClass()

            underTest.uiState.test {
                awaitItem()
                underTest.onActiveTransfersReorderPreview(doubleChange.first, doubleChange.second)
                underTest.onActiveTransfersReorderPreview(doubleChange.second, doubleChange.third)
                val actual = awaitItem().activeTransfers
                assertThat(actual).containsExactlyElementsIn(expected)
                cancelAndIgnoreRemainingEvents()
            }
            verifyNoInteractions(
                moveTransferBeforeByTagUseCase,
                moveTransferToFirstByTagUseCase,
                moveTransferToLastByTagUseCase,
            )
        }

    @Test
    fun `test that moveTransferBeforeByTagUseCase is invoked when preview reorder is confirmed`() =
        runTest {
            val initialActiveTransfers = (1..10).map { index ->
                mock<InProgressTransfer.Download> {
                    on { this.priority } doReturn index.toBigInteger()
                    on { this.tag } doReturn index
                }
            }
            val doubleChange = Triple(0, 1, 5)
            val map = initialActiveTransfers.associateBy { it.tag.toLong() }
            whenever(monitorInProgressTransfersUseCase()).thenReturn(
                map.asHotFlow()
            )
            initTestClass()

            underTest.uiState.test {
                awaitItem()
                underTest.onActiveTransfersReorderPreview(doubleChange.first, doubleChange.second)
                underTest.onActiveTransfersReorderPreview(doubleChange.second, doubleChange.third)
                underTest.onActiveTransfersReorderConfirmed(initialActiveTransfers[doubleChange.first])
                cancelAndIgnoreRemainingEvents()
            }
            val expectedTag = initialActiveTransfers[doubleChange.first].tag
            val expectedDest = initialActiveTransfers[doubleChange.third + 1].tag
            verify(moveTransferBeforeByTagUseCase).invoke(
                expectedTag,
                expectedDest
            )
            verifyNoInteractions(
                moveTransferToFirstByTagUseCase,
                moveTransferToLastByTagUseCase,
            )
        }

    @Test
    fun `test that moveTransferToFirstByTagUseCase is invoked when preview reorder to first is confirmed`() =
        runTest {
            val initialActiveTransfers = (1..10).map { index ->
                mock<InProgressTransfer.Download> {
                    on { this.priority } doReturn index.toBigInteger()
                    on { this.tag } doReturn index
                }
            }
            val doubleChange = Triple(2, 1, 0)
            val map = initialActiveTransfers.associateBy { it.tag.toLong() }
            whenever(monitorInProgressTransfersUseCase()).thenReturn(
                map.asHotFlow()
            )
            initTestClass()

            underTest.uiState.test {
                awaitItem()
                underTest.onActiveTransfersReorderPreview(doubleChange.first, doubleChange.second)
                underTest.onActiveTransfersReorderPreview(doubleChange.second, doubleChange.third)
                underTest.onActiveTransfersReorderConfirmed(initialActiveTransfers[doubleChange.first])
                cancelAndIgnoreRemainingEvents()
            }
            val expectedTag = initialActiveTransfers[doubleChange.first].tag
            verify(moveTransferToFirstByTagUseCase).invoke(expectedTag)
            verifyNoInteractions(
                moveTransferBeforeByTagUseCase,
                moveTransferToLastByTagUseCase,
            )
        }

    @Test
    fun `test that moveTransferToLastByTagUseCase is invoked when preview reorder to last is confirmed`() =
        runTest {
            val initialActiveTransfers = (1..10).map { index ->
                mock<InProgressTransfer.Download> {
                    on { this.priority } doReturn index.toBigInteger()
                    on { this.tag } doReturn index
                }
            }
            val doubleChange = Triple(
                initialActiveTransfers.lastIndex - 2,
                initialActiveTransfers.lastIndex - 1,
                initialActiveTransfers.lastIndex
            )
            val map = initialActiveTransfers.associateBy { it.tag.toLong() }
            whenever(monitorInProgressTransfersUseCase()).thenReturn(
                map.asHotFlow()
            )
            initTestClass()

            underTest.uiState.test {
                awaitItem()
                underTest.onActiveTransfersReorderPreview(doubleChange.first, doubleChange.second)
                underTest.onActiveTransfersReorderPreview(doubleChange.second, doubleChange.third)
                underTest.onActiveTransfersReorderConfirmed(initialActiveTransfers[doubleChange.first])
                cancelAndIgnoreRemainingEvents()
            }
            val expectedTag = initialActiveTransfers[doubleChange.first].tag
            verify(moveTransferToLastByTagUseCase).invoke(expectedTag)
            verifyNoInteractions(
                moveTransferBeforeByTagUseCase,
                moveTransferToFirstByTagUseCase,
            )
        }

    @Test
    fun `test that retryFailedTransfer throws IllegalArgumentException if transfer is not upload or download`() =
        runTest {
            val unknownTransfer = mock<CompletedTransfer> {
                on { id } doReturn 4
                on { state } doReturn TransferState.STATE_FAILED
                on { timestamp } doReturn 4L
                on { parentHandle } doReturn 4L
                on { type } doReturn TransferType.NONE
                on { this.originalPath } doReturn originalPath
            }

            initTestClass()

            assertThrows<IllegalArgumentException> {
                underTest.getCloudTransferEventByFailedTransfer(unknownTransfer)
            }
        }

    @Test
    fun `test that retryFailedTransfer behaves correctly when the transfer is an offline download transfer`() =
        runTest {
            val id = failedOfflineDownload.id ?: return@runTest
            val idAndEvent = mapOf(id to offlineStartEvent)
            val expectedStartEvent = triggered(TransferTriggerEvent.RetryTransfers(idAndEvent))

            whenever(getNodeByIdUseCase(NodeId(failedOfflineDownload.handle))) doReturn typedNode

            initTestClass()

            with(underTest) {
                retryFailedTransfer(failedOfflineDownload)
                advanceUntilIdle()
                uiState.map { it.startEvent }.test {
                    assertThat(awaitItem()).isEqualTo(expectedStartEvent)
                }
            }
            verifyNoInteractions(deleteCompletedTransfersByIdUseCase)
        }

    @Test
    fun `test that retryFailedTransfer behaves correctly when the transfer is a general download transfer`() =
        runTest {
            val id = cancelledDownload.id ?: return@runTest
            val idAndEvent = mapOf(id to downloadRetryEvent)
            val expectedStartEvent = triggered(TransferTriggerEvent.RetryTransfers(idAndEvent))

            whenever(getNodeByIdUseCase(NodeId(cancelledDownload.handle))) doReturn typedNode

            initTestClass()

            with(underTest) {
                retryFailedTransfer(cancelledDownload)
                advanceUntilIdle()
                uiState.map { it.startEvent }.test {
                    assertThat(awaitItem()).isEqualTo(expectedStartEvent)
                }
            }
            verifyNoInteractions(deleteCompletedTransfersByIdUseCase)
        }

    @Test
    fun `test that retryFailedTransfer does not update startEvent but invokes DeleteCompletedTransfersByIdUseCase when the transfer is a chat upload transfer`() =
        runTest {
            whenever(retryChatUploadUseCase(chatAppData.mapNotNull { it as? TransferAppData.ChatUpload })) doReturn Unit

            initTestClass()

            with(underTest) {
                retryFailedTransfer(cancelledChatUpload)
                advanceUntilIdle()
                uiState.map { it.startEvent }.test {
                    assertThat(awaitItem()).isEqualTo(consumed())
                }
            }
            cancelledChatUpload.id?.let {
                verify(deleteCompletedTransfersByIdUseCase).invoke(listOf(it))
            }
        }

    @Test
    fun `test that retryFailedTransfer updates startEvent with StartUpload and does not invoke DeleteCompletedTransfersByIdUseCase when the transfer is a chat upload transfer`() =
        runTest {
            val chatData = chatAppData.mapNotNull { it as? TransferAppData.ChatUpload }
            val uploadStartEvent = TransferTriggerEvent.StartUpload.Files(
                mapOf(originalPath to null),
                NodeId(cancelledChatUpload.parentHandle)
            )
            val id = cancelledChatUpload.id ?: return@runTest
            val idAndEvent = mapOf(id to uploadStartEvent)
            val expectedStartEvent = triggered(TransferTriggerEvent.RetryTransfers(idAndEvent))

            whenever(retryChatUploadUseCase(chatData)) doThrow ChatUploadNotRetriedException()

            initTestClass()

            with(underTest) {
                retryFailedTransfer(cancelledChatUpload)
                advanceUntilIdle()
                uiState.map { it.startEvent }.test {
                    assertThat(awaitItem()).isEqualTo(expectedStartEvent)
                }
            }

            verifyNoInteractions(deleteCompletedTransfersByIdUseCase)
        }

    @Test
    fun `test that retryFailedTransfer updates startEvent with StartUpload and does not invoke DeleteCompletedTransfersByIdUseCase when the transfer is a general upload`() =
        runTest {
            val id = failedUpload.id ?: return@runTest
            val idAndEvent = mapOf(id to uploadStartEvent)
            val expectedStartEvent = triggered(TransferTriggerEvent.RetryTransfers(idAndEvent))

            whenever(getNodeByIdUseCase(NodeId(failedUpload.handle))) doReturn typedNode

            initTestClass()

            with(underTest) {
                retryFailedTransfer(failedUpload)
                advanceUntilIdle()
                uiState.map { it.startEvent }.test {
                    assertThat(awaitItem()).isEqualTo(expectedStartEvent)
                }
            }

            verifyNoInteractions(deleteCompletedTransfersByIdUseCase)
        }

    @Test
    fun `test that retryFailedTransfer updates startEvent with StartUpload and does not invoke DeleteCompletedTransfersByIdUseCase when the transfer is a general upload from cache`() =
        runTest {
            val id = failedUpload.id ?: return@runTest
            val idAndEvent = mapOf(id to uploadStartEvent)
            val expectedStartEvent = triggered(TransferTriggerEvent.RetryTransfers(idAndEvent))

            whenever(getNodeByIdUseCase(NodeId(failedUpload.handle))) doReturn typedNode

            initTestClass()

            with(underTest) {
                retryFailedTransfer(failedUpload)
                advanceUntilIdle()
                uiState.map { it.startEvent }.test {
                    assertThat(awaitItem()).isEqualTo(expectedStartEvent)
                }
            }
            verifyNoInteractions(deleteCompletedTransfersByIdUseCase)
        }

    @Test
    fun `test that retryAllFailedTransfers updates startEvent in state with RetryTransfers for different transfers and invokes DeleteCompletedTransfersByIdUseCase`() =
        runTest {
            val flow = MutableSharedFlow<List<CompletedTransfer>>()
            val transfersList = listOf(
                failedOfflineDownload,
                cancelledDownload,
                cancelledChatUpload,
            )
            val failedOfflineDownloadId = failedOfflineDownload.id ?: return@runTest
            val cancelledDownloadId = cancelledDownload.id ?: return@runTest
            val cloudTransferEvents = mapOf(
                failedOfflineDownloadId to offlineStartEvent,
                cancelledDownloadId to downloadRetryEvent,
            )
            val expected = TransferTriggerEvent.RetryTransfers(cloudTransferEvents)

            whenever(monitorCompletedTransfersUseCase()) doReturn flow
            whenever(getNodeByIdUseCase(NodeId(failedOfflineDownload.handle))) doReturn typedNode
            whenever(getNodeByIdUseCase(NodeId(cancelledDownload.handle))) doReturn typedNode
            whenever(retryChatUploadUseCase(chatAppData.mapNotNull { it as? TransferAppData.ChatUpload })) doReturn Unit

            initTestClass()

            with(underTest) {
                flow.emit(transfersList)
                retryAllFailedTransfers()
                advanceUntilIdle()
                uiState.map { it.startEvent }.test {
                    assertThat(awaitItem()).isEqualTo(triggered(expected))
                }
            }

            cancelledChatUpload.id?.let {
                verify(deleteCompletedTransfersByIdUseCase).invoke(listOf(it))
            }
        }

    @Test
    fun `test that retryAllFailedTransfers updates startEvent in state with RetryTransfers for different transfers and does not invoke DeleteCompletedTransfersByIdUseCase`() =
        runTest {
            val flow = MutableSharedFlow<List<CompletedTransfer>>()
            val transfersList = listOf(
                failedOfflineDownload,
                cancelledDownload,
                failedUpload,
            )
            val failedOfflineDownloadId = failedOfflineDownload.id ?: return@runTest
            val cancelledDownloadId = cancelledDownload.id ?: return@runTest
            val failedUploadId = failedUpload.id ?: return@runTest
            val cloudTransferEvents = mapOf(
                failedOfflineDownloadId to offlineStartEvent,
                cancelledDownloadId to downloadRetryEvent,
                failedUploadId to uploadStartEvent,
            )
            val expected = TransferTriggerEvent.RetryTransfers(cloudTransferEvents)

            whenever(monitorCompletedTransfersUseCase()) doReturn flow
            whenever(getNodeByIdUseCase(NodeId(failedOfflineDownload.handle))) doReturn typedNode
            whenever(getNodeByIdUseCase(NodeId(cancelledDownload.handle))) doReturn typedNode
            whenever(retryChatUploadUseCase(chatAppData.mapNotNull { it as? TransferAppData.ChatUpload })) doReturn Unit

            initTestClass()

            with(underTest) {
                flow.emit(transfersList)
                advanceUntilIdle()
                retryAllFailedTransfers()
                uiState.map { it.startEvent }.test {
                    assertThat(awaitItem()).isEqualTo(triggered(expected))
                }
            }

            verifyNoInteractions(deleteCompletedTransfersByIdUseCase)
        }

    @Test
    fun `test that clearAllFailedTransfers invokes DeleteFailedOrCancelledTransfersUseCase`() =
        runTest {
            whenever(deleteFailedOrCancelledTransfersUseCase()) doReturn emptyList()

            initTestClass()
            underTest.clearAllFailedTransfers()

            verify(deleteFailedOrCancelledTransfersUseCase).invoke()
        }

    @Test
    fun `test that clearAllCompletedTransfers invokes DeleteCompletedTransfersUseCase`() =
        runTest {
            whenever(deleteCompletedTransfersUseCase()) doReturn Unit

            initTestClass()
            underTest.clearAllCompletedTransfers()

            verify(deleteCompletedTransfersUseCase).invoke()
        }

    @Nested
    inner class SelectModeActiveTransfers {
        @Test
        fun `test that startActiveTransfersSelection set selected transfers to empty list`() =
            runTest {
                initTestClass()
                underTest.uiState.test {
                    assertThat(awaitItem().selectedActiveTransfersIds).isNull()
                    underTest.startActiveTransfersSelection()
                    assertThat(awaitItem().selectedActiveTransfersIds).isEmpty()
                }
            }

        @Test
        fun `test that stopActiveTransfersSelection set selected transfers to null`() = runTest {
            initTestClass()
            underTest.startActiveTransfersSelection()
            underTest.uiState.test {
                assertThat(awaitItem().selectedActiveTransfersIds).isNotNull()
                underTest.stopTransfersSelection()
                assertThat(awaitItem().selectedActiveTransfersIds).isNull()
            }
        }

        @Test
        fun `test that selectActiveTransfer adds the transfer to selected transfers`() = runTest {
            val initialActiveTransfers = (1..10).map { index ->
                mock<InProgressTransfer.Download> {
                    on { this.priority } doReturn index.toBigInteger()
                    on { this.tag } doReturn index
                    on { this.uniqueId } doReturn index.toLong()
                }
            }
            val map = initialActiveTransfers.associateBy { it.uniqueId }
            whenever(monitorInProgressTransfersUseCase()).thenReturn(map.asHotFlow())
            initTestClass()
            underTest.startActiveTransfersSelection()
            val inProgressTransfer = initialActiveTransfers[3]
            val uniqueId = inProgressTransfer.uniqueId
            underTest.uiState.test {
                assertThat(awaitItem().selectedActiveTransfersIds).isEmpty()
                underTest.toggleActiveTransferSelected(inProgressTransfer)
                assertThat(awaitItem().selectedActiveTransfersIds).contains(uniqueId)
            }
        }

        @Test
        fun `test that selectAllActiveTransfers adds all current active transfers to selected transfers`() =
            runTest {
                val initialActiveTransfers = (1..10).map { index ->
                    mock<InProgressTransfer.Download> {
                        on { this.priority } doReturn index.toBigInteger()
                        on { this.tag } doReturn index
                        on { this.uniqueId } doReturn index.toLong()
                    }
                }
                val map = initialActiveTransfers.associateBy { it.uniqueId }
                val uniqueIds = initialActiveTransfers.map { it.uniqueId }
                whenever(monitorInProgressTransfersUseCase()).thenReturn(map.asHotFlow())
                initTestClass()
                underTest.startActiveTransfersSelection()

                underTest.uiState.test {
                    assertThat(awaitItem().selectedActiveTransfersIds).isEmpty()
                    underTest.selectAllActiveTransfers()
                    assertThat(awaitItem().selectedActiveTransfersIds)
                        .containsExactlyElementsIn(uniqueIds)
                }
            }

        @Test
        fun `test that cancelSelectedActiveTransfers cancels selected transfers`() = runTest {
            val tag = 3454
            val inProgressTransfer = mock<InProgressTransfer.Download> {
                on { it.tag } doReturn tag
                on { it.uniqueId } doReturn 546345L
            }
            val inProgressTransfer2 = mock<InProgressTransfer.Download> {
                on { it.tag } doReturn 4643
                on { it.uniqueId } doReturn 94354L
            }
            val flow = flowOf(
                listOf(
                    inProgressTransfer,
                    inProgressTransfer2,
                ).associateBy { it.uniqueId }
            )
            whenever(monitorInProgressTransfersUseCase()).thenReturn(flow)
            initTestClass()

            underTest.toggleActiveTransferSelected(inProgressTransfer)

            underTest.cancelSelectedActiveTransfers()

            verify(cancelTransferByTagUseCase)(tag)
        }
    }

    @Nested
    inner class SelectModeCompletedTransfers {
        @Test
        fun `test that startCompletedTransfersSelection set selected transfers to empty list`() =
            runTest {
                initTestClass()
                underTest.uiState.test {
                    assertThat(awaitItem().selectedCompletedTransfersIds).isNull()
                    underTest.startCompletedTransfersSelection()
                    assertThat(awaitItem().selectedCompletedTransfersIds).isEmpty()
                }
            }

        @Test
        fun `test that stopTransfersSelection set selected transfers to null`() = runTest {
            initTestClass()
            underTest.startCompletedTransfersSelection()
            underTest.uiState.test {
                assertThat(awaitItem().selectedCompletedTransfersIds).isNotNull()
                underTest.stopTransfersSelection()
                assertThat(awaitItem().selectedCompletedTransfersIds).isNull()
            }
        }

        @Test
        fun `test that toggleCompletedTransferSelection adds the transfer to selected transfers`() =
            runTest {
                val completedTransfers = (1..10).map { index ->
                    mock<CompletedTransfer> {
                        on { this.id } doReturn index
                        on { it.state } doReturn TransferState.STATE_COMPLETED
                    }
                }
                whenever(monitorCompletedTransfersUseCase()) doReturn completedTransfers.asHotFlow()
                initTestClass()
                underTest.startCompletedTransfersSelection()
                val completedTransfer = completedTransfers[3]
                val id = completedTransfer.id
                underTest.uiState.test {
                    assertThat(awaitItem().selectedCompletedTransfersIds).isEmpty()
                    underTest.toggleCompletedTransferSelection(completedTransfer)
                    assertThat(awaitItem().selectedCompletedTransfersIds).contains(id)
                }
            }

        @Test
        fun `test that selectAllCompletedTransfers adds all current completed transfers to selected transfers`() =
            runTest {
                val completedTransfers = (1..10).map { index ->
                    mock<CompletedTransfer> {
                        on { this.id } doReturn index
                        on { it.state } doReturn TransferState.STATE_COMPLETED
                    }
                }
                whenever(monitorCompletedTransfersUseCase()) doReturn completedTransfers.asHotFlow()
                initTestClass()
                underTest.startCompletedTransfersSelection()
                val ids = completedTransfers.map { it.id }

                underTest.uiState.test {
                    assertThat(awaitItem().completedTransfers).isNotEmpty()
                    underTest.selectAllCompletedTransfers()
                    assertThat(awaitItem().selectedCompletedTransfersIds)
                        .containsExactlyElementsIn(ids)
                }
            }

        @Test
        fun `test that clearSelectedCompletedTransfers clear selected transfers`() = runTest {
            val completedTransfers = (1..10).map { index ->
                mock<CompletedTransfer> {
                    on { this.id } doReturn index
                    on { it.state } doReturn TransferState.STATE_COMPLETED
                }
            }
            whenever(monitorCompletedTransfersUseCase()) doReturn completedTransfers.asHotFlow()
            initTestClass()
            val completedTransferSelected = completedTransfers[3]
            val id = completedTransferSelected.id ?: -1
            underTest.toggleCompletedTransferSelection(completedTransferSelected)

            underTest.clearSelectedCompletedTransfers()

            verify(deleteCompletedTransfersByIdUseCase)(listOf(id))
        }
    }

    @Nested
    inner class SelectModeFailedTransfers {
        @Test
        fun `test that startFailedTransfersSelection set selected transfers to empty list`() =
            runTest {
                initTestClass()
                underTest.uiState.test {
                    assertThat(awaitItem().selectedFailedTransfersIds).isNull()
                    underTest.startFailedTransfersSelection()
                    assertThat(awaitItem().selectedFailedTransfersIds).isEmpty()
                }
            }

        @Test
        fun `test that stopTransfersSelection set selected transfers to null`() = runTest {
            initTestClass()
            underTest.startFailedTransfersSelection()
            underTest.uiState.test {
                assertThat(awaitItem().selectedFailedTransfersIds).isNotNull()
                underTest.stopTransfersSelection()
                assertThat(awaitItem().selectedFailedTransfersIds).isNull()
            }
        }

        @Test
        fun `test that toggleFailedTransferSelection adds the transfer to selected transfers`() =
            runTest {
                val failedTransfers = (1..10).map { index ->
                    mock<CompletedTransfer> {
                        on { this.id } doReturn index
                        on { it.state } doReturn TransferState.STATE_FAILED
                    }
                }
                whenever(monitorCompletedTransfersUseCase()) doReturn failedTransfers.asHotFlow()
                initTestClass()
                underTest.startFailedTransfersSelection()
                val failedTransfer = failedTransfers[3]
                val id = failedTransfer.id
                underTest.uiState.test {
                    assertThat(awaitItem().selectedFailedTransfersIds).isEmpty()
                    underTest.toggleFailedTransferSelection(failedTransfer)
                    assertThat(awaitItem().selectedFailedTransfersIds).contains(id)
                }
            }

        @Test
        fun `test that selectAllFailedTransfers adds all current failed transfers to selected transfers`() =
            runTest {
                val failedTransfers = (1..10).map { index ->
                    mock<CompletedTransfer> {
                        on { this.id } doReturn index
                        on { it.state } doReturn TransferState.STATE_FAILED
                    }
                }
                whenever(monitorCompletedTransfersUseCase()) doReturn failedTransfers.asHotFlow()
                initTestClass()
                underTest.startFailedTransfersSelection()
                val ids = failedTransfers.map { it.id }

                underTest.uiState.test {
                    assertThat(awaitItem().selectedFailedTransfersIds).isEmpty()
                    underTest.selectAllFailedTransfers()
                    assertThat(awaitItem().selectedFailedTransfersIds)
                        .containsExactlyElementsIn(ids)
                }
            }

        @Test
        fun `test that clearSelectedFailedTransfers clear selected failed transfers`() = runTest {
            val failedTransfers = (1..10).map { index ->
                mock<CompletedTransfer> {
                    on { this.id } doReturn index
                    on { it.state } doReturn TransferState.STATE_FAILED
                }
            }
            whenever(monitorCompletedTransfersUseCase()) doReturn failedTransfers.asHotFlow()
            initTestClass()
            val failedTransferSelected = failedTransfers[3]
            val id = failedTransferSelected.id ?: -1
            underTest.toggleFailedTransferSelection(failedTransferSelected)

            underTest.clearSelectedFailedTransfers()

            verify(deleteCompletedTransfersByIdUseCase)(listOf(id))
        }
    }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}