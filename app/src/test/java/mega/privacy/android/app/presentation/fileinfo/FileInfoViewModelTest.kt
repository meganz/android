package mega.privacy.android.app.presentation.fileinfo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.usecase.IsNodeInInbox
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.filenode.GetFileHistoryNumVersions
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
internal class FileInfoViewModelTest {
    private lateinit var underTest: FileInfoViewModel

    private val monitorStorageStateEvent = mock<MonitorStorageStateEvent>()
    private val monitorConnectivity = mock<MonitorConnectivity>()
    private val getFileHistoryNumVersions = mock<GetFileHistoryNumVersions>()
    private val isNodeInInbox = mock<IsNodeInInbox>()
    private val node = mock<MegaNode>()


    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = FileInfoViewModel(
            monitorStorageStateEvent,
            monitorConnectivity,
            getFileHistoryNumVersions,
            isNodeInInbox
        )
        whenever(node.handle).thenReturn(NODE_HANDLE)
        runTest {
            whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(0)
            whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(false)
        }
    }

    @Test
    fun `test that viewModel state's historyVersions property reflects the value of the getFileHistoryNumVersions use case after updating the node`() =
        runTest {
            for (n in 0..5) {
                whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(n)
                underTest.updateNode(node)
                underTest.uiState.test {
                    val state = awaitItem()
                    Truth.assertThat(state.historyVersions).isEqualTo(n)
                }
            }
        }

    @Test
    fun `test that viewModel state's isNodeInInbox property reflects the value of the isNodeInInbox use case after updating the node`() =
        runTest {
            suspend fun verify(isNodeInInbox: Boolean) {
                whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(isNodeInInbox)
                underTest.updateNode(node)
                underTest.uiState.test {
                    val state = awaitItem()
                    Truth.assertThat(state.isNodeInInbox).isEqualTo(isNodeInInbox)
                }
                Truth.assertThat(underTest.isNodeInInbox()).isEqualTo(isNodeInInbox)
            }
            verify(true)
            verify(false)
        }

    @Test
    fun `test showHistoryVersions is true if the node contains one version and is not in the inbox`() =
        runTest {
            whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(1)
            whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(false)
            underTest.updateNode(node)
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.showHistoryVersions).isEqualTo(true)
            }
        }

    @Test
    fun `test showHistoryVersions is true if the node contains more than one version and is not in the inbox`() =
        runTest {
            whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(2)
            whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(false)
            underTest.updateNode(node)
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.showHistoryVersions).isEqualTo(true)
            }
        }

    @Test
    fun `test showHistoryVersions is false if the node contains one version but is in the inbox`() =
        runTest {
            whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(1)
            whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(true)
            underTest.updateNode(node)
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.showHistoryVersions).isEqualTo(false)
            }
        }

    @Test
    fun `test showHistoryVersions is false if the node contains no versions and is not in the inbox`() =
        runTest {
            whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(0)
            whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(false)
            underTest.updateNode(node)
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.showHistoryVersions).isEqualTo(false)
            }
        }

    companion object {
        const val NODE_HANDLE = 10L
    }
}