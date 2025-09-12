package mega.privacy.android.app.presentation.transfers

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.transfers.TransfersManagementViewModel.Companion.waitTimeToShowOffline
import mega.privacy.android.app.presentation.transfers.model.mapper.TransfersInfoMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.TransfersStatusInfo
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorLastTransfersHaveBeenCancelledUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersStatusUseCase
import mega.privacy.android.domain.usecase.transfers.errorstatus.MonitorTransferInErrorStatusUseCase
import mega.privacy.android.shared.original.core.ui.model.TransfersInfo
import mega.privacy.android.shared.original.core.ui.model.TransfersStatus
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransfersManagementViewModelTest {
    private lateinit var underTest: TransfersManagementViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val transfersInfoMapper = mock<TransfersInfoMapper>()
    private val monitorLastTransfersHaveBeenCancelledUseCase =
        mock<MonitorLastTransfersHaveBeenCancelledUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()

    private val monitorTransfersStatusFlow = MutableSharedFlow<TransfersStatusInfo>()
    private var monitorConnectivityUseCaseFlow = MutableStateFlow(false)
    private val monitorLastTransfersHaveBeenCancelledUseCaseFlow = MutableSharedFlow<Unit>()
    private val monitorTransferInErrorStatusUseCase = mock<MonitorTransferInErrorStatusUseCase>()

    @BeforeAll
    fun setup() = runTest {
        Dispatchers.setMain(ioDispatcher)
        commonStub()
        initTest()
    }

    private fun initTest() {
        //this mocks are only used in viewmodel init, so no need to reset
        val monitorTransfersStatusUseCase = mock<MonitorTransfersStatusUseCase>()
        whenever(monitorTransfersStatusUseCase()) doReturn monitorTransfersStatusFlow
        underTest = TransfersManagementViewModel(
            transfersInfoMapper = transfersInfoMapper,
            ioDispatcher = ioDispatcher,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            monitorTransfersStatusUseCase = monitorTransfersStatusUseCase,
            monitorLastTransfersHaveBeenCancelledUseCase = monitorLastTransfersHaveBeenCancelledUseCase,
            monitorTransferInErrorStatusUseCase = monitorTransferInErrorStatusUseCase,
            samplePeriod = 0L,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            transfersInfoMapper,
            monitorConnectivityUseCase,
            monitorLastTransfersHaveBeenCancelledUseCase,
        )
        commonStub()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
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
                    isOnline = eq(true),
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
    fun `test that isOnline ui state is updated when monitorConnectivityUseCase emits, skipping unstable offline`() =
        runTest {
            initTest()
            underTest.state.test {
                monitorConnectivityUseCaseFlow.emit(true)
                assertThat(awaitItem().isOnline).isTrue()
                monitorConnectivityUseCaseFlow.emit(false)
                delay(waitTimeToShowOffline / 2)
                monitorConnectivityUseCaseFlow.emit(true)
                expectNoEvents() //short offline is skipped
                monitorConnectivityUseCaseFlow.emit(false)
                delay(waitTimeToShowOffline * 1.1)
                assertThat(awaitItem().isOnline).isFalse() // long offline is not skipped
                monitorConnectivityUseCaseFlow.emit(true)
                assertThat(awaitItem().isOnline).isTrue() // online is updated immediately
            }
        }

    @Test
    fun `test that lastTransfersCancelled ui state is updated to true when monitorLastTransfersHaveBeenCancelledUseCase emits`() =
        runTest {
            assertThat(underTest.state.value.lastTransfersCancelled).isFalse()
            monitorLastTransfersHaveBeenCancelledUseCaseFlow.emit(Unit)
            assertThat(underTest.state.value.lastTransfersCancelled).isTrue()
        }

    @Test
    fun `test that monitorTransferInErrorStatusUseCase updates isTransferError state when is changed to true`() =
        runTest {
            val mutableStateFlow = MutableStateFlow(false)
            whenever(monitorTransferInErrorStatusUseCase()) doReturn mutableStateFlow

            initTest()

            underTest.state.test {
                assertThat(awaitItem().isTransferError).isFalse()
                mutableStateFlow.value = true
                assertThat(awaitItem().isTransferError).isTrue()
            }
        }

    @Test
    fun `test that monitorTransferInErrorStatusUseCase updates isTransferError state and transfer info status when is changed to false and is completed`() =
        runTest {
            val mutableStateFlow = MutableStateFlow(true)
            whenever(monitorTransferInErrorStatusUseCase()) doReturn mutableStateFlow

            initTest()

            underTest.state.test {
                assertThat(awaitItem().isTransferError).isTrue()
                mutableStateFlow.value = false
                val actual = awaitItem()
                assertThat(actual.isTransferError).isFalse()
                assertThat(actual.transfersInfo.status).isEqualTo(TransfersStatus.Completed)
            }
        }

    @Test
    fun `test that monitorTransferInErrorStatusUseCase updates isTransferError state and transfer info status when is changed to false and is transferring`() =
        runTest {
            val mutableStateFlow = MutableStateFlow(true)
            whenever(monitorTransferInErrorStatusUseCase()) doReturn mutableStateFlow

            initTest()
            val transferInfoTransferring = TransfersInfo(
                totalSizeToTransfer = 1545L
            )
            whenever(
                transfersInfoMapper(
                    numPendingUploads = any(),
                    numPendingDownloadsNonBackground = any(),
                    totalSizeToTransfer = any(),
                    totalSizeTransferred = any(),
                    areTransfersPaused = any(),
                    isTransferError = any(),
                    isOnline = any(),
                    isTransferOverQuota = any(),
                    isStorageOverQuota = any(),
                    lastTransfersCancelled = any(),
                )
            ) doReturn transferInfoTransferring

            //emit a new value to update the ui state to transferring
            monitorTransfersStatusFlow.emit(mock())

            underTest.state.test {
                assertThat(awaitItem().isTransferError).isTrue()
                mutableStateFlow.value = false
                val actual = awaitItem()
                assertThat(actual.isTransferError).isFalse()
                assertThat(actual.transfersInfo.status).isEqualTo(TransfersStatus.Transferring)
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that monitorTransferInErrorStatusUseCase does not update the state if it is already updated`(
        isTransferError: Boolean,
    ) =
        runTest {
            val mutableStateFlow = MutableStateFlow(isTransferError)
            whenever(monitorTransferInErrorStatusUseCase()) doReturn mutableStateFlow

            initTest()

            underTest.state.test {
                assertThat(awaitItem().isTransferError).isEqualTo(isTransferError)
                mutableStateFlow.value = isTransferError
                expectNoEvents()
            }
        }

    private fun commonStub() {
        monitorConnectivityUseCaseFlow = MutableStateFlow(true)
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
                isOnline = any(),
                isTransferOverQuota = any(),
                isStorageOverQuota = any(),
                lastTransfersCancelled = any(),
            )
        ) doReturn TransfersInfo()
        whenever(monitorTransferInErrorStatusUseCase()) doReturn emptyFlow()
    }
}