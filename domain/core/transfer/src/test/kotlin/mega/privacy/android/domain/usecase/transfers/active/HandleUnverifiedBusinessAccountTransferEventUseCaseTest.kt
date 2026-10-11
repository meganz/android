package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.BusinessAccountUnverifiedException
import mega.privacy.android.domain.usecase.account.SetUnverifiedBusinessAccountUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleUnverifiedBusinessAccountTransferEventUseCaseTest {
    private lateinit var underTest: HandleUnverifiedBusinessAccountTransferEventUseCase

    private val setUnverifiedBusinessAccountUseCase = mock<SetUnverifiedBusinessAccountUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = HandleUnverifiedBusinessAccountTransferEventUseCase(
            setUnverifiedBusinessAccountUseCase = setUnverifiedBusinessAccountUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(setUnverifiedBusinessAccountUseCase)
    }

    @Test
    fun `test that set unverified business account use case is invoked when a finish transfer event with business account unverified error is received`() =
        runTest {
            val transfer = mock<Transfer>()
            val finishEvent = TransferEvent.TransferFinishEvent(
                transfer = transfer,
                error = BusinessAccountUnverifiedException(1)
            )

            underTest(finishEvent)

            verify(setUnverifiedBusinessAccountUseCase).invoke(true)
        }

    @Test
    fun `test that set unverified business account use case is not invoked when a finish transfer event without business account unverified error is received`() =
        runTest {
            val transfer = mock<Transfer>()
            val finishEvent = TransferEvent.TransferFinishEvent(
                transfer = transfer,
                error = null
            )

            underTest(finishEvent)

            verifyNoInteractions(setUnverifiedBusinessAccountUseCase)
        }

    @Test
    fun `test that set unverified business account use case is not invoked when a transfer update event is received`() =
        runTest {
            val transfer = mock<Transfer>()
            val updateEvent = TransferEvent.TransferUpdateEvent(transfer)

            underTest(updateEvent)

            verifyNoInteractions(setUnverifiedBusinessAccountUseCase)
        }

    @Test
    fun `test that set unverified business account use case is invoked when multiple finish transfer events with business account unverified error are received`() =
        runTest {
            val transfer = mock<Transfer>()
            val finishEvent = TransferEvent.TransferFinishEvent(
                transfer = transfer,
                error = BusinessAccountUnverifiedException(1)
            )

            underTest(finishEvent, finishEvent)

            verify(setUnverifiedBusinessAccountUseCase).invoke(true)
        }
}