package test.mega.privacy.android.app.presentation.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jraska.livedata.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.domain.usecase.GetBrowserNodeByHandle
import mega.privacy.android.app.domain.usecase.GetRubbishBinNodeByHandle
import mega.privacy.android.app.domain.usecase.MonitorGlobalUpdates
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit


@ExperimentalCoroutinesApi
class ManagerViewModelTest {
    private lateinit var underTest: ManagerViewModel

    private val monitorGlobalUpdates = mock<MonitorGlobalUpdates>()
    private val monitorNodeUpdates = mock<MonitorNodeUpdates>()
    private val getRubbishBinNodeByHandle = mock<GetRubbishBinNodeByHandle>()
    private val getBrowserNodeByHandle = mock<GetBrowserNodeByHandle>()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    /**
     * Initialize the view model under test
     */
    private fun setUnderTest() {
        underTest = ManagerViewModel(
            monitorNodeUpdates,
            monitorGlobalUpdates,
            getRubbishBinNodeByHandle,
            getBrowserNodeByHandle
        )
    }

    /**
     * Simulate a repository emission and setup the [ManagerViewModel]
     *
     * @param updates the values to emit from the repository
     * @param after a lambda function to call after setting up the viewModel
     */
    @Suppress("DEPRECATION")
    private fun triggerRepositoryUpdate(updates: List<GlobalUpdate>, after: () -> Unit) {
        whenever(monitorGlobalUpdates()).thenReturn(updates.asFlow())
        setUnderTest()
        after()
    }

    @Test
    fun `test that user updates live data is not set when no updates triggered from use case`() = runTest {
        setUnderTest()

        underTest.updateUsers.test().assertNoValue()
    }

    @Test
    fun `test that user alert updates live data is not set when no updates triggered from use case`() = runTest {
        setUnderTest()

        underTest.updateUserAlerts.test().assertNoValue()
    }

    @Test
    fun `test that node updates live data is not set when no updates triggered from use case`() = runTest {
        setUnderTest()

        underTest.updateNodes.test().assertNoValue()
    }

    @Test
    fun `test that contact request updates live data is not set when no updates triggered from use case`() = runTest {
        underTest = ManagerViewModel(monitorNodeUpdates, monitorGlobalUpdates, getRubbishBinNodeByHandle, getBrowserNodeByHandle)

        underTest.updateContactsRequests.test().assertNoValue()
    }

    @Test
    fun `test that node updates live data is set when node updates triggered from use case`() = runTest {
        whenever(monitorNodeUpdates()).thenReturn(flowOf(listOf(mock())))

        setUnderTest()

        runCatching {
            underTest.updateNodes.test().awaitValue(50, TimeUnit.MILLISECONDS)
        }.onSuccess { result ->
            result.assertValue { it.size == 1 }
        }
    }

    @Test
    fun `test that node updates live data is not set when node updates triggered from use case with an empty list`() = runTest {
        whenever(monitorNodeUpdates()).thenReturn(flowOf(emptyList()))

        setUnderTest()

        underTest.updateNodes.test().assertNoValue()
    }

    @Test
    fun `test that user updates live data is set when user updates triggered from use case`() = runTest {
        triggerRepositoryUpdate(listOf(GlobalUpdate.OnUsersUpdate(arrayListOf(mock())))) {

            runCatching {
                underTest.updateUsers.test().awaitValue(50, TimeUnit.MILLISECONDS)
            }.onSuccess { result ->
                result.assertValue { it.size == 1 }
            }
        }
    }

    @Test
    fun `test that user updates live data is not set when user updates triggered from use case with null or empty list`() = runTest {
        triggerRepositoryUpdate(
            listOf(
                GlobalUpdate.OnUsersUpdate(null),
                GlobalUpdate.OnUsersUpdate(arrayListOf()),
            )
        ) {
            underTest.updateUsers.test().assertNoValue()
        }
    }

    @Test
    fun `test that user alert updates live data is set when user alert updates triggered from use case`() = runTest {
        triggerRepositoryUpdate(listOf(GlobalUpdate.OnUserAlertsUpdate(arrayListOf(mock())))) {

            runCatching {
                underTest.updateUserAlerts.test().awaitValue(50, TimeUnit.MILLISECONDS)
            }.onSuccess { result ->
                result.assertValue { it.size == 1 }
            }
        }
    }

    @Test
    fun `test that user alert updates live data is not set when user alert updates triggered from use case with null or empty list`() = runTest {
        triggerRepositoryUpdate(
            listOf(
                GlobalUpdate.OnUserAlertsUpdate(null),
                GlobalUpdate.OnUserAlertsUpdate(arrayListOf()),
            )
        ) {
            underTest.updateUserAlerts.test().assertNoValue()
        }
    }

    @Test
    fun `test that contact request updates live data is set when contact request updates triggered from use case`() = runTest {
        triggerRepositoryUpdate(listOf(GlobalUpdate.OnContactRequestsUpdate(arrayListOf(mock())))) {

            runCatching {
                underTest.updateContactsRequests.test().awaitValue(50, TimeUnit.MILLISECONDS)
            }.onSuccess { result ->
                result.assertValue { it.size == 1 }
            }
        }
    }

    @Test
    fun `test that contact request updates live data is not set when contact request updates triggered from use case with null or empty list`() = runTest {
        triggerRepositoryUpdate(
            listOf(
                GlobalUpdate.OnContactRequestsUpdate(null),
                GlobalUpdate.OnContactRequestsUpdate(arrayListOf()),
            )
        ) {
            underTest.updateContactsRequests.test().assertNoValue()
        }
    }
}
