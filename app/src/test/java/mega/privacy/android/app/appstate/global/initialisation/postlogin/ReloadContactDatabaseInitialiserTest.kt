package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.contact.ReloadContactDatabase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReloadContactDatabaseInitialiserTest {

    private lateinit var underTest: ReloadContactDatabaseInitialiser
    private val reloadContactDatabase = mock<ReloadContactDatabase>()
    private val monitorFetchNodesFinishUseCase = mock<MonitorFetchNodesFinishUseCase>()

    @BeforeEach
    fun setUp() {
        reset(reloadContactDatabase, monitorFetchNodesFinishUseCase)
        // Default: fetch nodes is already finished
        whenever(monitorFetchNodesFinishUseCase()).thenReturn(flowOf(true))
        underTest = ReloadContactDatabaseInitialiser(
            reloadContactDatabase = reloadContactDatabase,
            monitorFetchNodesFinishUseCase = monitorFetchNodesFinishUseCase
        )
    }

    @Test
    fun `test that reloadContactDatabase is called with false for fast login after fetch nodes finishes`() =
        runTest {
            whenever(monitorFetchNodesFinishUseCase()).thenReturn(flowOf(true))
            whenever(reloadContactDatabase(false)).thenReturn(Unit)

            underTest("test-session", true)

            verify(monitorFetchNodesFinishUseCase).invoke()
            verify(reloadContactDatabase).invoke(false)
        }

    @Test
    fun `test that reloadContactDatabase is called with true for normal login after fetch nodes finishes`() =
        runTest {
            whenever(monitorFetchNodesFinishUseCase()).thenReturn(flowOf(true))
            whenever(reloadContactDatabase(true)).thenReturn(Unit)

            underTest("test-session", false)

            verify(monitorFetchNodesFinishUseCase).invoke()
            verify(reloadContactDatabase).invoke(true)
        }

    @Test
    fun `test that exception is handled gracefully when reloadContactDatabase throws for fast login`() =
        runTest {
            whenever(monitorFetchNodesFinishUseCase()).thenReturn(flowOf(true))
            whenever(reloadContactDatabase(false)).thenThrow(RuntimeException("Test error"))

            // Should not throw exception
            underTest("test-session", true)

            verify(monitorFetchNodesFinishUseCase).invoke()
            verify(reloadContactDatabase).invoke(false)
        }

    @Test
    fun `test that exception is handled gracefully when reloadContactDatabase throws for normal login`() =
        runTest {
            whenever(monitorFetchNodesFinishUseCase()).thenReturn(flowOf(true))
            whenever(reloadContactDatabase(true)).thenThrow(RuntimeException("Test error"))

            // Should not throw exception
            underTest("test-session", false)

            verify(monitorFetchNodesFinishUseCase).invoke()
            verify(reloadContactDatabase).invoke(true)
        }

    @Test
    fun `test that reloadContactDatabase waits for fetch nodes to finish`() = runTest {
        // Simulate fetch nodes not finished yet, then finishing
        whenever(monitorFetchNodesFinishUseCase()).thenReturn(flowOf(false, true))
        whenever(reloadContactDatabase(true)).thenReturn(Unit)

        underTest("test-session", false)

        verify(monitorFetchNodesFinishUseCase).invoke()
        // Should only be called when isFinish is true
        verify(reloadContactDatabase).invoke(true)
    }

    @Test
    fun `test that reloadContactDatabase does not execute when isFinish is false`() = runTest {
        // Simulate fetch nodes not finished (flow completes after false)
        // Note: In real scenario, the flow would keep emitting, but for testing we use a completing flow
        whenever(monitorFetchNodesFinishUseCase()).thenReturn(flowOf(false))
        whenever(reloadContactDatabase(true)).thenReturn(Unit)

        underTest("test-session", false)

        verify(monitorFetchNodesFinishUseCase).invoke()
        // Should not be called when isFinish is false
        verify(reloadContactDatabase, never()).invoke(any())
    }
}

