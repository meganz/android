package test.mega.privacy.android.app.fileinfo

import android.app.Activity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.domain.usecase.GetNodeLocationInfo
import mega.privacy.android.app.domain.usecase.offline.SetNodeAvailableOffline
import mega.privacy.android.app.domain.usecase.shares.GetOutShares
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.presentation.fileinfo.FileInfoJobInProgressState
import mega.privacy.android.app.presentation.fileinfo.FileInfoOneOffViewEvent
import mega.privacy.android.app.presentation.fileinfo.FileInfoViewModel
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.utils.wrapper.FileUtilWrapper
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.exception.VersionsNotDeletedException
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import mega.privacy.android.domain.usecase.GetNodeById
import mega.privacy.android.domain.usecase.GetPreview
import mega.privacy.android.domain.usecase.IsAvailableOffline
import mega.privacy.android.domain.usecase.IsNodeInInbox
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MonitorChildrenUpdates
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.MonitorNodeUpdatesById
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.filenode.CopyNodeByHandle
import mega.privacy.android.domain.usecase.filenode.DeleteNodeByHandle
import mega.privacy.android.domain.usecase.filenode.DeleteNodeVersionsByHandle
import mega.privacy.android.domain.usecase.filenode.GetFileHistoryNumVersions
import mega.privacy.android.domain.usecase.filenode.GetNodeVersionsByHandle
import mega.privacy.android.domain.usecase.filenode.MoveNodeByHandle
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishByHandle
import mega.privacy.android.domain.usecase.shares.GetContactItemFromInShareFolder
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.lang.ref.WeakReference
import java.net.URI

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
internal class FileInfoViewModelTest {
    private lateinit var underTest: FileInfoViewModel

    private val fileUtilWrapper: FileUtilWrapper = mock()
    private val monitorStorageStateEvent: MonitorStorageStateEvent = mock()
    private val monitorConnectivity: MonitorConnectivity = mock()
    private val getFileHistoryNumVersions: GetFileHistoryNumVersions = mock()
    private val isNodeInInbox: IsNodeInInbox = mock()
    private val isNodeInRubbish: IsNodeInRubbish = mock()
    private val checkNameCollision: CheckNameCollision = mock()
    private val moveNodeByHandle: MoveNodeByHandle = mock()
    private val moveNodeToRubbishByHandle: MoveNodeToRubbishByHandle = mock()
    private val copyNodeByHandle: CopyNodeByHandle = mock()
    private val deleteNodeByHandle: DeleteNodeByHandle = mock()
    private val deleteNodeVersionsByHandle: DeleteNodeVersionsByHandle = mock()
    private val node: MegaNode = mock()
    private val nameCollision: NameCollision = mock()
    private val getPreview: GetPreview = mock()
    private val getFolderTreeInfo: GetFolderTreeInfo = mock()
    private val getNodeById: GetNodeById = mock()
    private val getContactItemFromInShareFolder: GetContactItemFromInShareFolder = mock()
    private val monitorNodeUpdatesById: MonitorNodeUpdatesById = mock()
    private val monitorChildrenUpdates: MonitorChildrenUpdates = mock()
    private val monitorContactUpdates: MonitorContactUpdates = mock()
    private val megaNodeRepository: MegaNodeRepository = mock()
    private val getNodeVersionsByHandle: GetNodeVersionsByHandle = mock()
    private val getNodeLocationInfo: GetNodeLocationInfo = mock()
    private val getOutShares: GetOutShares = mock()
    private val isAvailableOffline: IsAvailableOffline = mock()
    private val setNodeAvailableOffline: SetNodeAvailableOffline = mock()
    private val getNodeAccessPermission: GetNodeAccessPermission = mock()

    private val typedFileNode: TypedFileNode = mock()

    private val previewFile: File = mock()
    private val activity = WeakReference(mock<Activity>())


    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        runTest {
            initDefaultMockBehaviour()
        }
        initUnderTestViewModel()
    }

    private fun initUnderTestViewModel() {
        underTest = FileInfoViewModel(
            tempMegaNodeRepository = megaNodeRepository,
            fileUtilWrapper = fileUtilWrapper,
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
            getNodeById = getNodeById,
            getContactItemFromInShareFolder = getContactItemFromInShareFolder,
            monitorNodeUpdatesById = monitorNodeUpdatesById,
            monitorChildrenUpdates = monitorChildrenUpdates,
            monitorContactUpdates = monitorContactUpdates,
            getNodeVersionsByHandle = getNodeVersionsByHandle,
            getNodeLocationInfo = getNodeLocationInfo,
            getOutShares = getOutShares,
            isAvailableOffline = isAvailableOffline,
            setNodeAvailableOffline = setNodeAvailableOffline,
            getNodeAccessPermission = getNodeAccessPermission,
        )
    }

    private suspend fun initDefaultMockBehaviour() {
        whenever(node.handle).thenReturn(NODE_HANDLE)
        whenever(typedFileNode.id).thenReturn(nodeId)
        whenever(monitorConnectivity.invoke()).thenReturn(MutableStateFlow(true))
        whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(0)
        whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(false)
        whenever(isNodeInRubbish(NODE_HANDLE)).thenReturn(false)
        whenever(previewFile.exists()).thenReturn(true)
        whenever(previewFile.toURI()).thenReturn(URI.create(previewUri))
        whenever(getNodeById.invoke(nodeId)).thenReturn(typedFileNode)
        whenever(megaNodeRepository.getNodeByHandle(node.handle)).thenReturn(node)
        whenever(getNodeVersionsByHandle(nodeId)).thenReturn(null)
        whenever(monitorNodeUpdatesById.invoke(nodeId)).thenReturn(emptyFlow())
        whenever(monitorChildrenUpdates.invoke(nodeId)).thenReturn(emptyFlow())
        whenever(monitorContactUpdates.invoke()).thenReturn(emptyFlow())
        whenever(fileUtilWrapper.getFileIfExists(null, thumbUri))
            .thenReturn(File(null as File?, thumbUri))
        whenever(typedFileNode.name).thenReturn("File name")
        whenever(typedFileNode.id).thenReturn(nodeId)
        whenever(getNodeAccessPermission.invoke(nodeId)).thenReturn(AccessPermission.READ)
        whenever(getPreview.invoke(anyLong())).thenReturn(null)
        whenever(typedFileNode.thumbnailPath).thenReturn(null)
        whenever(typedFileNode.hasPreview).thenReturn(false)
        whenever(isAvailableOffline.invoke(any())).thenReturn(true)
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
                underTest.setNode(node.handle)
                Truth.assertThat(underTest.uiState.value.historyVersions).isEqualTo(n)
            }
        }

    @Test
    fun `test that viewModel state's isNodeInInbox property reflects the value of the isNodeInInbox use case after updating the node`() =
        runBlocking {
            suspend fun verify(isNodeInInbox: Boolean) {
                whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(isNodeInInbox)
                underTest.setNode(node.handle)
                Truth.assertThat(underTest.uiState.value.isNodeInInbox).isEqualTo(isNodeInInbox)
            }
            verify(true)
            verify(false)
        }

    @Test
    fun `test that viewModel state's isNodeInRubbish property reflects the value of the isNodeInRubbish use case after updating the node`() =
        runTest {
            suspend fun verify(isNodeInRubbish: Boolean) {
                whenever(isNodeInRubbish(NODE_HANDLE)).thenReturn(isNodeInRubbish)
                underTest.setNode(node.handle)
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
            underTest.setNode(node.handle)
            Truth.assertThat(underTest.uiState.value.showHistoryVersions).isEqualTo(true)
        }

    @Test
    fun `test showHistoryVersions is true if the node contains more than one version and is not in the inbox`() =
        runBlocking {
            whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(2)
            whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(false)
            underTest.setNode(node.handle)
            Truth.assertThat(underTest.uiState.value.showHistoryVersions).isEqualTo(true)
        }

    @Test
    fun `test showHistoryVersions is false if the node contains one version but is in the inbox`() =
        runBlocking {
            whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(1)
            whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(true)
            underTest.setNode(node.handle)
            Truth.assertThat(underTest.uiState.value.showHistoryVersions).isEqualTo(false)

        }

    @Test
    fun `test showHistoryVersions is false if the node contains no versions and is not in the inbox`() =
        runBlocking {
            whenever(getFileHistoryNumVersions(NODE_HANDLE)).thenReturn(0)
            whenever(isNodeInInbox(NODE_HANDLE)).thenReturn(false)
            underTest.setNode(node.handle)
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
            underTest.setNode(node.handle)
            underTest.moveNodeCheckingCollisions(parentId)
            testEventIsOfType(FileInfoOneOffViewEvent.CollisionDetected::class.java)
        }

    @Test
    fun `test CollisionDetected event is launched when a collision is found while copying`() =
        runTest {
            mockCollisionCopying()
            underTest.setNode(node.handle)
            underTest.copyNodeCheckingCollisions(parentId)
            testEventIsOfType(FileInfoOneOffViewEvent.CollisionDetected::class.java)
        }

    @Test
    fun `test GeneralError event is launched when an unknown error is returned when check collision`() =
        runTest {
            whenever(checkNameCollision(nodeId, parentId, NameCollisionType.COPY))
                .thenThrow(RuntimeException::class.java)
            underTest.setNode(node.handle)
            underTest.copyNodeCheckingCollisions(parentId)
            testEventIsOfType(FileInfoOneOffViewEvent.GeneralError::class.java)
        }

    @Test
    fun `test FinishedMoving event is launched without exceptions when the move finished successfully`() =
        runTest {
            mockMoveSuccess()
            underTest.setNode(node.handle)
            underTest.moveNodeCheckingCollisions(parentId)
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Moving::class.java)?.also {
                Truth.assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedMoving event is launched with the proper exceptions when the move finished with an error`() =
        runTest {
            mockMoveFailure()
            underTest.setNode(node.handle)
            underTest.moveNodeCheckingCollisions(parentId)
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Moving::class.java)?.also {
                Truth.assertThat(it.exception).isNotNull()
            }
        }

    @Test
    fun `test FinishedMovingToRubbish event is launched without exceptions when the move finished successfully`() =
        runTest {
            mockMoveToRubbishSuccess()
            underTest.setNode(node.handle)
            underTest.removeNode()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.MovingToRubbish::class.java)?.also {
                Truth.assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedMovingToRubbish event is launched with the proper exceptions when the move finished with an error`() =
        runTest {
            mockMoveToRubbishFailure()
            underTest.setNode(node.handle)
            underTest.removeNode()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.MovingToRubbish::class.java)?.also {
                Truth.assertThat(it.exception).isNotNull()
            }
        }

    @Test
    fun `test FinishedDeleting event is launched without exceptions when the delete finished successfully`() =
        runTest {
            mockDeleteSuccess()
            underTest.setNode(node.handle)
            underTest.removeNode()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Deleting::class.java)?.also {
                Truth.assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedDeleting event is launched with the proper exceptions when the delete finished with an error`() =
        runTest {
            mockDeleteFailure()
            underTest.setNode(node.handle)
            underTest.removeNode()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Deleting::class.java)?.also {
                Truth.assertThat(it.exception).isNotNull()
            }
        }

    @Test
    fun `test FinishedCopying event is launched without exceptions when the move finished successfully`() =
        runTest {
            mockCopySuccess()
            underTest.setNode(node.handle)
            underTest.copyNodeCheckingCollisions(parentId)
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Copying::class.java)?.also {
                Truth.assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedCopying event is launched with the proper exceptions when the move finished with an error`() =
        runTest {
            mockCopyFailure()
            underTest.setNode(node.handle)
            underTest.copyNodeCheckingCollisions(parentId)
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Copying::class.java)?.also {
                Truth.assertThat(it.exception).isNotNull()
            }
        }

    @Test
    fun `test FinishedDeletingVersions event is launched without exceptions when the delete versions finished successfully`() =
        runTest {
            mockDeleteVersionsSuccess()
            underTest.setNode(node.handle)
            underTest.deleteHistoryVersions()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.DeletingVersions::class.java)?.also {
                Truth.assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedDeletingVersions event is launched with the proper exceptions when the delete versions finished with an error`() =
        runTest {
            mockDeleteVersionsFailure(null)
            underTest.setNode(node.handle)
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
            underTest.setNode(node.handle)
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
            underTest.setNode(node.handle)
            testProgressIsSetWhileCopyingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while copying with an error, and unset at the end`() =
        runTest {
            mockCopyFailure()
            underTest.setNode(node.handle)
            testProgressIsSetWhileCopyingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while copying and Name conflict found, and unset at the end`() =
        runTest {
            mockCollisionCopying()
            underTest.setNode(node.handle)
            testProgressIsSetWhileCopyingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while moving successfully, and unset at the end`() =
        runTest {
            mockMoveSuccess()
            underTest.setNode(node.handle)
            testProgressIsSetWhileMovingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while moving with an error, and unset at the end`() =
        runTest {
            mockMoveFailure()
            underTest.setNode(node.handle)
            testProgressIsSetWhileMovingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while moving and Name conflict found, and unset at the end`() =
        runTest {
            mockCollisionMoving()
            underTest.setNode(node.handle)
            testProgressIsSetWhileMovingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while moving to rubbish successfully, and unset at the end`() =
        runTest {
            mockMoveToRubbishSuccess()
            underTest.setNode(node.handle)
            testProgressIsSetWhileMovingToRubbishBinAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while moving to rubbish with an error, and unset at the end`() =
        runTest {
            mockMoveToRubbishFailure()
            underTest.setNode(node.handle)
            testProgressIsSetWhileMovingToRubbishBinAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while deleting successfully, and unset at the end`() =
        runTest {
            mockDeleteSuccess()
            underTest.setNode(node.handle)
            testProgressIsSetWhileDeletingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while deleting with an error, and unset at the end`() =
        runTest {
            mockDeleteSuccess()
            underTest.setNode(node.handle)
            testProgressIsSetWhileDeletingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while deleting versions successfully, and unset at the end`() =
        runTest {
            mockDeleteVersionsSuccess()
            underTest.setNode(node.handle)
            testProgressIsSetWhileDeletingVersionsAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while deleting versions with an error, and unset at the end`() =
        runTest {
            mockDeleteVersionsFailure()
            underTest.setNode(node.handle)
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
        whenever(typedFileNode.hasPreview).thenReturn(true)
        whenever(getPreview.invoke(NODE_HANDLE)).thenReturn(previewFile)
        whenever(typedFileNode.thumbnailPath).thenReturn(null)
        underTest.setNode(node.handle)
        underTest.uiState.mapNotNull { it.actualPreviewUriString }.test {
            val state = awaitItem()
            Truth.assertThat(state).isEqualTo(previewUri)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test thumbnail is assigned when node is updated and there are no preview`() = runTest {
        whenever(typedFileNode.hasPreview).thenReturn(false)
        whenever(typedFileNode.thumbnailPath).thenReturn(thumbUri)
        underTest.setNode(node.handle)
        underTest.uiState.mapNotNull { it.actualPreviewUriString }.test {
            val state = awaitItem()
            Truth.assertThat(state).isEqualTo("file:$thumbUri")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test preview has priority over thumbnail`() = runTest {
        whenever(getPreview.invoke(NODE_HANDLE)).thenReturn(previewFile)
        whenever(typedFileNode.thumbnailPath).thenReturn(thumbUri)
        whenever(typedFileNode.hasPreview).thenReturn(true)
        underTest.setNode(node.handle)
        underTest.uiState.mapNotNull { it.actualPreviewUriString }.test {
            val state = awaitItem()
            Truth.assertThat(state).isEqualTo(previewUri)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test getContactItemFromInShareFolder is invoked when the node is a Folder`() = runTest {
        val folderNode = mockFolder()
        underTest.setNode(node.handle)
        //first quick fetch
        verify(getContactItemFromInShareFolder).invoke(folderNode, false)
        //second not cached slow fetch
        verify(getContactItemFromInShareFolder).invoke(folderNode, true)
    }

    @Test
    fun `test monitorNodeUpdatesById updates owner`() = runTest {
        val folderNode = mockFolder()
        whenever(monitorNodeUpdatesById.invoke(folderNode.id)).thenReturn(
            flowOf(listOf(NodeChanges.Owner))
        )
        underTest.setNode(node.handle)
        //check 2 invocations: first invocation when node is set, second one the update itself
        verify(getContactItemFromInShareFolder, times(2)).invoke(folderNode, true)
    }

    @Test
    fun `test getNodeAccessPermission is fetched if getContactItemFromInShareFolder returns ContactItem`() =
        runTest {
            val expected = AccessPermission.FULL
            val folderNode = mockFolder()
            whenever(getContactItemFromInShareFolder.invoke(folderNode, false)).thenReturn(mock())
            whenever(getNodeAccessPermission.invoke(folderNode.id)).thenReturn(expected)
            underTest.setNode(folderNode.id.longValue)
            Truth.assertThat(underTest.uiState.value.accessPermission)
                .isEqualTo(expected)
            verify(getNodeAccessPermission, times(1)).invoke(nodeId)
        }

    @Test
    fun `test getNodeAccessPermission is fetched again if monitorNodeUpdatesById updates In share`() =
        runTest {
            val expectedFirst = AccessPermission.FULL
            val expectedChanged = AccessPermission.READ
            val folderNode = mockFolder()
            whenever(getContactItemFromInShareFolder.invoke(folderNode, false)).thenReturn(mock())
            whenever(getNodeAccessPermission.invoke(nodeId))
                .thenReturn(expectedFirst)
                .thenReturn(expectedChanged)
            whenever(monitorNodeUpdatesById.invoke(nodeId)).thenReturn(
                flowOf(listOf(NodeChanges.Inshare))
            )
            underTest.setNode(node.handle)
            Truth.assertThat(underTest.uiState.value.accessPermission)
                .isEqualTo(expectedChanged)
            verify(getNodeAccessPermission, times(2)).invoke(nodeId)
        }

    @Test
    fun `test getNodeLocationInfo fetches node location when its set`() = runTest {
        underTest.setNode(node.handle)
        verify(getNodeLocationInfo, times(1)).invoke(typedFileNode)
    }

    @Test
    fun `test getNodeLocationInfo fetches node location when its parent is updated`() = runTest {
        whenever(monitorNodeUpdatesById.invoke(nodeId))
            .thenReturn(
                flowOf(listOf(NodeChanges.Parent))
            )
            .thenReturn(emptyFlow()) //second time we don't want to emit another update to avoid a circular call in this test
        underTest.setNode(node.handle)
        verify(getNodeLocationInfo, times(2)).invoke(typedFileNode)
    }

    @Test
    fun `test getOutShares is fetched when node is set`() = runTest {
        underTest.setNode(node.handle)
        verify(getOutShares, times(1)).invoke(nodeId)
    }

    @Test
    fun `test getOutShares result is set on uiState`() = runTest {
        val expected = mock<List<MegaShare>>()
        whenever(getOutShares.invoke(nodeId)).thenReturn(expected)
        underTest.setNode(node.handle)
        Truth.assertThat(underTest.uiState.value.outShares).isEqualTo(expected)
    }

    @Test
    fun `test getOutShares is fetched when out shares update is received`() = runTest {
        whenever(monitorNodeUpdatesById.invoke(nodeId)).thenReturn(
            flowOf(listOf(NodeChanges.Outshare))
        )
        underTest.setNode(node.handle)
        verify(getOutShares, times(2)).invoke(nodeId)
    }

    @Test
    fun `test getOutShares is fetched when contacts update is received and there are out shares`() =
        runTest {
            val outShares = mock<List<MegaShare>>()
            whenever(getOutShares.invoke(nodeId)).thenReturn(outShares)
            val updateChanges = mapOf(Pair(UserId(1L), listOf(UserChanges.Alias)))
            val update = mock<UserUpdate> {
                on { changes }.thenReturn(updateChanges)
            }
            whenever(monitorContactUpdates.invoke()).thenReturn(flowOf(update))
            underTest.setNode(node.handle)
            verify(getOutShares, times(2)).invoke(nodeId)
        }

    @Test
    fun `test isAvailableOffline result is set on uiState`() = runTest {
        val expected = true
        whenever(isAvailableOffline.invoke(typedFileNode)).thenReturn(expected)
        underTest.setNode(node.handle)
        Truth.assertThat(underTest.uiState.value.isAvailableOffline).isEqualTo(expected)
    }

    @Test
    fun `test availableOfflineChanged changes ui state accordingly`() = runTest {
        mockMonitorStorageStateEvent(StorageState.Green)
        val expected = true
        whenever(isAvailableOffline.invoke(any())).thenReturn(false)
        underTest.setNode(node.handle)
        underTest.availableOfflineChanged(expected, activity)
        underTest.uiState.mapNotNull { it.isAvailableOffline }.test {
            Truth.assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
        verify(setNodeAvailableOffline, times(1)).invoke(nodeId, expected, activity)
    }

    @Test
    fun `test availableOfflineChanged does nothing if getState()`() = runTest {
        mockMonitorStorageStateEvent(StorageState.PayWall)
        whenever(isAvailableOffline.invoke(typedFileNode)).thenReturn(false)
        underTest.setNode(node.handle)
        underTest.availableOfflineChanged(true, activity)
        verifyNoInteractions(setNodeAvailableOffline)
    }

    private fun mockMonitorStorageStateEvent(state: StorageState) {
        val storageStateEvent = StorageStateEvent(
            1L, "", 1L, "", EventType.Storage,
            state,
        )
        whenever(monitorStorageStateEvent.invoke()).thenReturn(MutableStateFlow(storageStateEvent))
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
        underTest.setNode(node.handle)
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
        underTest.setNode(node.handle)
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

    private suspend fun mockFolder() = mock<TypedFolderNode> {
        on { id }.thenReturn(nodeId)
        on { name }.thenReturn("Folder name")
    }.also { folderNode ->
        whenever(getNodeById.invoke(nodeId)).thenReturn(folderNode)
        whenever(getFolderTreeInfo.invoke(folderNode)).thenReturn(mock())
        whenever(getContactItemFromInShareFolder.invoke(any(), any())).thenReturn(mock())
    }

    companion object {
        private const val NODE_HANDLE = 10L
        private const val PARENT_NODE_HANDLE = 12L
        private val nodeId = NodeId(NODE_HANDLE)
        private val parentId = NodeId(PARENT_NODE_HANDLE)
        private const val thumbUri = "/thumb"
        private const val previewUri = "/preview"
    }
}