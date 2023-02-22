package mega.privacy.android.app.presentation.fileinfo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.exception.VersionsNotDeletedException
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import mega.privacy.android.domain.usecase.GetNodeById
import mega.privacy.android.domain.usecase.GetPreview
import mega.privacy.android.domain.usecase.IsNodeInInbox
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.filenode.CopyNodeByHandle
import mega.privacy.android.domain.usecase.filenode.DefaultDeleteNodeVersionsByHandle
import mega.privacy.android.domain.usecase.filenode.DeleteNodeByHandle
import mega.privacy.android.domain.usecase.filenode.GetFileHistoryNumVersions
import mega.privacy.android.domain.usecase.filenode.MoveNodeByHandle
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishByHandle
import nz.mega.sdk.MegaNode
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.net.URI

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
internal class FileInfoViewModelTest {
    private lateinit var underTest: FileInfoViewModel

    private lateinit var monitorStorageStateEvent: MonitorStorageStateEvent
    private lateinit var monitorConnectivity: MonitorConnectivity
    private lateinit var getFileHistoryNumVersions: GetFileHistoryNumVersions
    private lateinit var isNodeInInbox: IsNodeInInbox
    private lateinit var isNodeInRubbish: IsNodeInRubbish
    private lateinit var checkNameCollision: CheckNameCollision
    private lateinit var moveNodeByHandle: MoveNodeByHandle
    private lateinit var moveNodeToRubbishByHandle: MoveNodeToRubbishByHandle
    private lateinit var copyNodeByHandle: CopyNodeByHandle
    private lateinit var deleteNodeByHandle: DeleteNodeByHandle
    private lateinit var deleteNodeVersionsByHandle: DefaultDeleteNodeVersionsByHandle
    private lateinit var node: MegaNode
    private lateinit var nameCollision: NameCollision
    private lateinit var getPreview: GetPreview
    private lateinit var getFolderTreeInfo: GetFolderTreeInfo
    private lateinit var getNodeById: GetNodeById

    private lateinit var typedFileNode: TypedFileNode
    private lateinit var previewFile: File


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
        isNodeInRubbish = mock()
        checkNameCollision = mock()
        moveNodeByHandle = mock()
        copyNodeByHandle = mock()
        moveNodeToRubbishByHandle = mock()
        deleteNodeByHandle = mock()
        deleteNodeVersionsByHandle = mock()
        node = mock()
        nameCollision = mock()
        getPreview = mock()
        getFolderTreeInfo = mock()
        getNodeById = mock()

        typedFileNode = mock()
        previewFile = mock()
    }

    private fun initUnderTestViewModel() {
        underTest = FileInfoViewModel(
            monitorStorageStateEvent = monitorStorageStateEvent,
            monitorConnectivity = monitorConnectivity,
            getFileHistoryNumVersions = getFileHistoryNumVersions,
            isNodeInInbox = isNodeInInbox,
            isNodeInRubbish = isNodeInRubbish,
            checkNameCollision = checkNameCollision,
            moveNodeByHandle = moveNodeByHandle,
            copyNodeByHandle = copyNodeByHandle,
            moveNodeToRubbishByHandle = moveNodeToRubbishByHandle,
            deleteNodeByHandle = deleteNodeByHandle,
            deleteNodeVersionsByHandle = deleteNodeVersionsByHandle,
            getPreview = getPreview,
            getFolderTreeInfo = getFolderTreeInfo,
            getNodeById = getNodeById
        )
        underTest.updateNode(node)
    }

    private suspend fun initDefaultMockBehaviour() {
        whenever(node.handle).thenReturn(NODE_HANDLE)
        whenever(typedFileNode.id).thenReturn(nodeId)
        whenever(node.hasPreview()).thenReturn(true)
        whenever(monitorConnectivity.invoke()).thenReturn(MutableStateFlow(true))
        whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(0)
        whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(false)
        whenever(isNodeInRubbish(NODE_HANDLE)).thenReturn(false)
        whenever(previewFile.exists()).thenReturn(true)
        whenever(previewFile.toURI()).thenReturn(URI.create(previewUri))
        whenever(getNodeById.invoke(nodeId)).thenReturn(typedFileNode)
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
    fun `test that viewModel state's isNodeInRubbish property reflects the value of the isNodeInRubbish use case after updating the node`() =
        runTest {
            suspend fun verify(isNodeInRubbish: Boolean) {
                whenever(isNodeInRubbish(NODE_HANDLE)).thenReturn(isNodeInRubbish)
                underTest.updateNode(node)
                underTest.uiState.test {
                    val state = awaitItem()
                    Truth.assertThat(state.isNodeInRubbish).isEqualTo(isNodeInRubbish)
                }
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
    fun `test NotConnected event is launched if not connected while copying`() =
        runTest {
            whenever(monitorConnectivity.invoke()).thenReturn(MutableStateFlow(false))
            underTest.copyNodeCheckingCollisions(parentId)
            Truth.assertThat(underTest.uiState.value.oneOffViewEvent)
                .isEqualTo(FileInfoOneOffViewEvent.NotConnected)
        }

    @Test
    fun `test CollisionDetected event is launched when a collision is found while moving`() =
        runTest {
            mockCollisionMoving()
            underTest.moveNodeCheckingCollisions(parentId)
            testEventIsOfType(FileInfoOneOffViewEvent.CollisionDetected::class.java)
        }

    @Test
    fun `test CollisionDetected event is launched when a collision is found while copying`() =
        runTest {
            mockCollisionCopying()
            underTest.copyNodeCheckingCollisions(parentId)
            testEventIsOfType(FileInfoOneOffViewEvent.CollisionDetected::class.java)
        }

    @Test
    fun `test GeneralError event is launched when an unknown error is returned when check collision`() =
        runTest {
            whenever(checkNameCollision(nodeId, parentId, NameCollisionType.COPY))
                .thenThrow(RuntimeException::class.java)
            underTest.copyNodeCheckingCollisions(parentId)
            testEventIsOfType(FileInfoOneOffViewEvent.GeneralError::class.java)
        }

    @Test
    fun `test FinishedMoving event is launched without exceptions when the move finished successfully`() =
        runTest {
            mockMoveSuccess()
            underTest.moveNodeCheckingCollisions(parentId)
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Moving::class.java)?.also {
                Truth.assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedMoving event is launched with the proper exceptions when the move finished with an error`() =
        runTest {
            mockMoveFailure()
            underTest.moveNodeCheckingCollisions(parentId)
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Moving::class.java)?.also {
                Truth.assertThat(it.exception).isNotNull()
            }
        }

    @Test
    fun `test FinishedMovingToRubbish event is launched without exceptions when the move finished successfully`() =
        runTest {
            mockMoveToRubbishSuccess()
            underTest.removeNode()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.MovingToRubbish::class.java)?.also {
                Truth.assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedMovingToRubbish event is launched with the proper exceptions when the move finished with an error`() =
        runTest {
            mockMoveToRubbishFailure()
            underTest.removeNode()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.MovingToRubbish::class.java)?.also {
                Truth.assertThat(it.exception).isNotNull()
            }
        }

    @Test
    fun `test FinishedDeleting event is launched without exceptions when the delete finished successfully`() =
        runTest {
            mockDeleteSuccess()
            underTest.removeNode()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Deleting::class.java)?.also {
                Truth.assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedDeleting event is launched with the proper exceptions when the delete finished with an error`() =
        runTest {
            mockDeleteFailure()
            underTest.removeNode()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Deleting::class.java)?.also {
                Truth.assertThat(it.exception).isNotNull()
            }
        }

    @Test
    fun `test FinishedCopying event is launched without exceptions when the move finished successfully`() =
        runTest {
            mockCopySuccess()
            underTest.copyNodeCheckingCollisions(parentId)
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Copying::class.java)?.also {
                Truth.assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedCopying event is launched with the proper exceptions when the move finished with an error`() =
        runTest {
            mockCopyFailure()
            underTest.copyNodeCheckingCollisions(parentId)
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Copying::class.java)?.also {
                Truth.assertThat(it.exception).isNotNull()
            }
        }

    @Test
    fun `test FinishedDeletingVersions event is launched without exceptions when the delete versions finished successfully`() =
        runTest {
            mockDeleteVersionsSuccess()
            underTest.deleteHistoryVersions()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.DeletingVersions::class.java)?.also {
                Truth.assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedDeletingVersions event is launched with the proper exceptions when the delete versions finished with an error`() =
        runTest {
            mockDeleteVersionsFailure(null)
            underTest.deleteHistoryVersions()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.DeletingVersions::class.java)?.also {
                Truth.assertThat(it.exception).isNotNull()
                Truth.assertThat(it.exception)
                    .isNotInstanceOf(VersionsNotDeletedException::class.java)
            }
        }

    @Test
    fun `test FinishedDeletingVersions event is launched with the proper exceptions when the delete versions finished with some errors`() =
        runTest {
            val errors = 3
            val total = 5
            mockDeleteVersionsFailure(total, errors)
            underTest.deleteHistoryVersions()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.DeletingVersions::class.java)?.also {
                Truth.assertThat(it.exception).isNotNull()
                Truth.assertThat(it.exception).isInstanceOf(VersionsNotDeletedException::class.java)
                (it.exception as? VersionsNotDeletedException)?.let { exception ->
                    Truth.assertThat(exception.totalNotDeleted).isEqualTo(errors)
                    Truth.assertThat(exception.totalRequestedToDelete).isEqualTo(total)
                }
            }
        }

    @Test
    fun `test FileInfoJobInProgressState is set while copying successfully, and unset at the end`() =
        runTest {
            mockCopySuccess()
            testProgressIsSetWhileCopyingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while copying with an error, and unset at the end`() =
        runTest {
            mockCopyFailure()
            testProgressIsSetWhileCopyingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while copying and Name conflict found, and unset at the end`() =
        runTest {
            mockCollisionCopying()
            testProgressIsSetWhileCopyingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while moving successfully, and unset at the end`() =
        runTest {
            mockMoveSuccess()
            testProgressIsSetWhileMovingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while moving with an error, and unset at the end`() =
        runTest {
            mockMoveFailure()
            testProgressIsSetWhileMovingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while moving and Name conflict found, and unset at the end`() =
        runTest {
            mockCollisionMoving()
            testProgressIsSetWhileMovingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while moving to rubbish successfully, and unset at the end`() =
        runTest {
            mockMoveToRubbishSuccess()
            testProgressIsSetWhileMovingToRubbishBinAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while moving to rubbish with an error, and unset at the end`() =
        runTest {
            mockMoveToRubbishFailure()
            testProgressIsSetWhileMovingToRubbishBinAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while deleting successfully, and unset at the end`() =
        runTest {
            mockDeleteSuccess()
            testProgressIsSetWhileDeletingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while deleting with an error, and unset at the end`() =
        runTest {
            mockDeleteSuccess()
            testProgressIsSetWhileDeletingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while deleting versions successfully, and unset at the end`() =
        runTest {
            mockDeleteVersionsSuccess()
            testProgressIsSetWhileDeletingVersionsAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while deleting versions with an error, and unset at the end`() =
        runTest {
            mockDeleteVersionsFailure()
            testProgressIsSetWhileDeletingVersionsAndUnset()
        }


    @Test
    fun `test on-off event is removed from state once is consumed`() {
        `test CollisionDetected event is launched when a collision is found while copying`()
        runTest {
            Truth.assertThat(underTest.uiState.value.oneOffViewEvent).isNotNull()
            underTest.consumeOneOffEvent(underTest.uiState.value.oneOffViewEvent ?: return@runTest)
            Truth.assertThat(underTest.uiState.value.oneOffViewEvent).isNull()
        }
    }

    @Test
    fun `test preview is assigned when node is updated`() = runTest {
        whenever(getPreview.invoke(NODE_HANDLE)).thenReturn(previewFile)
        whenever(typedFileNode.thumbnailPath).thenReturn(null)
        underTest.updateNode(node)
        underTest.uiState.mapNotNull { it.actualPreviewUriString }.test {
            val state = awaitItem()
            Truth.assertThat(state).isEqualTo(previewUri)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test thumbnail is assigned when node is updated and there are no preview`() = runTest {
        whenever(getPreview.invoke(NODE_HANDLE)).thenReturn(null)
        whenever(typedFileNode.thumbnailPath).thenReturn(thumbUri)
        underTest.updateNode(node)
        underTest.uiState.mapNotNull { it.actualPreviewUriString }.test {
            val state = awaitItem()
            Truth.assertThat(state).isEqualTo(thumbUri)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test preview has priority over thumbnail`() = runTest {
        whenever(getPreview.invoke(NODE_HANDLE)).thenReturn(previewFile)
        whenever(typedFileNode.thumbnailPath).thenReturn(thumbUri)
        underTest.updateNode(node)
        underTest.uiState.mapNotNull { it.actualPreviewUriString }.test {
            val state = awaitItem()
            Truth.assertThat(state).isEqualTo(previewUri)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : FileInfoOneOffViewEvent> testEventIsOfType(
        clazz: Class<T>,
    ): T? {
        return getEvent().also {
            Truth.assertThat(it).isInstanceOf(clazz)
        } as? T?
    }

    private suspend fun <T : FileInfoJobInProgressState> testNextEventIsOfTypeFinishedAndJobIsOfType(
        finishedJobClass: Class<T>,
    ): FileInfoOneOffViewEvent.Finished? {
        return getEvent().also {
            Truth.assertThat(it).isInstanceOf(FileInfoOneOffViewEvent.Finished::class.java)
            val jobFinished = (it as FileInfoOneOffViewEvent.Finished).jobFinished
            Truth.assertThat(jobFinished).isInstanceOf(finishedJobClass)
        } as? FileInfoOneOffViewEvent.Finished?
    }

    private suspend fun getEvent(): FileInfoOneOffViewEvent =
        underTest.uiState.value.oneOffViewEvent
            ?: underTest.uiState.mapNotNull { it.oneOffViewEvent }.first()


    private suspend fun mockCollisionCopying() {
        whenever(checkNameCollision(nodeId, parentId, NameCollisionType.COPY))
            .thenReturn(nameCollision)
    }

    private suspend fun mockCollisionMoving() {
        whenever(checkNameCollision(nodeId, parentId, NameCollisionType.MOVE))
            .thenReturn(nameCollision)
    }

    private suspend fun mockCopySuccess() {
        whenever(copyNodeByHandle.invoke(nodeId, parentId)).thenReturn(nodeId)
        whenever(checkNameCollision(nodeId, parentId, NameCollisionType.COPY))
            .thenThrow(MegaNodeException.ChildDoesNotExistsException::class.java)
    }

    private suspend fun mockMoveSuccess() {
        whenever(moveNodeByHandle.invoke(nodeId, parentId)).thenReturn(nodeId)
        whenever(checkNameCollision(nodeId, parentId, NameCollisionType.MOVE))
            .thenThrow(MegaNodeException.ChildDoesNotExistsException::class.java)
    }

    private suspend fun mockCopyFailure() {
        whenever(copyNodeByHandle.invoke(nodeId, parentId))
            .thenThrow(RuntimeException("fake exception"))
        whenever(checkNameCollision(nodeId, parentId, NameCollisionType.COPY))
            .thenThrow(MegaNodeException.ChildDoesNotExistsException::class.java)
    }

    private suspend fun mockMoveFailure() {
        whenever(moveNodeByHandle.invoke(nodeId, parentId))
            .thenThrow(RuntimeException("fake exception"))
        whenever(checkNameCollision(nodeId, parentId, NameCollisionType.MOVE))
            .thenThrow(MegaNodeException.ChildDoesNotExistsException::class.java)
    }

    private suspend fun mockMoveToRubbishSuccess() {
        whenever(moveNodeToRubbishByHandle.invoke(nodeId)).thenReturn(Unit)
    }

    private suspend fun mockMoveToRubbishFailure() {
        whenever(moveNodeToRubbishByHandle.invoke(nodeId)).thenThrow(RuntimeException("fake exception"))
    }

    private suspend fun mockDeleteSuccess() {
        whenever(isNodeInRubbish(NODE_HANDLE)).thenReturn(true)
        underTest.updateNode(node)
        whenever(deleteNodeByHandle.invoke(nodeId)).thenReturn(Unit)
    }

    private suspend fun mockDeleteVersionsSuccess() {
        whenever(deleteNodeVersionsByHandle.invoke(nodeId)).thenReturn(Unit)
    }

    private suspend fun mockDeleteVersionsFailure(
        totalRequested: Int? = null,
        totalFailure: Int? = null,
    ) {
        whenever(deleteNodeVersionsByHandle.invoke(nodeId)).thenThrow(
            if (totalFailure == null) {
                RuntimeException("fake exception")
            } else {
                VersionsNotDeletedException(totalRequested ?: (totalFailure + 1), totalFailure)
            }
        )
    }

    private suspend fun mockDeleteFailure() {
        whenever(isNodeInRubbish(NODE_HANDLE)).thenReturn(true)
        underTest.updateNode(node)
        whenever(deleteNodeByHandle.invoke(nodeId)).thenThrow(RuntimeException("fake exception"))
    }

    private suspend fun testProgressIsSetWhileCopyingAndUnset() =
        testProgressSetAndUnset(FileInfoJobInProgressState.Copying) {
            underTest.copyNodeCheckingCollisions(parentId)
        }

    private suspend fun testProgressIsSetWhileMovingAndUnset() =
        testProgressSetAndUnset(FileInfoJobInProgressState.Moving) {
            underTest.moveNodeCheckingCollisions(parentId)
        }

    private suspend fun testProgressIsSetWhileMovingToRubbishBinAndUnset() =
        testProgressSetAndUnset(FileInfoJobInProgressState.MovingToRubbish) {
            underTest.removeNode()
        }

    private suspend fun testProgressIsSetWhileDeletingAndUnset() =
        testProgressSetAndUnset(FileInfoJobInProgressState.Deleting) {
            underTest.removeNode()
        }

    private suspend fun testProgressIsSetWhileDeletingVersionsAndUnset() =
        testProgressSetAndUnset(FileInfoJobInProgressState.DeletingVersions) {
            underTest.deleteHistoryVersions()
        }

    private suspend fun testProgressSetAndUnset(
        progress: FileInfoJobInProgressState,
        block: () -> Unit,
    ) = runBlocking {
        underTest.uiState.map { it.jobInProgressState }.distinctUntilChanged().test {
            Truth.assertThat(awaitItem()).isNull()
            block()
            Truth.assertThat(awaitItem()).isEqualTo(progress)
            Truth.assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    companion object {
        private const val NODE_HANDLE = 10L
        private const val PARENT_NODE_HANDLE = 12L
        private val nodeId = NodeId(NODE_HANDLE)
        private val parentId = NodeId(PARENT_NODE_HANDLE)
        private const val thumbUri = "file:/thumb"
        private const val previewUri = "file:/preview"
    }
}