package test.mega.privacy.android.app.presentation.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.domain.usecase.MonitorGlobalUpdates
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.getOrAwaitValue
import java.util.concurrent.TimeoutException
import kotlin.test.assertContains


@ExperimentalCoroutinesApi
class ManagerViewModelTest {
    private lateinit var underTest: ManagerViewModel

    private val monitorGlobalUpdates = mock<MonitorGlobalUpdates>()
    private val monitorNodeUpdates = mock<MonitorNodeUpdates>()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    /**
     * Simulate a repository emission and setup the [ManagerViewModel]
     *
     * @param updates the values to emit from the repository
     * @param after a lambda function to call after setting up the viewModel
     */
    @Suppress("DEPRECATION")
    private fun triggerRepositoryUpdate(updates: List<GlobalUpdate>, after: () -> Unit) {
        whenever(monitorGlobalUpdates()).thenReturn(
            flow {
                updates.forEach { emit(it) }
            }
        )
        underTest = ManagerViewModel(monitorNodeUpdates, monitorGlobalUpdates)
        after()
    }

    @Test
    fun `test that live data are not set when no updates are triggered from api`() = runTest {
        underTest = ManagerViewModel(
            monitorNodeUpdates,
            monitorGlobalUpdates
        )

        val updateUserException = assertThrows(TimeoutException::class.java) {
            underTest.updateUsers.getOrAwaitValue { advanceUntilIdle() }
        }
        assertContains(updateUserException.message ?: "", "LiveData value was never set.", true)

        val updateUserAlertsException = assertThrows(TimeoutException::class.java) {
            underTest.updateUserAlerts.getOrAwaitValue { advanceUntilIdle() }
        }
        assertContains(updateUserAlertsException.message ?: "", "LiveData value was never set.", true)

        val updateNodesException = assertThrows(TimeoutException::class.java) {
            underTest.updateNodes.getOrAwaitValue { advanceUntilIdle() }
        }
        assertContains(updateNodesException.message ?: "", "LiveData value was never set.", true)

        val updateContactsRequestsException = assertThrows(TimeoutException::class.java) {
            underTest.updateContactsRequests.getOrAwaitValue { advanceUntilIdle() }
        }
        assertContains(updateContactsRequestsException.message ?: "", "LiveData value was never set.", true)

    }

    @Test
    fun `test that live data value is set when node updates is triggered from api`() = runTest {
        whenever(monitorNodeUpdates()).thenReturn(flowOf(listOf(mock())))
        underTest = ManagerViewModel(monitorNodeUpdates, monitorGlobalUpdates)
        val updateNodes = underTest.updateNodes.getOrAwaitValue { advanceUntilIdle() }
        assertEquals(updateNodes.size, 1)
    }

    @Test
    fun `test that live data value is not set when node updates is triggered from api with an empty list`() = runTest {
        whenever(monitorNodeUpdates()).thenReturn(flowOf(emptyList()))

        underTest = ManagerViewModel(monitorNodeUpdates, monitorGlobalUpdates)
        val updateNodesException = assertThrows(TimeoutException::class.java) {
            underTest.updateNodes.getOrAwaitValue { advanceUntilIdle() }
        }
        assertThat(updateNodesException.message?.contains("LiveData value was never set."))
    }

    @Test
    fun `test that live data is dispatched when updates triggered from api`() = runTest {
        triggerRepositoryUpdate(
            listOf(GlobalUpdate.OnUsersUpdate(arrayListOf(mock())))
        ) {
            val updateUsers = underTest.updateUsers.getOrAwaitValue { advanceUntilIdle() }
            assertEquals(updateUsers.size, 1)
        }
        triggerRepositoryUpdate(
            listOf(GlobalUpdate.OnUserAlertsUpdate(arrayListOf(mock())))
        ) {
            val updateUserAlerts = underTest.updateUserAlerts.getOrAwaitValue { advanceUntilIdle() }
            assertEquals(updateUserAlerts.size, 1)
        }
        triggerRepositoryUpdate(
            listOf(GlobalUpdate.OnContactRequestsUpdate(arrayListOf(mock())))
        ) {
            val updateContactRequests = underTest.updateContactsRequests.getOrAwaitValue { advanceUntilIdle() }
            assertEquals(updateContactRequests.size, 1)
        }
    }

    @Test
    fun `test that an update is not dispatched when an update is triggered from api with null or empty list`() = runTest {
        triggerRepositoryUpdate(
            listOf(
                GlobalUpdate.OnUsersUpdate(null),
                GlobalUpdate.OnUsersUpdate(arrayListOf()),
            )
        ) {
            val updateUserException = assertThrows(TimeoutException::class.java) {
                underTest.updateUsers.getOrAwaitValue { advanceUntilIdle() }
            }
            assertContains(updateUserException.message ?: "", "LiveData value was never set.", true)
        }
        triggerRepositoryUpdate(
            listOf(
                GlobalUpdate.OnUserAlertsUpdate(null),
                GlobalUpdate.OnUserAlertsUpdate(arrayListOf()),
            )
        ) {
            val updateUserAlerts = assertThrows(TimeoutException::class.java) {
                underTest.updateUserAlerts.getOrAwaitValue { advanceUntilIdle() }
            }
            assertContains(updateUserAlerts.message ?: "", "LiveData value was never set.", true)
        }
        triggerRepositoryUpdate(
            listOf(
                GlobalUpdate.OnContactRequestsUpdate(null),
                GlobalUpdate.OnContactRequestsUpdate(arrayListOf()),
            )
        ) {
            val updateContactsRequestsException = assertThrows(TimeoutException::class.java) {
                underTest.updateContactsRequests.getOrAwaitValue { advanceUntilIdle() }
            }
            assertContains(updateContactsRequestsException.message ?: "", "LiveData value was never set.", true)
        }
    }
}
