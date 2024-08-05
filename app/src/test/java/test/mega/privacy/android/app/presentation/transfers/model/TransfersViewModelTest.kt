package test.mega.privacy.android.app.presentation.transfers.model

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.transfers.model.TransfersViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorInProgressTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransferByTagUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
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

    @BeforeEach
    fun resetMocks() {
        reset(pauseTransferByTagUseCase)
        wheneverBlocking { monitorInProgressTransfersUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorStorageStateEventUseCase() } doReturn MutableStateFlow(
            StorageStateEvent(1L, "", 0L, "", EventType.Unknown, StorageState.Unknown)
        )
        wheneverBlocking { monitorTransferOverQuotaUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorPausedTransfersUseCase() }.thenReturn(emptyFlow())
    }

    private fun initTestClass() {
        underTest = TransfersViewModel(
            monitorInProgressTransfersUseCase = monitorInProgressTransfersUseCase,
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            monitorTransferOverQuotaUseCase = monitorTransferOverQuotaUseCase,
            monitorPausedTransfersUseCase = monitorPausedTransfersUseCase,
            pauseTransferByTagUseCase = pauseTransferByTagUseCase,
        )
    }

    @Test
    fun `test that MonitorInProgressTransfersUseCase updates state with in progress transfers`() =
        runTest {
            val flow = MutableSharedFlow<Map<Int, InProgressTransfer>>()
            val transfer1 = mock<InProgressTransfer.Upload> {
                on { priority } doReturn BigInteger.ONE
            }
            val transfer2 = mock<InProgressTransfer.Download> {
                on { priority } doReturn BigInteger.TWO
            }
            val map = mapOf(1 to transfer1, 2 to transfer2)

            whenever(monitorInProgressTransfersUseCase()).thenReturn(flow)

            initTestClass()

            underTest.uiState.map { it.inProgressTransfers }.test {
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
        val flow = MutableSharedFlow<Map<Int, InProgressTransfer>>()
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
        val map = mapOf(1 to transfer1, 2 to transfer2)

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

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}