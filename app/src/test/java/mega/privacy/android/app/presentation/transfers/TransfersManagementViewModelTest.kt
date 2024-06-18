package mega.privacy.android.app.presentation.transfers

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.presentation.transfers.model.mapper.TransfersInfoMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.TransfersInfo
import mega.privacy.android.domain.entity.TransfersSizeInfo
import mega.privacy.android.domain.entity.TransfersStatus
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersSizeUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetNumPendingDownloadsNonBackgroundUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreAllTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.GetNumPendingUploadsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransfersManagementViewModelTest {
    private lateinit var underTest: TransfersManagementViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val areAllTransfersPausedUseCase = mock<AreAllTransfersPausedUseCase>()
    private val transfersInfoMapper = mock<TransfersInfoMapper>()
    private val getNumPendingDownloadsNonBackgroundUseCase =
        mock<GetNumPendingDownloadsNonBackgroundUseCase>()
    private val getNumPendingUploadsUseCase = mock<GetNumPendingUploadsUseCase>()
    private val transfersManagement = mock<TransfersManagement>()

    private val monitorTransfersSizeFlow = MutableSharedFlow<TransfersSizeInfo>()

    @BeforeAll
    fun setup() = runTest {
        //this mocks are used in viewmodel init only, so no need to reset
        val monitorTransfersSize = mock<MonitorTransfersSizeUseCase>()
        val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
        whenever(monitorTransfersSize()) doReturn monitorTransfersSizeFlow
        whenever(getFeatureFlagValueUseCase(AppFeatures.UploadWorker)) doReturn true
        whenever(areAllTransfersPausedUseCase()) doReturn true //to don't trigger initial update

        underTest = TransfersManagementViewModel(
            getNumPendingDownloadsNonBackgroundUseCase = getNumPendingDownloadsNonBackgroundUseCase,
            getNumPendingUploadsUseCase = getNumPendingUploadsUseCase,
            getNumPendingTransfersUseCase = mock(),
            isCompletedTransfersEmptyUseCase = mock(),
            areAllTransfersPausedUseCase = areAllTransfersPausedUseCase,
            transfersInfoMapper = transfersInfoMapper,
            transfersManagement = transfersManagement,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            ioDispatcher = ioDispatcher,
            monitorConnectivityUseCase = mock(),
            monitorTransfersSize = monitorTransfersSize,
            samplePeriod = 0L,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            areAllTransfersPausedUseCase,
            transfersInfoMapper,
            getNumPendingDownloadsNonBackgroundUseCase,
            getNumPendingUploadsUseCase,
        )
    }

    @Test
    fun `test ui state is updated with correct values when there's a new emission of monitorTransfersSize`() =
        runTest {
            commonStub()
            val pendingDownloads = 5
            val pendingUploads = 4
            val totalSizeTransferred = 3L
            val totalSizeToTransfer = 5L
            val transfersSizeInfo = TransfersSizeInfo(
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
                monitorTransfersSizeFlow.emit(transfersSizeInfo)
                val actual = awaitItem().transfersInfo
                assertThat(actual).isEqualTo(expected)
            }
        }

    private fun commonStub() = runTest {
        whenever(getNumPendingDownloadsNonBackgroundUseCase()) doReturn 0
        whenever(getNumPendingUploadsUseCase()) doReturn 0
        whenever(areAllTransfersPausedUseCase()) doReturn false
        whenever(transfersManagement.getAreFailedTransfers()) doReturn false
        whenever(transfersManagement.shouldShowNetworkWarning) doReturn false
    }
}