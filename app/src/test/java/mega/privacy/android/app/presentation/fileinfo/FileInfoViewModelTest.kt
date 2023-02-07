package mega.privacy.android.app.presentation.fileinfo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.usecase.CopyNodeUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.IsNodeInInbox
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.filenode.GetFileHistoryNumVersions
import mega.privacy.android.domain.usecase.filenode.MoveNodeByHandle
import nz.mega.sdk.MegaNode
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
internal class FileInfoViewModelTest {
    private lateinit var underTest: FileInfoViewModel

    private lateinit var monitorStorageStateEvent: MonitorStorageStateEvent
    private lateinit var monitorConnectivity: MonitorConnectivity
    private lateinit var getFileHistoryNumVersions: GetFileHistoryNumVersions
    private lateinit var isNodeInInbox: IsNodeInInbox
    private lateinit var checkNameCollision: CheckNameCollision
    private lateinit var moveNodeByHandle: MoveNodeByHandle
    private lateinit var copyNodeUseCase: CopyNodeUseCase
    private lateinit var node: MegaNode
    private lateinit var nameCollision: NameCollision


    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initMocks()
        runTest {
            initDefaultMockBehaviour()
        }
        initUnderTestViewModel()
    }

    private fun initMocks() {
        monitorStorageStateEvent = mock()
        monitorConnectivity = mock()
        getFileHistoryNumVersions = mock()
        isNodeInInbox = mock()
        checkNameCollision = mock()
        moveNodeByHandle = mock()
        copyNodeUseCase = mock()
        node = mock()
        nameCollision = mock()
    }

    private fun initUnderTestViewModel() {
        underTest = FileInfoViewModel(
            monitorStorageStateEvent,
            monitorConnectivity,
            getFileHistoryNumVersions,
            isNodeInInbox,
            checkNameCollision,
            moveNodeByHandle,
            copyNodeUseCase
        )
        underTest.updateNode(node)
    }

    private suspend fun initDefaultMockBehaviour() {
        whenever(node.handle).thenReturn(NODE_HANDLE)
        whenever(monitorConnectivity.invoke()).thenReturn(MutableStateFlow(true))
        whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(0)
        whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that viewModel state's historyVersions property reflects the value of the getFileHistoryNumVersions use case after updating the node`() =
        runBlocking {
            for (n in 0..5) {
                whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(n)
                underTest.updateNode(node)
                Truth.assertThat(underTest.uiState.value.historyVersions).isEqualTo(n)
            }
        }

    @Test
    fun `test that viewModel state's isNodeInInbox property reflects the value of the isNodeInInbox use case after updating the node`() =
        runBlocking {
            suspend fun verify(isNodeInInbox: Boolean) {
                whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(isNodeInInbox)
                underTest.updateNode(node)
                Truth.assertThat(underTest.uiState.value.isNodeInInbox).isEqualTo(isNodeInInbox)
                Truth.assertThat(underTest.isNodeInInbox()).isEqualTo(isNodeInInbox)
            }
            verify(true)
            verify(false)
        }

    @Test
    fun `test showHistoryVersions is true if the node contains one version and is not in the inbox`() =
        runBlocking {
            whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(1)
            whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(false)
            underTest.updateNode(node)
            Truth.assertThat(underTest.uiState.value.showHistoryVersions).isEqualTo(true)
        }

    @Test
    fun `test showHistoryVersions is true if the node contains more than one version and is not in the inbox`() =
        runBlocking {
            whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(2)
            whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(false)
            underTest.updateNode(node)
            Truth.assertThat(underTest.uiState.value.showHistoryVersions).isEqualTo(true)
        }

    @Test
    fun `test showHistoryVersions is false if the node contains one version but is in the inbox`() =
        runBlocking {
            whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(1)
            whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(true)
            underTest.updateNode(node)
            Truth.assertThat(underTest.uiState.value.showHistoryVersions).isEqualTo(false)

        }

    @Test
    fun `test showHistoryVersions is false if the node contains no versions and is not in the inbox`() =
        runBlocking {
            whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(0)
            whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(false)
            underTest.updateNode(node)
            Truth.assertThat(underTest.uiState.value.showHistoryVersions).isEqualTo(false)

        }

    @Test
    fun `test NotConnected event is launched if not connected while moving`() =
        runTest {
            whenever(monitorConnectivity.invoke()).thenReturn(MutableStateFlow(false))
            underTest.moveNodeCheckingCollisions(parentId)
            Truth.assertThat(underTest.uiState.value.oneOffViewEvent)
                .isEqualTo(FileInfoOneOffViewEvent.NotConnected)

        }

    @Test
    fun `test CollisionDetected event is launched when a collision is found while moving`() =
        runTest {
            whenever(checkNameCollision(nodeId, parentId, NameCollisionType.MOVE)).thenReturn(
                nameCollision
            )
            underTest.moveNodeCheckingCollisions(parentId)
            testNextEventIsOfType(FileInfoOneOffViewEvent.CollisionDetected::class.java)
        }

    @Test
    fun `test CollisionDetected event is launched when a collision is found while copying`() =
        runTest {
            whenever(checkNameCollision(nodeId, parentId, NameCollisionType.COPY)).thenReturn(
                nameCollision
            )
            underTest.copyNodeCheckingCollisions(parentId)
            testNextEventIsOfType(FileInfoOneOffViewEvent.CollisionDetected::class.java)
        }

    @Test
    fun `test FinishedMoving event is launched without exceptions when the move finished OK`() =
        runTest {
            whenever(
                moveNodeByHandle.invoke(nodeId, parentId)
            ).thenReturn(Unit)
            whenever(checkNameCollision(nodeId, parentId, NameCollisionType.MOVE)).thenThrow(
                MegaNodeException.ChildDoesNotExistsException::class.java
            )
            underTest.moveNodeCheckingCollisions(parentId)
            testNextEventIsOfType(FileInfoOneOffViewEvent.FinishedMoving::class.java)?.also {
                Truth.assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedMoving event is launched with the proper exceptions when the move finished KO`() =
        runTest {
            whenever(
                moveNodeByHandle.invoke(nodeId, parentId)
            ).thenThrow(RuntimeException("fake exception"))
            whenever(checkNameCollision(nodeId, parentId, NameCollisionType.MOVE)).thenThrow(
                MegaNodeException.ChildDoesNotExistsException::class.java
            )
            underTest.moveNodeCheckingCollisions(parentId)
            testNextEventIsOfType(FileInfoOneOffViewEvent.FinishedMoving::class.java)?.also {
                Truth.assertThat(it.exception).isNotNull()
            }
        }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : FileInfoOneOffViewEvent> testNextEventIsOfType(
        clazz: Class<T>,
    ): T? {
        return getNextEvent().also {
            Truth.assertThat(it).isInstanceOf(clazz)
        } as? T?
    }

    private suspend fun getNextEvent(): FileInfoOneOffViewEvent =
        underTest.uiState.mapNotNull { it.oneOffViewEvent }.first()


    @Test
    fun `test NotConnected event is launched if not connected while copying`() =
        runBlocking {
            whenever(monitorConnectivity.invoke()).thenReturn(MutableStateFlow(false))
            underTest.copyNodeCheckingCollisions(parentId)
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.oneOffViewEvent)
                    .isEqualTo(FileInfoOneOffViewEvent.NotConnected)
            }
        }

    companion object {
        private const val NODE_HANDLE = 10L
        private const val PARENT_NODE_HANDLE = 12L
        private val nodeId = NodeId(NODE_HANDLE)
        private val parentId = NodeId(PARENT_NODE_HANDLE)
    }
}