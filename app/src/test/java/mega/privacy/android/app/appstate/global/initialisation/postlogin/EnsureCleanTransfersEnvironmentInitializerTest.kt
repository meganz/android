package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.transfers.completed.DeleteAllCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.errorstatus.ClearTransferErrorStatusUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnsureCleanTransfersEnvironmentInitializerTest {

    private lateinit var underTest: EnsureCleanTransfersEnvironmentInitializer

    private val deleteAllCompletedTransfersUseCase = mock<DeleteAllCompletedTransfersUseCase>()
    private val clearTransferErrorStatusUseCase = mock<ClearTransferErrorStatusUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = EnsureCleanTransfersEnvironmentInitializer(
            deleteAllCompletedTransfersUseCase = deleteAllCompletedTransfersUseCase,
            clearTransferErrorStatusUseCase = clearTransferErrorStatusUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(deleteAllCompletedTransfersUseCase, clearTransferErrorStatusUseCase)
    }

    @Test
    fun `test that a fresh login clears completed transfers and transfer error status`() = runTest {
        underTest(SESSION, isFastLogin = false)

        verify(deleteAllCompletedTransfersUseCase).invoke()
        verify(clearTransferErrorStatusUseCase).invoke()
    }

    @Test
    fun `test that a fast login does not clear completed transfers nor transfer error status`() =
        runTest {
            underTest(SESSION, isFastLogin = true)

            verifyNoInteractions(deleteAllCompletedTransfersUseCase)
            verifyNoInteractions(clearTransferErrorStatusUseCase)
        }

    @Test
    fun `test that an exception is caught when clearing fails`() = runTest {
        whenever(deleteAllCompletedTransfersUseCase()).thenThrow(RuntimeException("Test exception"))

        underTest(SESSION, isFastLogin = false)

        verify(deleteAllCompletedTransfersUseCase).invoke()
    }

    private companion object {
        const val SESSION = "session"
    }
}
