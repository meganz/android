package test.mega.privacy.android.app.presentation.clouddrive

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.presentation.clouddrive.FileBrowserViewModel
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates
import kotlin.test.assertFalse

@ExperimentalCoroutinesApi
class FileBrowserViewModelTest {
    private lateinit var underTest: FileBrowserViewModel

    private val getRootFolder = mock<GetRootFolder>()
    private val isNodeInRubbish = mock<IsNodeInRubbish>()
    private val getBrowserChildrenNode = mock<GetBrowserChildrenNode>()
    private val monitorMediaDiscoveryView = mock<MonitorMediaDiscoveryView> {
        on { invoke() }.thenReturn(
            emptyFlow()
        )
    }
    private val monitorNodeUpdates = FakeMonitorUpdates()
    private val getFileBrowserParentNodeHandle = mock<GetParentNodeHandle>()

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
            getFileBrowserParentNodeHandle = getFileBrowserParentNodeHandle,
            getIsNodeInRubbish = isNodeInRubbish
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
    fun `test that on setting Browser Parent Handle, handle File Browser node returns some items in list`() =
        runTest {
            val newValue = 123456789L
            whenever(getBrowserChildrenNode.invoke(newValue)).thenReturn(
                listOf(mock(), mock())
            )
            val update = mapOf<Node, List<NodeChanges>>(
                mock<Node>() to emptyList(),
                mock<Node>() to emptyList()
            )
            monitorNodeUpdates.emit(NodeUpdate(update))
            underTest.setBrowserParentHandle(newValue)
            assertThat(underTest.state.value.nodes.size).isEqualTo(2)
        }

    @Test
    fun `test that on setting Browser Parent Handle, handle File Browser node returns null`() =
        runTest {
            val newValue = 123456789L
            whenever(getBrowserChildrenNode.invoke(newValue)).thenReturn(null)
            underTest.setBrowserParentHandle(newValue)
            assertThat(underTest.state.value.nodes.size).isEqualTo(0)
            verify(getBrowserChildrenNode, times(1)).invoke(newValue)
        }

    @Test
    fun `test that when nodes are empty then Enter in MD mode will return false`() = runTest {
        val newValue = 123456789L
        whenever(getBrowserChildrenNode.invoke(newValue)).thenReturn(null)
        underTest.setBrowserParentHandle(newValue)

        val shouldEnter =
            underTest.shouldEnterMediaDiscoveryMode(newValue,
                MediaDiscoveryViewSettings.INITIAL.ordinal)
        assertFalse(shouldEnter)
    }

    @Test
    fun `test that when MediaDiscoveryViewSettings is Disabled then Enter in MD mode will return false`() =
        runTest {
            val newValue = 123456789L
            whenever(getBrowserChildrenNode.invoke(newValue)).thenReturn(
                listOf(mock(), mock())
            )
            val update = mapOf<Node, List<NodeChanges>>(
                mock<Node>() to emptyList(),
                mock<Node>() to emptyList()
            )
            monitorNodeUpdates.emit(NodeUpdate(update))
            underTest.setBrowserParentHandle(newValue)

            val shouldEnter =
                underTest.shouldEnterMediaDiscoveryMode(
                    newValue,
                    MediaDiscoveryViewSettings.DISABLED.ordinal
                )
            assertFalse(shouldEnter)
        }

    @Test
    fun `test that when MediaDiscoveryViewSettings is Enabled and nodes contains not folder then Enter in MD mode will return false`() =
        runTest {
            val newValue = 123456789L
            val folderNode = mock<MegaNode> {
                on { isFolder }.thenReturn(true)
            }
            whenever(getBrowserChildrenNode.invoke(newValue)).thenReturn(listOf(folderNode))

            underTest.setBrowserParentHandle(newValue)

            val shouldEnter =
                underTest.shouldEnterMediaDiscoveryMode(
                    newValue,
                    MediaDiscoveryViewSettings.ENABLED.ordinal
                )
            assertFalse(shouldEnter)
        }

    @Test
    fun `test that when folder is clicked from adapter, then stack gets updated with appropriate value`() =
        runTest {
            val lastFirstVisiblePosition = 123456
            val newValue = 12345L

            val update = mapOf<Node, List<NodeChanges>>(
                mock<Node>() to emptyList(),
                mock<Node>() to emptyList()
            )
            monitorNodeUpdates.emit(NodeUpdate(update))
            underTest.setBrowserParentHandle(newValue)

            underTest.onFolderItemClicked(lastFirstVisiblePosition, newValue)
            assertThat(underTest.popLastPositionStack()).isEqualTo(lastFirstVisiblePosition)
        }

    @Test
    fun `test that last position returns 0 when items are popped from stack and stack has no items`() {
        val poppedValue = underTest.popLastPositionStack()
        assertThat(poppedValue).isEqualTo(0)
    }

    @Test
    fun `test that when handle on back pressed and parent handle is null, then getRubbishBinChildrenNode is not invoked`() =
        runTest {
            val newValue = 123456789L
            underTest.onBackPressed()
            verify(getBrowserChildrenNode, times(0)).invoke(newValue)
        }

    @Test
    fun `test that when handle on back pressed and parent handle is not null, then getBrowserChildrenNode is invoked once`() =
        runTest {
            val newValue = 123456789L
            // to update handles rubbishBinHandle
            underTest.setBrowserParentHandle(newValue)
            underTest.onBackPressed()
            verify(getBrowserChildrenNode, times(1)).invoke(newValue)
        }
}
