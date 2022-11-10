package test.mega.privacy.android.app.presentation.inbox

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetInboxNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.inbox.InboxViewModel
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.domain.usecase.FakeMonitorBackupFolder
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates

/**
 * Test class for [InboxViewModel]
 */
@ExperimentalCoroutinesApi
class InboxViewModelTest {
    private lateinit var underTest: InboxViewModel

    private val getChildrenNode = mock<GetChildrenNode>()
    private val getCloudSortOrder = mock<GetCloudSortOrder> {
        onBlocking { invoke() }.thenReturn(SortOrder.ORDER_DEFAULT_ASC)
    }
    private val getInboxNode = mock<GetInboxNode>()
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getParentNodeHandle = mock<GetParentNodeHandle>()

    private val monitorBackupFolder = FakeMonitorBackupFolder()
    private val monitorNodeUpdates = FakeMonitorUpdates()

    private val rootInboxNode = mock<MegaNode> {
        on { it.handle }.thenReturn(ROOT_INBOX_NODE_ID)
    }
    private val myBackupsNode = mock<NodeId> {
        on { this.id }.thenReturn(MY_BACKUPS_ID)
    }
    private val inboxNode = mock<MegaNode> {
        on { this.handle }.thenReturn(INBOX_NODE_ID)
    }
    private val retrievedNode = mock<MegaNode> {
        on { this.handle }.thenReturn(RETRIEVED_NODE_ID)
    }
    private val emittedNode = mock<Node> {
        on { this.id }.thenReturn(NodeId(EMITTED_NODE_ID))
    }

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    private fun setUnderTest() {
        underTest = InboxViewModel(
            getChildrenNode = getChildrenNode,
            getCloudSortOrder = getCloudSortOrder,
            getInboxNode = getInboxNode,
            getNodeByHandle = getNodeByHandle,
            getParentNodeHandle = getParentNodeHandle,
            monitorBackupFolder = monitorBackupFolder,
            monitorNodeUpdates = monitorNodeUpdates,
        )
    }

    private suspend fun setupData() {
        whenever(getNodeByHandle(any())).thenReturn(inboxNode)
        whenever(getChildrenNode(
            parent = inboxNode,
            order = getCloudSortOrder(),
        )).thenReturn(listOf(retrievedNode))
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        setUnderTest()

        underTest.state.test {
            val initialState = awaitItem()
            assertThat(initialState.inboxNodeId).isEqualTo(NodeId(-1L))
            assertThat(initialState.hideMultipleItemSelection).isFalse()
            assertThat(initialState.myBackupsFolderNodeId).isEqualTo(NodeId(-1L))
            assertThat(initialState.nodes).isEmpty()
            assertThat(initialState.shouldExitInbox).isFalse()
            assertThat(initialState.triggerBackPress).isFalse()
        }
    }

    @Test
    fun `test that nodes are refreshed when receiving a node update`() = runTest {
        setupData()
        setUnderTest()

        underTest.updateInboxNodeId(INBOX_NODE_ID)
        monitorNodeUpdates.emit(listOf(emittedNode))

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.hideMultipleItemSelection).isTrue()
            assertThat(state.nodes).isEqualTo(listOf(retrievedNode))
        }
    }

    @Test
    fun `test that nodes are not refreshed when receiving a node update and the parent id is invalid`() =
        runTest {
            setupData()
            setUnderTest()

            monitorNodeUpdates.emit(listOf(emittedNode))

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.nodes).isEmpty()
            }
        }

    @Test
    fun `test that when receiving a my backups folder update, the nodes are refreshed`() =
        runTest {
            setupData()
            setUnderTest()

            underTest.updateInboxNodeId(INBOX_NODE_ID)
            monitorBackupFolder.emit(Result.success(myBackupsNode))

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.inboxNodeId).isEqualTo(NodeId(INBOX_NODE_ID))
                assertThat(state.myBackupsFolderNodeId).isEqualTo(myBackupsNode)
                assertThat(state.nodes).isEqualTo(listOf(retrievedNode))
            }
        }

    @Test
    fun `test that when receiving a my backups folder update, the nodes are refreshed using the my backups folder node id`() =
        runTest {
            setupData()
            setUnderTest()

            monitorBackupFolder.emit(Result.success(myBackupsNode))

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.inboxNodeId).isEqualTo(myBackupsNode)
                assertThat(state.myBackupsFolderNodeId).isEqualTo(myBackupsNode)
                assertThat(state.nodes).isEqualTo(listOf(retrievedNode))
            }
        }

    @Test
    fun `test that the multiple item selection has been handled`() = runTest {
        setUnderTest()

        underTest.hideMultipleItemSelectionHandled()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.hideMultipleItemSelection).isFalse()
        }
    }

    @Test
    fun `test that exiting the inbox has been handled`() = runTest {
        setUnderTest()

        underTest.exitInboxHandled()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.shouldExitInbox).isFalse()
        }
    }

    @Test
    fun `test that the back press has been handled`() = runTest {
        setUnderTest()

        underTest.triggerBackPressHandled()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.triggerBackPress).isFalse()
        }
    }

    @Test
    fun `test that the user is currently on the backup folder level`() = runTest {
        setupData()
        setUnderTest()

        monitorBackupFolder.emit(Result.success(myBackupsNode))

        assertThat(underTest.isCurrentlyOnBackupFolderLevel()).isTrue()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.myBackupsFolderNodeId).isEqualTo(myBackupsNode)
            assertThat(state.inboxNodeId).isEqualTo(myBackupsNode)
        }
    }

    @Test
    fun `test that the user is currently on the backup folder level if the inbox node id is invalid`() =
        runTest {
            setUnderTest()

            assertThat(underTest.isCurrentlyOnBackupFolderLevel()).isTrue()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.inboxNodeId).isEqualTo(NodeId(-1L))
            }
        }

    @Test
    fun `test that the user exits inbox on back press if the root inbox node is null`() = runTest {
        whenever(getParentNodeHandle(any())).thenReturn(987L)
        whenever(getInboxNode()).thenReturn(null)

        setUnderTest()

        with(underTest) {
            updateInboxNodeId(INBOX_NODE_ID)
            handleBackPress()
        }

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.shouldExitInbox).isTrue()
        }
    }

    @Test
    fun `test that the user exits the inbox on back press if the parent node id is null`() =
        runTest {
            whenever(getParentNodeHandle(any())).thenReturn(null)
            whenever(getInboxNode()).thenReturn(rootInboxNode)

            setUnderTest()

            with(underTest) {
                updateInboxNodeId(INBOX_NODE_ID)
                handleBackPress()
            }

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.shouldExitInbox).isTrue()
            }
        }

    @Test
    fun `test that the user exits the inbox on back press if both the root inbox and parent node have the same ids`() =
        runTest {
            val rootInboxNode = mock<MegaNode> {
                on { it.handle }.thenReturn(987L)
            }
            whenever(getParentNodeHandle(any())).thenReturn(987L)
            whenever(getInboxNode()).thenReturn(rootInboxNode)

            setUnderTest()

            with(underTest) {
                updateInboxNodeId(INBOX_NODE_ID)
                handleBackPress()
            }

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.shouldExitInbox).isTrue()
            }
        }

    @Test
    fun `test that the nodes are updated on back press`() = runTest {
        val parentNode = mock<MegaNode> {
            on { it.handle }.thenReturn(654L)
        }
        whenever(getParentNodeHandle(any())).thenReturn(654L)
        whenever(getInboxNode()).thenReturn(rootInboxNode)
        whenever(getNodeByHandle(any())).thenReturn(parentNode)
        whenever(getChildrenNode(
            parent = parentNode,
            order = getCloudSortOrder()
        )).thenReturn(listOf(retrievedNode))

        setUnderTest()

        with(underTest) {
            updateInboxNodeId(INBOX_NODE_ID)
            handleBackPress()
        }

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.inboxNodeId).isEqualTo(NodeId(654L))
            assertThat(state.triggerBackPress).isTrue()
            assertThat(state.nodes).isEqualTo(listOf(retrievedNode))
        }
    }

    @Test
    fun `test that get order returns cloud sort order`() = runTest {
        val expected = SortOrder.ORDER_SIZE_DESC
        whenever(getCloudSortOrder()).thenReturn(expected)

        setUnderTest()

        assertThat(underTest.getOrder()).isEqualTo(expected)
    }

    companion object {
        private const val MY_BACKUPS_ID = 12L
        private const val INBOX_NODE_ID = 34L
        private const val RETRIEVED_NODE_ID = 56L
        private const val EMITTED_NODE_ID = 78L
        private const val ROOT_INBOX_NODE_ID = 90L
    }
}