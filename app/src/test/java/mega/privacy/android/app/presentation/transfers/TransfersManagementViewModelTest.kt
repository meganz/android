package mega.privacy.android.app.presentation.transfers

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.presentation.transfers.model.mapper.TransfersInfoMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.TransfersStatusInfo
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersStatusUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreAllTransfersPausedUseCase
import mega.privacy.android.shared.original.core.ui.model.TransfersInfo
import mega.privacy.android.shared.original.core.ui.model.TransfersStatus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransfersManagementViewModelTest {
    private lateinit var underTest: TransfersManagementViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val areAllTransfersPausedUseCase = mock<AreAllTransfersPausedUseCase>()
    private val transfersInfoMapper = mock<TransfersInfoMapper>()
    private val transfersManagement = mock<TransfersManagement>()

    private val monitorTransfersSizeFlow = MutableSharedFlow<TransfersStatusInfo>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val monitorConnectivityUseCaseFlow = MutableStateFlow(false)

    @BeforeAll
    fun setup() = runTest {
        //this mocks are only used in viewmodel init, so no need to reset
        val monitorTransfersSize = mock<MonitorTransfersStatusUseCase>()
        whenever(monitorTransfersSize()) doReturn monitorTransfersSizeFlow
        commonStub()

        underTest = TransfersManagementViewModel(
            getNumPendingTransfersUseCase = mock(),
            isCompletedTransfersEmptyUseCase = mock(),
            transfersInfoMapper = transfersInfoMapper,
            transfersManagement = transfersManagement,
            ioDispatcher = ioDispatcher,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            monitorTransfersSize = monitorTransfersSize,
            samplePeriod = 0L,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            areAllTransfersPausedUseCase,
            transfersInfoMapper,
            transfersManagement,
            monitorConnectivityUseCase,
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
                )
            ) doReturn expected
            underTest.state.test {
                awaitItem() // Skip initial value
                monitorTransfersSizeFlow.emit(transfersStatusInfo)
                val actual = awaitItem().transfersInfo
                assertThat(actual).isEqualTo(expected)
            }
        }

    @Test
    fun `test that when monitorConnectivityUseCase turns to true it calls transfersManagement resetNetworkTimer`() =
        runTest {
            monitorConnectivityUseCaseFlow.value = false // to ensure it changes to true
            monitorConnectivityUseCaseFlow.value = true
            verify(transfersManagement).resetNetworkTimer()
        }

    @Test
    fun `test that when monitorConnectivityUseCase turns to false it calls transfersManagement startNetworkTimer`() =
        runTest {
            monitorConnectivityUseCaseFlow.value = true // to ensure it changes to false
            monitorConnectivityUseCaseFlow.value = false
            verify(transfersManagement).startNetworkTimer()
        }

    private suspend fun commonStub() {
        whenever(areAllTransfersPausedUseCase()) doReturn false
        whenever(transfersManagement.getAreFailedTransfers()) doReturn false
        whenever(transfersManagement.shouldShowNetworkWarning) doReturn false
        whenever(monitorConnectivityUseCase()) doReturn monitorConnectivityUseCaseFlow
    }
}