package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.transfers.active.UpdateActiveTransfersUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateActiveTransfersInitializerTest {

    private lateinit var underTest: UpdateActiveTransfersInitializer

    private val updateActiveTransfersUseCase = mock<UpdateActiveTransfersUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateActiveTransfersInitializer(
            updateActiveTransfersUseCase = updateActiveTransfersUseCase
        )
    }

    @AfterEach
    fun resetMock() {
        reset(updateActiveTransfersUseCase)
    }

    @Test
    fun `test that update active transfers use case is invoked and returns early when true`() =
        runTest {
            whenever(updateActiveTransfersUseCase()).thenReturn(true)

            underTest("session", false)

            verify(updateActiveTransfersUseCase).invoke()
        }

    @Test
    fun `test that use case is called multiple times until it returns true`() = runTest {
        whenever(updateActiveTransfersUseCase())
            .thenReturn(false)
            .thenReturn(false)
            .thenReturn(true)

        launch { underTest("session", false) }
        advanceUntilIdle()

        verify(updateActiveTransfersUseCase, times(3)).invoke()
    }

    @Test
    fun `test that exception is caught and does not throw`() = runTest {
        whenever(updateActiveTransfersUseCase()).thenThrow(RuntimeException("Test exception"))

        underTest("session", false)

        verify(updateActiveTransfersUseCase).invoke()
    }
}
