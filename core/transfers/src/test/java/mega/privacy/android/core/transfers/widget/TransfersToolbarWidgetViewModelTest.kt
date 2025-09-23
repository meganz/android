package mega.privacy.android.core.transfers.widget

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidgetViewModel.Companion.waitTimeToShowOffline
import mega.privacy.android.domain.entity.TransfersStatusInfo
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorLastTransfersHaveBeenCancelledUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersStatusUseCase
import mega.privacy.android.domain.usecase.transfers.errorstatus.MonitorTransferInErrorStatusUseCase
import mega.privacy.android.feature.transfers.components.widget.TransfersToolbarWidgetStatus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever


@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransfersToolbarWidgetViewModelTest {
    private lateinit var underTest: TransfersToolbarWidgetViewModel

    private val monitorLastTransfersHaveBeenCancelledUseCase =
        mock<MonitorLastTransfersHaveBeenCancelledUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()

    private val monitorTransfersStatusFlow = MutableSharedFlow<TransfersStatusInfo>()
    private var monitorConnectivityUseCaseFlow = MutableStateFlow(false)
    private val monitorLastTransfersHaveBeenCancelledUseCaseFlow = MutableSharedFlow<Unit>()
    private val monitorTransferInErrorStatusUseCase = mock<MonitorTransferInErrorStatusUseCase>()
    private val monitorUserCredentialsUseCase = mock<MonitorUserCredentialsUseCase>()
    private val monitorUserCredentialsUseCaseFlow = MutableStateFlow<UserCredentials?>(mock())

    @BeforeAll
    fun setup() {
        commonStub()
        initTest()
    }

    private fun initTest() {
        //this mocks are only used in viewmodel init, so no need to reset
        val monitorTransfersStatusUseCase = mock<MonitorTransfersStatusUseCase>()
        whenever(monitorTransfersStatusUseCase()) doReturn monitorTransfersStatusFlow
        underTest = TransfersToolbarWidgetViewModel(
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            monitorTransfersStatusUseCase = monitorTransfersStatusUseCase,
            monitorLastTransfersHaveBeenCancelledUseCase = monitorLastTransfersHaveBeenCancelledUseCase,
            monitorTransferInErrorStatusUseCase = monitorTransferInErrorStatusUseCase,
            samplePeriod = 0L,
            monitorUserCredentialsUseCase = monitorUserCredentialsUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorConnectivityUseCase,
            monitorLastTransfersHaveBeenCancelledUseCase,
            monitorUserCredentialsUseCase,
        )
        monitorUserCredentialsUseCaseFlow.value = mock()
        commonStub()
    }

    @Test
    fun `test that initial status is null`() = runTest {
        assertThat(underTest.state.value.status).isNull()
    }

    @Test
    fun `test ui state is updated with correct values when there's a new emission of monitorTransfersStatusFlow`() =
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
            val expected = TransfersToolabarWidgetUiState(
                status = TransfersToolbarWidgetStatus.Transferring,
                totalSizeAlreadyTransferred = totalSizeTransferred,
                totalSizeToTransfer = totalSizeToTransfer,
                isOnline = true,
                isUserLoggedIn = true,
            )

            initTest()

            underTest.state.test {
                awaitItem() // Skip initial value
                monitorTransfersStatusFlow.emit(transfersStatusInfo)
                val actual = awaitItem()
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
                assertThat(actual.status).isEqualTo(TransfersToolbarWidgetStatus.Completed)
            }
        }

    @Test
    fun `test that monitorTransferInErrorStatusUseCase updates isTransferError state and transfer info status when is changed to false and is transferring`() =
        runTest {
            val mutableStateFlow = MutableStateFlow(true)
            whenever(monitorTransferInErrorStatusUseCase()) doReturn mutableStateFlow

            initTest()

            //emit a new value to update the ui state to transferring
            monitorTransfersStatusFlow.emit(
                TransfersStatusInfo(
                    totalSizeToTransfer = 10L,
                    totalSizeTransferred = 5L,
                    pendingUploads = 1,
                    pendingDownloads = 1,
                )
            )

            underTest.state.test {
                assertThat(awaitItem().isTransferError).isTrue()
                mutableStateFlow.value = false
                val actual = awaitItem()
                assertThat(actual.isTransferError).isFalse()
                assertThat(actual.status).isEqualTo(TransfersToolbarWidgetStatus.Transferring)
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that monitorTransferInErrorStatusUseCase does not update the state if it is already updated`(
        isTransferError: Boolean,
    ) = runTest {
        val mutableStateFlow = MutableStateFlow(isTransferError)
        whenever(monitorTransferInErrorStatusUseCase()) doReturn mutableStateFlow

        initTest()

        underTest.state.test {
            assertThat(awaitItem().isTransferError).isEqualTo(isTransferError)
            mutableStateFlow.value = isTransferError
            expectNoEvents()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that isUserLoggedIn is updated when monitorUserCredentialsUseCase emits a new value`(
        isLoggedIn: Boolean,
    ) = runTest {
        monitorUserCredentialsUseCaseFlow.emit(if (isLoggedIn) mock() else null)
        advanceUntilIdle()
        assertThat(underTest.state.value.isUserLoggedIn).isEqualTo(isLoggedIn)
    }

    private fun commonStub() {
        monitorConnectivityUseCaseFlow = MutableStateFlow(true)
        whenever(monitorConnectivityUseCase()) doReturn monitorConnectivityUseCaseFlow
        whenever(monitorLastTransfersHaveBeenCancelledUseCase()) doReturn monitorLastTransfersHaveBeenCancelledUseCaseFlow
        whenever(monitorUserCredentialsUseCase()) doReturn monitorUserCredentialsUseCaseFlow
        whenever(monitorTransferInErrorStatusUseCase()) doReturn emptyFlow()
    }
}