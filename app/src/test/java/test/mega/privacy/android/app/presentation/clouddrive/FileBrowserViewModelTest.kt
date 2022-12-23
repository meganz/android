package test.mega.privacy.android.app.presentation.clouddrive

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.jraska.livedata.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.presentation.clouddrive.FileBrowserViewModel
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import nz.mega.sdk.MegaApiJava
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class FileBrowserViewModelTest {
    private lateinit var underTest: FileBrowserViewModel

    private val getRootFolder = mock<GetRootFolder>()
    private val getBrowserChildrenNode = mock<GetBrowserChildrenNode>()
    private val monitorMediaDiscoveryView = mock<MonitorMediaDiscoveryView>()
    private val monitorNodeUpdates = FakeMonitorUpdates()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initViewModel()
    }

    private fun initViewModel() {
        underTest = FileBrowserViewModel(
            getRootFolder = getRootFolder,
            getBrowserChildrenNode = getBrowserChildrenNode,
            monitorMediaDiscoveryView = monitorMediaDiscoveryView,
            monitorNodeUpdates = monitorNodeUpdates,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.fileBrowserHandle).isEqualTo(-1L)
            assertThat(initial.mediaDiscoveryViewSettings).isEqualTo(MediaDiscoveryViewSettings.INITIAL.ordinal)
        }
    }


    @Test
    fun `test that browser parent handle is updated if new value provided`() = runTest {
        underTest.state.map { it.fileBrowserHandle }.distinctUntilChanged()
            .test {
                val newValue = 123456789L
                assertThat(awaitItem()).isEqualTo(-1L)
                underTest.setBrowserParentHandle(newValue)
                assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that get safe browser handle returns INVALID_HANDLE if not set and root folder fails`() =
        runTest {
            whenever(getRootFolder()).thenReturn(null)
            assertThat(underTest.getSafeBrowserParentHandle()).isEqualTo(MegaApiJava.INVALID_HANDLE)
        }

    @Test
    fun `test that get safe browser handle returns if set`() =
        runTest {
            val expectedHandle = 123456789L
            underTest.setBrowserParentHandle(expectedHandle)
            assertThat(underTest.getSafeBrowserParentHandle()).isEqualTo(expectedHandle)
        }

    @Test
    fun `test that browser node updates live data is set when node updates triggered from use case`() =
        runTest {
            whenever(getBrowserChildrenNode(any())).thenReturn(listOf(mock(), mock()))

            runCatching {
                val result =
                    underTest.updateBrowserNodes.test().awaitValue(50, TimeUnit.MILLISECONDS)
                monitorNodeUpdates.emit(listOf(mock()))
                result
            }.onSuccess { result ->
                result.assertValue { it.getContentIfNotHandled()?.size == 2 }
            }
        }

    @Test
    fun `test that browser node updates live data is not set when get browser node returns a null list`() =
        runTest {
            whenever(getBrowserChildrenNode(any())).thenReturn(null)

            runCatching {
                val result =
                    underTest.updateBrowserNodes.test().awaitValue(50, TimeUnit.MILLISECONDS)
                monitorNodeUpdates.emit(listOf(mock()))
                result
            }.onSuccess { result ->
                result.assertNoValue()
            }
        }
}
