package test.mega.privacy.android.app.presentation.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.domain.repository.GlobalUpdatesRepository
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.getOrAwaitValue
import java.util.concurrent.TimeoutException


@ExperimentalCoroutinesApi
class ManagerViewModelTest {
    private lateinit var underTest: ManagerViewModel

    private val globalUpdateRepository = mock<GlobalUpdatesRepository>()

    private val scheduler = TestCoroutineScheduler()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
    }

    /**
     * Simulate a repository emission and setup the [ManagerViewModel]
     *
     * @param updates the values to emit from the repository
     * @param after a lambda function to call after setting up the viewmodel
     */
    private fun triggerRepositoryUpdate(updates: List<GlobalUpdate>, after: () -> Unit) {
        whenever(globalUpdateRepository.monitorGlobalUpdates()).thenReturn(
            flow {
                updates.forEach { emit(it) }
            }
        )
        underTest = ManagerViewModel(globalUpdateRepository)
        after()
    }

    @Test
    fun `test that live data are not set when no updates are triggered from api`() = runTest {
        underTest = ManagerViewModel(globalUpdateRepository)

        val updateUserException = assertThrows(TimeoutException::class.java) {
            underTest.updateUsers.getOrAwaitValue { advanceUntilIdle() }
        }
        assertThat(updateUserException.message?.contains("LiveData value was never set."))

        val updateUserAlertsException = assertThrows(TimeoutException::class.java) {
            underTest.updateUserAlerts.getOrAwaitValue { advanceUntilIdle() }
        }
        assertThat(updateUserAlertsException.message?.contains("LiveData value was never set."))

        val updateNodesException = assertThrows(TimeoutException::class.java) {
            underTest.updateNodes.getOrAwaitValue { advanceUntilIdle() }
        }
        assertThat(updateNodesException.message?.contains("LiveData value was never set."))

        val updateContactsRequestsException = assertThrows(TimeoutException::class.java) {
            underTest.updateContactsRequests.getOrAwaitValue { advanceUntilIdle() }
        }
        assertThat(updateContactsRequestsException.message?.contains("LiveData value was never set."))

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
            listOf(GlobalUpdate.OnNodesUpdate(arrayListOf(mock())))
        ) {
            val updateNodes = underTest.updateNodes.getOrAwaitValue { advanceUntilIdle() }
            assertEquals(updateNodes.size, 1)
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
            assertThat(updateUserException.message?.contains("LiveData value was never set."))
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
            assertThat(updateUserAlerts.message?.contains("LiveData value was never set."))
        }
        triggerRepositoryUpdate(
            listOf(
                GlobalUpdate.OnNodesUpdate(null),
                GlobalUpdate.OnNodesUpdate(arrayListOf()),
            )
        ) {
            val updateNodesException = assertThrows(TimeoutException::class.java) {
                underTest.updateNodes.getOrAwaitValue { advanceUntilIdle() }
            }
            assertThat(updateNodesException.message?.contains("LiveData value was never set."))
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
            assertThat(updateContactsRequestsException.message?.contains("LiveData value was never set."))
        }
        triggerRepositoryUpdate(
            listOf(GlobalUpdate.OnEvent(null))
        ) {
            val eventException = assertThrows(TimeoutException::class.java) {
                underTest.updateContactsRequests.getOrAwaitValue { advanceUntilIdle() }
            }
            assertThat(eventException.message?.contains("LiveData value was never set."))
        }
    }
}
