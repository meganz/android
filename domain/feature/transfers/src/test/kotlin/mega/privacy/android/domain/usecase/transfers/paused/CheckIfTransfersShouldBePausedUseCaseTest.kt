package mega.privacy.android.domain.usecase.transfers.paused

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckIfTransfersShouldBePausedUseCaseTest {

    private lateinit var underTest: CheckIfTransfersShouldBePausedUseCase

    private val areTransfersPausedUseCase = mock<AreTransfersPausedUseCase>()
    private val pauseTransfersQueueUseCase = mock<PauseTransfersQueueUseCase>()

    @BeforeAll
    fun setup() {
        underTest = CheckIfTransfersShouldBePausedUseCase(
            areTransfersPausedUseCase,
            pauseTransfersQueueUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            areTransfersPausedUseCase,
            pauseTransfersQueueUseCase,
        )
    }

    @Test
    fun `test that pauseTransfersQueueUseCase is invoked if areTransfersPausedUseCase returns true`() =
        runTest {
            whenever(areTransfersPausedUseCase()).thenReturn(true)
            whenever(pauseTransfersQueueUseCase(true)).thenReturn(true)

            underTest.invoke()

            verify(areTransfersPausedUseCase).invoke()
            verify(pauseTransfersQueueUseCase).invoke(true)
            verifyNoMoreInteractions(areTransfersPausedUseCase)
            verifyNoMoreInteractions(pauseTransfersQueueUseCase)
        }

    @Test
    fun `test that pauseTransfersQueueUseCase is not invoked if areTransfersPausedUseCase returns false`() =
        runTest {
            whenever(areTransfersPausedUseCase()).thenReturn(false)

            underTest.invoke()


            verify(areTransfersPausedUseCase).invoke()
            verifyNoMoreInteractions(areTransfersPausedUseCase)
            verifyNoInteractions(pauseTransfersQueueUseCase)
        }
}