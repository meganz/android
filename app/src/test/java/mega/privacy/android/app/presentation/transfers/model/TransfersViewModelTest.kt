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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.extensions.asHotFlow
import mega.privacy.android.app.extensions.moveElement
import mega.privacy.android.app.presentation.transfers.EXTRA_TAB
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.data.mapper.transfer.TransferAppDataMapper
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.chat.ChatUploadNotRetriedException
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.message.pendingmessages.RetryChatUploadUseCase
import mega.privacy.android.domain.usecase.file.CanReadUriUseCase
import mega.privacy.android.domain.usecase.file.IsUriPathInCacheUseCase
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
import nz.mega.sdk.MegaTransfer
import okhttp3.internal.immutableListOf
import org.junit.jupiter.api.BeforeEach
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
    private val canReadUriUseCase = mock<CanReadUriUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val retryChatUploadUseCase = mock<RetryChatUploadUseCase>()
    private val deleteFailedOrCancelledTransfersUseCase =
        mock<DeleteFailedOrCancelledTransfersUseCase>()
    private val deleteCompletedTransfersUseCase = mock<DeleteCompletedTransfersUseCase>()
    private val deleteCompletedTransfersByIdUseCase = mock<DeleteCompletedTransfersByIdUseCase>()
    private val isUriPathInCacheUseCase = mock<IsUriPathInCacheUseCase>()
    private val transferAppDataMapper = mock<TransferAppDataMapper>()

    private val originalPath = "originalPath"
    private val appDataString = "appDataString"
    private val chatAppData = listOf<TransferAppData>(TransferAppData.ChatUpload(1))
    private val originalUriPathAppData =
        listOf<TransferAppData>(TransferAppData.OriginalUriPath(UriPath(originalPath)))
    private val failedOfflineDownload = mock<CompletedTransfer> {
        on { id } doReturn 1
        on { state } doReturn MegaTransfer.STATE_FAILED
        on { timestamp } doReturn 1L
        on { handle } doReturn 1L
        on { type } doReturn MegaTransfer.TYPE_DOWNLOAD
        on { isOffline } doReturn true
    }
    private val cancelledDownload = mock<CompletedTransfer> {
        on { id } doReturn 2
        on { state } doReturn MegaTransfer.STATE_CANCELLED
        on { timestamp } doReturn 2L
        on { handle } doReturn 2L
        on { type } doReturn MegaTransfer.TYPE_DOWNLOAD
    }
    private val cancelledChatUpload = mock<CompletedTransfer> {
        on { id } doReturn 3
        on { state } doReturn MegaTransfer.STATE_CANCELLED
        on { timestamp } doReturn 3L
        on { type } doReturn MegaTransfer.TYPE_UPLOAD
        on { this.appData } doReturn appDataString
        on { this.originalPath } doReturn originalPath
    }
    private val failedUpload = mock<CompletedTransfer> {
        on { id } doReturn 4
        on { state } doReturn MegaTransfer.STATE_FAILED
        on { timestamp } doReturn 4L
        on { parentHandle } doReturn 4L
        on { type } doReturn MegaTransfer.TYPE_UPLOAD
        on { this.appData } doReturn appDataString
        on { this.originalPath } doReturn originalPath
    }
    val typedNode = mock<TypedNode>()
    val offlineStartEvent = TransferTriggerEvent.StartDownloadForOffline(
        node = typedNode,
        withStartMessage = false
    )
    val downloadStartEvent = TransferTriggerEvent.StartDownloadNode(
        nodes = listOf(typedNode),
        withStartMessage = false
    )
    val uploadStartEvent = TransferTriggerEvent.StartUpload.Files(
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
            canReadUriUseCase,
            getNodeByIdUseCase,
            retryChatUploadUseCase,
            deleteFailedOrCancelledTransfersUseCase,
            deleteCompletedTransfersUseCase,
            deleteCompletedTransfersByIdUseCase,
            isUriPathInCacheUseCase,
            transferAppDataMapper,
        )
        wheneverBlocking { monitorInProgressTransfersUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorStorageStateEventUseCase() } doReturn MutableStateFlow(
            StorageStateEvent(1L, "", 0L, "", EventType.Unknown, StorageState.Unknown)
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
            canReadUriUseCase = canReadUriUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            retryChatUploadUseCase = retryChatUploadUseCase,
            deleteFailedOrCancelledTransfersUseCase = deleteFailedOrCancelledTransfersUseCase,
            deleteCompletedTransfersUseCase = deleteCompletedTransfersUseCase,
            deleteCompletedTransfersByIdUseCase = deleteCompletedTransfersByIdUseCase,
            isUriPathInCacheUseCase = isUriPathInCacheUseCase,
            transferAppDataMapper = transferAppDataMapper,
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
                "",
                0L,
                "",
                EventType.Unknown,
                storageState = StorageState.Unknown,
            )
        )

        whenever(monitorStorageStateEventUseCase()).thenReturn(flow)

        initTestClass()

        underTest.uiState.map { it.isStorageOverQuota }.test {
            assertThat(awaitItem()).isFalse()
            flow.emit(
                StorageStateEvent(
                    1L,
                    "",
                    0L,
                    "",
                    EventType.Unknown,
                    storageState = storageState,
                )
            )
            advanceUntilIdle()

            if (storageState == StorageState.Red || storageState == StorageState.PayWall) {
                assertThat(awaitItem()).isTrue()
            }
        }
    }

    @Test
    fun `test that MonitorTransferOverQuotaUseCase updates state with transfer over quota`() =
        runTest {
            val flow = MutableStateFlow(false)

            whenever(monitorTransferOverQuotaUseCase()).thenReturn(flow)

            initTestClass()

            underTest.uiState.map { it.isTransferOverQuota }.test {
                assertThat(awaitItem()).isFalse()
                flow.emit(true)
                assertThat(awaitItem()).isTrue()
                flow.emit(false)
                assertThat(awaitItem()).isFalse()
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
                on { state } doReturn MegaTransfer.STATE_COMPLETED
                on { timestamp } doReturn 1L
            }
            val transfer2 = mock<CompletedTransfer> {
                on { state } doReturn MegaTransfer.STATE_FAILED
                on { timestamp } doReturn 2L
            }
            val transfer3 = mock<CompletedTransfer> {
                on { state } doReturn MegaTransfer.STATE_COMPLETED
                on { timestamp } doReturn 3L
            }
            val transfer4 = mock<CompletedTransfer> {
                on { state } doReturn MegaTransfer.STATE_CANCELLED
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
                on { state } doReturn MegaTransfer.STATE_COMPLETED
                on { timestamp } doReturn 1L
            }
            val transfer2 = mock<CompletedTransfer> {
                on { state } doReturn MegaTransfer.STATE_COMPLETED
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
                on { state } doReturn MegaTransfer.STATE_CANCELLED
                on { timestamp } doReturn 1L
            }
            val transfer2 = mock<CompletedTransfer> {
                on { state } doReturn MegaTransfer.STATE_FAILED
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
    fun `test that retryFailedTransfer updates readRetryError in state correctly when cannot read the transfer Uri when the transfer is a failed upload`() =
        runTest {
            whenever(transferAppDataMapper(appDataString)) doReturn originalUriPathAppData
            whenever(isUriPathInCacheUseCase(UriPath(originalPath))) doReturn false
            whenever(canReadUriUseCase(originalPath)) doReturn false

            initTestClass()

            with(underTest) {
                retryFailedTransfer(failedUpload)
                advanceUntilIdle()
                uiState.map { it.readRetryError }.test {
                    assertThat(awaitItem()).isEqualTo(1)
                }
            }
        }

    @Test
    fun `test that retryFailedTransfer throws IllegalArgumentException if transfer is not upload or download`() =
        runTest {
            val unknownTransfer = mock<CompletedTransfer> {
                on { id } doReturn 4
                on { state } doReturn MegaTransfer.STATE_FAILED
                on { timestamp } doReturn 4L
                on { parentHandle } doReturn 4L
                on { type } doReturn 3
                on { this.originalPath } doReturn originalPath
            }

            initTestClass()

            assertThrows<IllegalArgumentException> {
                underTest.getStartTransferEventByFailedTransfer(unknownTransfer)
            }
        }

    @Test
    fun `test that retryFailedTransfer updates startEvent with StartDownloadForOffline and invokes DeleteCompletedTransfersByIdUseCase when the transfer is an offline download transfer`() =
        runTest {
            whenever(canReadUriUseCase(originalPath)) doReturn true
            whenever(getNodeByIdUseCase(NodeId(failedOfflineDownload.handle))) doReturn typedNode

            initTestClass()

            with(underTest) {
                retryFailedTransfer(failedOfflineDownload)
                advanceUntilIdle()
                uiState.map { it.startEvent }.test {
                    assertThat(awaitItem()).isEqualTo(triggered(offlineStartEvent))
                }
            }
            failedOfflineDownload.id?.let {
                verify(deleteCompletedTransfersByIdUseCase).invoke(listOf(it))
            }
        }

    @Test
    fun `test that retryFailedTransfer updates startEvent with StartDownloadNode and invokes DeleteCompletedTransfersByIdUseCase when the transfer is a general download transfer`() =
        runTest {
            whenever(canReadUriUseCase(originalPath)) doReturn true
            whenever(getNodeByIdUseCase(NodeId(cancelledDownload.handle))) doReturn typedNode

            initTestClass()

            with(underTest) {
                retryFailedTransfer(cancelledDownload)
                advanceUntilIdle()
                uiState.map { it.startEvent }.test {
                    assertThat(awaitItem()).isEqualTo(triggered(downloadStartEvent))
                }
            }
            cancelledDownload.id?.let {
                verify(deleteCompletedTransfersByIdUseCase).invoke(listOf(it))
            }
        }

    @Test
    fun `test that retryFailedTransfer does not update startEvent but invokes DeleteCompletedTransfersByIdUseCase when the transfer is a chat upload transfer`() =
        runTest {
            whenever(isUriPathInCacheUseCase(UriPath(originalPath))) doReturn false
            whenever(canReadUriUseCase(originalPath)) doReturn true
            whenever(transferAppDataMapper(appDataString)) doReturn chatAppData
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
    fun `test that retryFailedTransfer updates startEvent with StartUpload and invokes DeleteCompletedTransfersByIdUseCase when the transfer is a chat upload transfer`() =
        runTest {
            val chatData = chatAppData.mapNotNull { it as? TransferAppData.ChatUpload }
            val uploadStartEvent = triggered(
                TransferTriggerEvent.StartUpload.Files(
                    mapOf(originalPath to null),
                    NodeId(cancelledChatUpload.parentHandle)
                )
            )

            whenever(isUriPathInCacheUseCase(UriPath(originalPath))) doReturn false
            whenever(canReadUriUseCase(originalPath)) doReturn true
            whenever(transferAppDataMapper(appDataString)) doReturn chatAppData
            whenever(retryChatUploadUseCase(chatData)) doThrow ChatUploadNotRetriedException()

            initTestClass()

            with(underTest) {
                retryFailedTransfer(cancelledChatUpload)
                advanceUntilIdle()
                uiState.map { it.startEvent }.test {
                    assertThat(awaitItem()).isEqualTo(uploadStartEvent)
                }
            }
            cancelledChatUpload.id?.let {
                verify(deleteCompletedTransfersByIdUseCase).invoke(listOf(it))
            }
        }

    @Test
    fun `test that retryFailedTransfer updates startEvent with StartUpload and invokes DeleteCompletedTransfersByIdUseCase when the transfer is a general upload`() =
        runTest {
            whenever(isUriPathInCacheUseCase(UriPath(originalPath))) doReturn false
            whenever(canReadUriUseCase(originalPath)) doReturn true
            whenever(getNodeByIdUseCase(NodeId(failedUpload.handle))) doReturn typedNode

            initTestClass()

            with(underTest) {
                retryFailedTransfer(failedUpload)
                advanceUntilIdle()
                uiState.map { it.startEvent }.test {
                    assertThat(awaitItem()).isEqualTo(triggered(uploadStartEvent))
                }
            }
            failedUpload.id?.let {
                verify(deleteCompletedTransfersByIdUseCase).invoke(listOf(it))
            }
        }

    @Test
    fun `test that retryFailedTransfer updates startEvent with StartUpload and invokes DeleteCompletedTransfersByIdUseCase when the transfer is a general upload from cache`() =
        runTest {
            whenever(isUriPathInCacheUseCase(UriPath(originalPath))) doReturn true
            whenever(getNodeByIdUseCase(NodeId(failedUpload.handle))) doReturn typedNode

            initTestClass()

            with(underTest) {
                retryFailedTransfer(failedUpload)
                advanceUntilIdle()
                uiState.map { it.startEvent }.test {
                    assertThat(awaitItem()).isEqualTo(triggered(uploadStartEvent))
                }
            }
            failedUpload.id?.let {
                verify(deleteCompletedTransfersByIdUseCase).invoke(listOf(it))
            }
        }

    @Test
    fun `test that retryAllFailedTransfers updates readRetryError in state correctly when cannot read one of three transfer Uris`() =
        runTest {
            val flow = MutableSharedFlow<List<CompletedTransfer>>()
            val list = listOf(
                failedOfflineDownload,
                cancelledDownload,
                failedUpload,
                cancelledChatUpload
            )

            whenever(monitorCompletedTransfersUseCase()) doReturn flow
            whenever(transferAppDataMapper(appDataString))
                .thenReturn(chatAppData, originalUriPathAppData)
            whenever(isUriPathInCacheUseCase(UriPath(originalPath))) doReturn false
            whenever(canReadUriUseCase(originalPath)) doReturn false

            initTestClass()

            with(underTest) {
                flow.emit(list)
                retryAllFailedTransfers()
                advanceUntilIdle()
                uiState.map { it.readRetryError }.test {
                    assertThat(awaitItem()).isEqualTo(1)
                }
            }
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
            val startTransferEvents = listOf(
                StartTransferEvent(failedOfflineDownload.id, offlineStartEvent),
                StartTransferEvent(cancelledDownload.id, downloadStartEvent),
                StartTransferEvent(cancelledChatUpload.id, null),
            )

            val notNullStartEvents = startTransferEvents
                .filter { it.id != null && it.event != null }
                .associate { it.id!! to it.event!! }

            val expected = TransferTriggerEvent.RetryTransfers(notNullStartEvents)

            whenever(monitorCompletedTransfersUseCase()) doReturn flow
            whenever(transferAppDataMapper(appDataString)) doReturn chatAppData
            whenever(getNodeByIdUseCase(NodeId(failedOfflineDownload.handle))) doReturn typedNode
            whenever(getNodeByIdUseCase(NodeId(cancelledDownload.handle))) doReturn typedNode
            whenever(retryChatUploadUseCase(chatAppData.mapNotNull { it as? TransferAppData.ChatUpload })) doReturn Unit
            whenever(isUriPathInCacheUseCase(UriPath(originalPath))) doReturn false
            whenever(canReadUriUseCase(originalPath)) doReturn true

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
            val startTransferEvents = listOf(
                StartTransferEvent(failedOfflineDownload.id, offlineStartEvent),
                StartTransferEvent(cancelledDownload.id, downloadStartEvent),
                StartTransferEvent(failedUpload.id, uploadStartEvent),
            )

            val notNullStartEvents = startTransferEvents
                .filter { it.id != null && it.event != null }
                .associate { it.id!! to it.event!! }

            val expected = TransferTriggerEvent.RetryTransfers(notNullStartEvents)

            whenever(monitorCompletedTransfersUseCase()) doReturn flow
            whenever(transferAppDataMapper(appDataString)) doReturn originalUriPathAppData
            whenever(getNodeByIdUseCase(NodeId(failedOfflineDownload.handle))) doReturn typedNode
            whenever(getNodeByIdUseCase(NodeId(cancelledDownload.handle))) doReturn typedNode
            whenever(retryChatUploadUseCase(chatAppData.mapNotNull { it as? TransferAppData.ChatUpload })) doReturn Unit
            whenever(isUriPathInCacheUseCase(UriPath(originalPath))) doReturn false
            whenever(canReadUriUseCase(originalPath)) doReturn true

            initTestClass()

            with(underTest) {
                flow.emit(transfersList)
                retryAllFailedTransfers()
                advanceUntilIdle()
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

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}