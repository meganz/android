package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.transfers.completed.DeleteOldestCompletedTransfersUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteOldestCompletedTransfersInitializerTest {
    private lateinit var underTest: DeleteOldestCompletedTransfersInitializer

    private val deleteOldestCompletedTransfersUseCase =
        mock<DeleteOldestCompletedTransfersUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = DeleteOldestCompletedTransfersInitializer(
            deleteOldestCompletedTransfersUseCase = deleteOldestCompletedTransfersUseCase
        )
    }

    @AfterEach
    fun resetMock() {
        reset(deleteOldestCompletedTransfersUseCase)
    }

    @Test
    fun `test that delete oldest completed transfers use case is invoked`() =
        runTest {
            underTest("session", false)
            verify(deleteOldestCompletedTransfersUseCase).invoke()
        }

    @Test
    fun `test that exception is caught when delete oldest completed transfers fails`() = runTest {
        whenever(deleteOldestCompletedTransfersUseCase()).thenThrow(RuntimeException("Test exception"))

        // Should not throw exception
        underTest("session", false)

        verify(deleteOldestCompletedTransfersUseCase).invoke()
    }
}
