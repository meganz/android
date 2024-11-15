package mega.privacy.android.app.presentation.transfers

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.presentation.transfers.model.mapper.TransfersInfoMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.TransfersStatusInfo
import mega.privacy.android.domain.entity.transfer.CompletedTransferState
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorLastTransfersHaveBeenCancelledUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersStatusUseCase
import mega.privacy.android.domain.usecase.transfers.completed.MonitorCompletedTransferEventUseCase
import mega.privacy.android.shared.original.core.ui.model.TransfersInfo
import mega.privacy.android.shared.original.core.ui.model.TransfersStatus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransfersManagementViewModelTest {
    private lateinit var underTest: TransfersManagementViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val transfersInfoMapper = mock<TransfersInfoMapper>()
    private val transfersManagement = mock<TransfersManagement>()
    private val monitorLastTransfersHaveBeenCancelledUseCase =
        mock<MonitorLastTransfersHaveBeenCancelledUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()

    private val monitorTransfersStatusFlow = MutableSharedFlow<TransfersStatusInfo>()
    private var monitorConnectivityUseCaseFlow = MutableStateFlow(false)
    private val monitorLastTransfersHaveBeenCancelledUseCaseFlow = MutableSharedFlow<Unit>()
    private val monitorCompletedTransferEventUseCase = mock<MonitorCompletedTransferEventUseCase>()

    @BeforeAll
    fun setup() = runTest {
        commonStub()
        initTest()
    }

    private fun initTest() {
        //this mocks are only used in viewmodel init, so no need to reset
        val monitorTransfersStatusUseCase = mock<MonitorTransfersStatusUseCase>()
        whenever(monitorTransfersStatusUseCase()) doReturn monitorTransfersStatusFlow
        underTest = TransfersManagementViewModel(
            getNumPendingTransfersUseCase = mock(),
            isCompletedTransfersEmptyUseCase = mock(),
            transfersInfoMapper = transfersInfoMapper,
            transfersManagement = transfersManagement,
            ioDispatcher = ioDispatcher,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            monitorTransfersStatusUseCase = monitorTransfersStatusUseCase,
            monitorLastTransfersHaveBeenCancelledUseCase = monitorLastTransfersHaveBeenCancelledUseCase,
            monitorCompletedTransfersEventUseCase = monitorCompletedTransferEventUseCase,
            samplePeriod = 0L,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            transfersInfoMapper,
            transfersManagement,
            monitorConnectivityUseCase,
            monitorLastTransfersHaveBeenCancelledUseCase,
        )
        commonStub()
    }

    @Test
    fun `test ui state is updated with correct values when there's a new emission of monitorTransfersSize`() =
        runTest {
            val pendingDownloads = 5
            val pendingUploads = 4
            val totalSizeTransferred = 3L
            val totalSizeToTransfer = 5L
            val transfersStatusInfo = TransfersStatusInfo(
                totalSizeToTransfer,
                totalSizeTransferred,
                pendingUploads,
                pendingDownloads,
            )
            val expected = TransfersInfo(
                status = TransfersStatus.Transferring,
                totalSizeAlreadyTransferred = totalSizeTransferred,
                totalSizeToTransfer = totalSizeToTransfer,
                uploading = true
            )
            whenever(
                transfersInfoMapper(
                    numPendingUploads = eq(pendingUploads),
                    numPendingDownloadsNonBackground = eq(pendingDownloads),
                    totalSizeToTransfer = eq(totalSizeToTransfer),
                    totalSizeTransferred = eq(totalSizeTransferred),
                    areTransfersPaused = eq(false),
                    isTransferError = eq(false),
                    isTransferOverQuota = eq(false),
                    isStorageOverQuota = eq(false),
                    lastTransfersCancelled = any(),
                )
            ) doReturn expected

            initTest()

            underTest.state.test {
                awaitItem() // Skip initial value
                monitorTransfersStatusFlow.emit(transfersStatusInfo)
                val actual = awaitItem().transfersInfo
                assertThat(actual).isEqualTo(expected)
            }
        }

    @Test
    fun `test that when monitorConnectivityUseCase turns to true it calls transfersManagement resetNetworkTimer`() =
        runTest {
            initTest()

            monitorConnectivityUseCaseFlow.emit(true)

            verify(transfersManagement).resetNetworkTimer()
        }

    @Test
    fun `test that when monitorConnectivityUseCase turns to false it calls transfersManagement startNetworkTimer`() =
        runTest {
            initTest()

            monitorConnectivityUseCaseFlow.emit(false)

            verify(transfersManagement).startNetworkTimer()
        }

    @Test
    fun `test that lastTransfersCancelled ui state is updated to true when monitorLastTransfersHaveBeenCancelledUseCase emits`() =
        runTest {
            assertThat(underTest.state.value.lastTransfersCancelled).isFalse()
            monitorLastTransfersHaveBeenCancelledUseCaseFlow.emit(Unit)
            assertThat(underTest.state.value.lastTransfersCancelled).isTrue()
        }

    @ParameterizedTest(name = " if use case returns {0}")
    @EnumSource(CompletedTransferState::class)
    fun `test that monitorFailedTransfers updates state`(
        completedTransferState: CompletedTransferState,
    ) = runTest {
        whenever(monitorCompletedTransferEventUseCase()) doReturn flowOf(completedTransferState)

        initTest()

        underTest.state.map { it.isTransferError }.test {
            assertThat(awaitItem()).isEqualTo(completedTransferState == CompletedTransferState.Error)
        }
    }

    @ParameterizedTest(name = " if use case returns {0}")
    @EnumSource(CompletedTransferState::class)
    fun `test that shouldCheckTransferError updates state and returns correctly`(
        completedTransferState: CompletedTransferState,
    ) = runTest {
        whenever(monitorCompletedTransferEventUseCase()) doReturn flowOf(completedTransferState)

        initTest()

        assertThat(underTest.shouldCheckTransferError()).isEqualTo(completedTransferState == CompletedTransferState.Error)
        underTest.state.map { it.isTransferError }.test {
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `test that shouldCheckTransferError updates transfersInfo in state with Transferring status when transferInfo status is error`() =
        runTest {
            val pendingDownloads = 5
            val pendingUploads = 4
            val totalSizeTransferred = 3L
            val totalSizeToTransfer = 5L
            val transfersStatusInfo = TransfersStatusInfo(
                totalSizeToTransfer,
                totalSizeTransferred,
                pendingUploads,
                pendingDownloads,
            )
            val expected = TransfersInfo(
                status = TransfersStatus.TransferError,
                totalSizeAlreadyTransferred = totalSizeTransferred,
                totalSizeToTransfer = totalSizeToTransfer,
                uploading = true
            )
            val finalExpected = expected.copy(status = TransfersStatus.Transferring)

            whenever(
                transfersInfoMapper(
                    numPendingUploads = eq(pendingUploads),
                    numPendingDownloadsNonBackground = eq(pendingDownloads),
                    totalSizeToTransfer = eq(totalSizeToTransfer),
                    totalSizeTransferred = eq(totalSizeTransferred),
                    areTransfersPaused = eq(false),
                    isTransferError = eq(true),
                    isTransferOverQuota = eq(false),
                    isStorageOverQuota = eq(false),
                    lastTransfersCancelled = any(),
                )
            ) doReturn expected
            whenever(monitorCompletedTransferEventUseCase()) doReturn flowOf(CompletedTransferState.Error)

            initTest()

            with(underTest) {
                state.map { it.transfersInfo }.test {
                    awaitItem()
                    monitorTransfersStatusFlow.emit(transfersStatusInfo)
                    assertThat(awaitItem()).isEqualTo(expected)
                }

                shouldCheckTransferError()

                state.map { it.transfersInfo }.test {
                    assertThat(awaitItem()).isEqualTo(finalExpected)
                }
            }
        }

    @Test
    fun `test that shouldCheckTransferError updates transfersInfo in state with Completed status when transferInfo status is error`() =
        runTest {
            val pendingDownloads = 5
            val pendingUploads = 4
            val totalSizeTransferred = 3L
            val totalSizeToTransfer = 0L
            val transfersStatusInfo = TransfersStatusInfo(
                totalSizeToTransfer,
                totalSizeTransferred,
                pendingUploads,
                pendingDownloads,
            )
            val expected = TransfersInfo(
                status = TransfersStatus.TransferError,
                totalSizeAlreadyTransferred = totalSizeTransferred,
                totalSizeToTransfer = totalSizeToTransfer,
                uploading = true
            )
            val finalExpected = expected.copy(status = TransfersStatus.Completed)

            whenever(
                transfersInfoMapper(
                    numPendingUploads = eq(pendingUploads),
                    numPendingDownloadsNonBackground = eq(pendingDownloads),
                    totalSizeToTransfer = eq(totalSizeToTransfer),
                    totalSizeTransferred = eq(totalSizeTransferred),
                    areTransfersPaused = eq(false),
                    isTransferError = eq(true),
                    isTransferOverQuota = eq(false),
                    isStorageOverQuota = eq(false),
                    lastTransfersCancelled = any(),
                )
            ) doReturn expected
            whenever(monitorCompletedTransferEventUseCase()) doReturn flowOf(CompletedTransferState.Error)

            initTest()

            with(underTest) {
                state.map { it.transfersInfo }.test {
                    awaitItem()
                    monitorTransfersStatusFlow.emit(transfersStatusInfo)
                    assertThat(awaitItem()).isEqualTo(expected)
                }

                shouldCheckTransferError()

                state.map { it.transfersInfo }.test {
                    assertThat(awaitItem()).isEqualTo(finalExpected)
                }
            }
        }

    @Test
    fun `test that shouldCheckTransferError does not updates transfersInfo in state when transfer info status is transferring`() =
        runTest {
            val pendingDownloads = 5
            val pendingUploads = 4
            val totalSizeTransferred = 3L
            val totalSizeToTransfer = 5L
            val transfersStatusInfo = TransfersStatusInfo(
                totalSizeToTransfer,
                totalSizeTransferred,
                pendingUploads,
                pendingDownloads,
            )
            val expected = TransfersInfo(
                status = TransfersStatus.Transferring,
                totalSizeAlreadyTransferred = totalSizeTransferred,
                totalSizeToTransfer = totalSizeToTransfer,
                uploading = true
            )

            whenever(
                transfersInfoMapper(
                    numPendingUploads = eq(pendingUploads),
                    numPendingDownloadsNonBackground = eq(pendingDownloads),
                    totalSizeToTransfer = eq(totalSizeToTransfer),
                    totalSizeTransferred = eq(totalSizeTransferred),
                    areTransfersPaused = eq(false),
                    isTransferError = eq(true),
                    isTransferOverQuota = eq(false),
                    isStorageOverQuota = eq(false),
                    lastTransfersCancelled = any(),
                )
            ) doReturn expected
            whenever(monitorCompletedTransferEventUseCase()) doReturn flowOf(CompletedTransferState.Error)

            initTest()

            with(underTest) {
                state.map { it.transfersInfo }.test {
                    awaitItem()
                    monitorTransfersStatusFlow.emit(transfersStatusInfo)
                    assertThat(awaitItem()).isEqualTo(expected)
                }

                shouldCheckTransferError()

                state.map { it.transfersInfo }.test {
                    assertThat(awaitItem()).isEqualTo(expected)
                }
            }
        }

    private fun commonStub() {
        monitorConnectivityUseCaseFlow = MutableStateFlow(false)
        whenever(transfersManagement.shouldShowNetworkWarning) doReturn false
        whenever(monitorConnectivityUseCase()) doReturn monitorConnectivityUseCaseFlow
        whenever(monitorLastTransfersHaveBeenCancelledUseCase()) doReturn monitorLastTransfersHaveBeenCancelledUseCaseFlow
        whenever(
            transfersInfoMapper(
                numPendingUploads = any(),
                numPendingDownloadsNonBackground = any(),
                totalSizeToTransfer = any(),
                totalSizeTransferred = any(),
                areTransfersPaused = any(),
                isTransferError = any(),
                isTransferOverQuota = any(),
                isStorageOverQuota = any(),
                lastTransfersCancelled = any(),
            )
        ) doReturn TransfersInfo()
        whenever(monitorCompletedTransferEventUseCase()) doReturn emptyFlow()
    }
}