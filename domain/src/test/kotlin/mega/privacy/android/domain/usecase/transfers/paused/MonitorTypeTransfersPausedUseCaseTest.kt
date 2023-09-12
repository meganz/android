package mega.privacy.android.domain.usecase.transfers.paused

import app.cash.turbine.test
import com.google.common.truth.Truth.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorTypeTransfersPausedUseCaseTest {
    private lateinit var underTest: TestableMonitorTypeTransfersPausedUseCase

    private val transferRepository = mock<TransferRepository>()
    private val transfer = mock<Transfer>()

    @BeforeAll
    fun setup() {
        underTest = TestableMonitorTypeTransfersPausedUseCase()
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository, transfer)
    }

    @ParameterizedTest(name = "Global transfers paused {0}")
    @ValueSource(booleans = [true, false])
    fun `test that emits correct value when monitorPausedTransfers emits and there are no paused transfers`(
        paused: Boolean,
    ) = runTest {
        val pausedFlow = MutableStateFlow(paused)
        whenever(transferRepository.monitorPausedTransfers()).thenReturn(pausedFlow)
        whenever(transferRepository.monitorTransferEvents()).thenReturn(emptyFlow())
        underTest.totalPendingIndividualTransfers = 3
        underTest.totalPausedIndividualTransfers = 0
        underTest().test {
            assertThat(awaitItem()).isEqualTo(paused)
        }
    }

    @ParameterizedTest(name = "All individual transfers paused: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that emits correct value when monitorPausedTransfers is false and there are pending transfers`(
        paused: Boolean,
    ) = runTest {
        val pausedFlow = MutableStateFlow(false)
        whenever(transferRepository.monitorPausedTransfers()).thenReturn(pausedFlow)
        whenever(transferRepository.monitorTransferEvents()).thenReturn(emptyFlow())
        underTest.totalPendingIndividualTransfers = 3
        underTest.totalPausedIndividualTransfers = if (paused) 3 else 0
        underTest().test {
            assertThat(awaitItem()).isEqualTo(paused)
        }
    }

    @Test
    fun `test that emits false value when monitorPausedTransfers is false and there are no pending transfers`() =
        runTest {
            val pausedFlow = MutableStateFlow(false)
            whenever(transferRepository.monitorPausedTransfers()).thenReturn(pausedFlow)
            whenever(transferRepository.monitorTransferEvents()).thenReturn(emptyFlow())
            underTest.totalPendingIndividualTransfers = 0
            underTest.totalPausedIndividualTransfers = 0
            underTest().test {
                assertThat(awaitItem()).isEqualTo(false)
            }
        }

    @ParameterizedTest(name = "event of correct type: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that emits false when monitorPausedTransfers is false and pause event of correct type is received`(
        correctType: Boolean,
    ) =
        runTest {
            val pausedFlow = MutableStateFlow(false)
            val eventsFlow = flowOf(
                TransferEvent.TransferPaused(stubTransfer(correctType), false)
            )
            whenever(transferRepository.monitorPausedTransfers()).thenReturn(pausedFlow)
            whenever(transferRepository.monitorTransferEvents()).thenReturn(eventsFlow)
            underTest.totalPendingIndividualTransfers = 3
            underTest.totalPausedIndividualTransfers = 3
            underTest().test {
                assertThat(awaitItem()).isEqualTo(!correctType) //if not the correct type will keep the initial true value (because all transfers were paused)
                expectNoEvents()
            }
        }

    @ParameterizedTest(name = "event of correct type: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that emits false when monitorPausedTransfers is false and resume event of correct type is received`(
        correctType: Boolean,
    ) =
        runTest {
            val pausedFlow = MutableStateFlow(false)
            val eventsFlow = flow {
                //simulate resume transfer and event
                underTest.totalPausedIndividualTransfers = 3
                emit(TransferEvent.TransferPaused(stubTransfer(correctType), true))
            }
            whenever(transferRepository.monitorPausedTransfers()).thenReturn(pausedFlow)
            whenever(transferRepository.monitorTransferEvents()).thenReturn(eventsFlow)
            underTest.totalPendingIndividualTransfers = 3
            underTest.totalPausedIndividualTransfers = 2
            underTest().test {
                assertThat(awaitItem()).isEqualTo(correctType) //if not the correct type will keep the initial false value (because not all transfers were paused)
                expectNoEvents()
            }
        }

    private fun stubTransfer(correctType: Boolean) = transfer.also {
        transfer.stub {
            on { it.isBackgroundTransfer() }.thenReturn(false)
            on { it.isVoiceClip() }.thenReturn(false)
            on { it.tag }.thenReturn(if (correctType) CORRECT_TRANSFER_TAG else -1)
        }
    }

    companion object {
        private const val CORRECT_TRANSFER_TAG = 1
    }

    inner class TestableMonitorTypeTransfersPausedUseCase :
        MonitorTypeTransfersPausedUseCase() {
        var totalPendingIndividualTransfers = 0
        var totalPausedIndividualTransfers = 0
        override val transferRepository =
            this@MonitorTypeTransfersPausedUseCaseTest.transferRepository

        override fun isCorrectType(transfer: Transfer) = transfer.tag == CORRECT_TRANSFER_TAG


        override suspend fun totalPendingIndividualTransfers() = totalPendingIndividualTransfers

        override suspend fun totalPausedIndividualTransfers() = totalPausedIndividualTransfers

    }
}