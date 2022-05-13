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
import nz.mega.sdk.MegaEvent
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

        val reloadException = assertThrows(TimeoutException::class.java) {
            underTest.needReload.getOrAwaitValue { advanceUntilIdle() }
        }
        assertThat(reloadException.message?.contains("LiveData value was never set."))

        val updateAccountException = assertThrows(TimeoutException::class.java) {
            underTest.updateAccount.getOrAwaitValue { advanceUntilIdle() }
        }
        assertThat(updateAccountException.message?.contains("LiveData value was never set."))

        val updateContactsRequestsException = assertThrows(TimeoutException::class.java) {
            underTest.updateContactsRequests.getOrAwaitValue { advanceUntilIdle() }
        }
        assertThat(updateContactsRequestsException.message?.contains("LiveData value was never set."))

        val eventException = assertThrows(TimeoutException::class.java) {
            underTest.onEvent.getOrAwaitValue { advanceUntilIdle() }
        }
        assertThat(eventException.message?.contains("LiveData value was never set."))
    }

    @Test
    fun `test that live data is dispatched when updates triggered from api`() = runTest {
        triggerRepositoryUpdate(
            listOf(GlobalUpdate.OnUsersUpdate(arrayListOf(mock())))) {
            val updateUsers = underTest.updateUsers.getOrAwaitValue { advanceUntilIdle() }
            assertEquals(updateUsers.size, 1)
        }
        triggerRepositoryUpdate(
            listOf(GlobalUpdate.OnUserAlertsUpdate(arrayListOf(mock())))) {
            val updateUserAlerts = underTest.updateUserAlerts.getOrAwaitValue { advanceUntilIdle() }
            assertEquals(updateUserAlerts.size, 1)
        }
        triggerRepositoryUpdate(
            listOf(GlobalUpdate.OnNodesUpdate(arrayListOf(mock())))) {
            val updateNodes = underTest.updateNodes.getOrAwaitValue { advanceUntilIdle() }
            assertEquals(updateNodes.size, 1)
        }
        triggerRepositoryUpdate(
            listOf(GlobalUpdate.OnReloadNeeded)) {
            val needReload = underTest.needReload.getOrAwaitValue { advanceUntilIdle() }
            assertEquals(needReload, Unit)

        }
        triggerRepositoryUpdate(
            listOf(GlobalUpdate.OnAccountUpdate)) {
            val updateAccount = underTest.updateAccount.getOrAwaitValue { advanceUntilIdle() }
            assertEquals(updateAccount, Unit)
        }
        triggerRepositoryUpdate(
            listOf(GlobalUpdate.OnContactRequestsUpdate(arrayListOf(mock())))) {
            val updateContactRequests = underTest.updateContactsRequests.getOrAwaitValue { advanceUntilIdle() }
            assertEquals(updateContactRequests.size, 1)
        }
        triggerRepositoryUpdate(
            listOf(GlobalUpdate.OnEvent(mock()))) {
            val event = underTest.onEvent.getOrAwaitValue { advanceUntilIdle() }
            assertThat(event).isInstanceOf(MegaEvent::class.java)
        }
    }

    @Test
    fun `test that an update is not dispatched when an update is triggered from api with null or empty list`() = runTest {
        triggerRepositoryUpdate(
            listOf(
                GlobalUpdate.OnUsersUpdate(null),
                GlobalUpdate.OnUsersUpdate(arrayListOf()),
            )) {
            val updateUserException = assertThrows(TimeoutException::class.java) {
                underTest.updateUsers.getOrAwaitValue { advanceUntilIdle() }
            }
            assertThat(updateUserException.message?.contains("LiveData value was never set."))
        }
        triggerRepositoryUpdate(
            listOf(
                GlobalUpdate.OnUserAlertsUpdate(null),
                GlobalUpdate.OnUserAlertsUpdate(arrayListOf()),
            )) {
            val updateUserAlerts = assertThrows(TimeoutException::class.java) {
                underTest.updateUserAlerts.getOrAwaitValue { advanceUntilIdle() }
            }
            assertThat(updateUserAlerts.message?.contains("LiveData value was never set."))
        }
        triggerRepositoryUpdate(
            listOf(
                GlobalUpdate.OnNodesUpdate(null),
                GlobalUpdate.OnNodesUpdate(arrayListOf()),
            )) {
            val updateNodesException = assertThrows(TimeoutException::class.java) {
                underTest.updateNodes.getOrAwaitValue { advanceUntilIdle() }
            }
            assertThat(updateNodesException.message?.contains("LiveData value was never set."))
        }
        triggerRepositoryUpdate(
            listOf(
                GlobalUpdate.OnContactRequestsUpdate(null),
                GlobalUpdate.OnContactRequestsUpdate(arrayListOf()),
            )) {
            val updateContactsRequestsException = assertThrows(TimeoutException::class.java) {
                underTest.updateContactsRequests.getOrAwaitValue { advanceUntilIdle() }
            }
            assertThat(updateContactsRequestsException.message?.contains("LiveData value was never set."))
        }
        triggerRepositoryUpdate(
            listOf(GlobalUpdate.OnEvent(null))) {
            val eventException = assertThrows(TimeoutException::class.java) {
                underTest.updateContactsRequests.getOrAwaitValue { advanceUntilIdle() }
            }
            assertThat(eventException.message?.contains("LiveData value was never set."))
        }
    }

    private fun triggerRepositoryUpdate(updates: List<GlobalUpdate>, after: () -> Unit) {
        whenever(globalUpdateRepository.monitorGlobalUpdates()).thenReturn(
            flow {
                updates.forEach { emit(it) }
            }
        )
        underTest = ManagerViewModel(globalUpdateRepository)
        after()
    }

}
