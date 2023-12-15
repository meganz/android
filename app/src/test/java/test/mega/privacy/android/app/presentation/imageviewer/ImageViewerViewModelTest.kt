package test.mega.privacy.android.app.presentation.imageviewer

import android.content.Context
import com.google.common.truth.Truth
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.observeOnce
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.imageviewer.ImageViewerViewModel
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.imageviewer.usecase.GetImageHandlesUseCase
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.usecase.GetGlobalChangesUseCase
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.LegacyCopyNodeUseCase
import mega.privacy.android.app.usecase.chat.DeleteChatMessageUseCase
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.IsUserLoggedIn
import mega.privacy.android.domain.usecase.filenode.DeleteNodeByHandleUseCase
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishBinUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageByAlbumImportNodeUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageByNodeHandleUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageByNodePublicLinkUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageByOfflineNodeHandleUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageForChatMessageUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageFromFileUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.DisableExportUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetNumPendingDownloadsNonBackgroundUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ResetTotalDownloadsUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ImageViewerViewModelTest {
    private lateinit var underTest: ImageViewerViewModel
    private val checkNameCollision = mock<CheckNameCollision>()
    private val checkNameCollisionUseCase = mock<CheckNameCollisionUseCase>()
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val copyNodeUseCase = mock<CopyNodeUseCase>()
    private val legacyCopyNodeUseCase = mock<LegacyCopyNodeUseCase>()
    private val moveNodeUseCase = mock<MoveNodeUseCase>()
    private val isUserLoggedIn = mock<IsUserLoggedIn>()
    private val getGlobalChangesUseCase = mock<GetGlobalChangesUseCase>()
    private val context = mock<Context>()
    private val disableExportUseCase = mock<DisableExportUseCase>()
    private val getImageByNodeHandleUseCase = mock<GetImageByNodeHandleUseCase>()
    private val getImageByNodePublicLinkUseCase = mock<GetImageByNodePublicLinkUseCase>()
    private val getImageForChatMessageUseCase = mock<GetImageForChatMessageUseCase>()
    private val getImageByOfflineNodeHandleUseCase = mock<GetImageByOfflineNodeHandleUseCase>()
    private val getImageFromFileUseCase = mock<GetImageFromFileUseCase>()
    private val getNumPendingDownloadsNonBackgroundUseCase =
        mock<GetNumPendingDownloadsNonBackgroundUseCase>()
    private val resetTotalDownloadsUseCase = mock<ResetTotalDownloadsUseCase>()
    private val getImageHandlesUseCase = mock<GetImageHandlesUseCase>()
    private val getNodeUseCase = mock<GetNodeUseCase>()
    private val exportNodeUseCase = mock<ExportNodeUseCase>()
    private val cancelTransferByTagUseCase = mock<CancelTransferByTagUseCase>()
    private val deleteChatMessageUseCase = mock<DeleteChatMessageUseCase>()
    private val areTransfersPausedUseCase = mock<AreTransfersPausedUseCase>()
    private val deleteNodeByHandleUseCase = mock<DeleteNodeByHandleUseCase>()
    private val moveNodeToRubbishBinUseCase = mock<MoveNodeToRubbishBinUseCase>()
    private val getImageByAlbumImportNodeUseCase = mock<GetImageByAlbumImportNodeUseCase>()
    private val scheduler = Schedulers.trampoline()
    private val selectedNodeId = mock<NodeId> {
        on { longValue } doReturn 123456L
    }
    private val targetNodeId = mock<NodeId> {
        on { longValue } doReturn 654321L
    }
    private val selectedNode = mock<MegaNode> {
        on { handle } doReturn 123456L
    }
    private val targetNode = mock<MegaNode> {
        on { handle } doReturn 654321L
    }
    private val megaNodeItem = mock<MegaNodeItem> {
        on { handle } doReturn 123456L
        on { node } doReturn selectedNode
    }
    private val imageItem = mock<ImageItem.Node> {
        on { nodeItem } doReturn megaNodeItem
        on { getNodeHandle() } doReturn 123456L
        on { id } doReturn 123456L
    }


    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { scheduler }
    }

    @BeforeEach
    fun setUp() {
        reset(
            checkNameCollision,
            checkNameCollisionUseCase,
            getNodeByHandle,
            copyNodeUseCase,
            legacyCopyNodeUseCase,
            moveNodeUseCase,
            isUserLoggedIn,
            getGlobalChangesUseCase,
            context,
            disableExportUseCase,
            getImageByNodeHandleUseCase,
            getImageByNodePublicLinkUseCase,
            getImageForChatMessageUseCase,
            getImageByOfflineNodeHandleUseCase,
            getImageFromFileUseCase,
            getNumPendingDownloadsNonBackgroundUseCase,
            resetTotalDownloadsUseCase,
            getImageHandlesUseCase,
            getNodeUseCase,
            exportNodeUseCase,
            cancelTransferByTagUseCase,
            deleteChatMessageUseCase,
            areTransfersPausedUseCase,
            deleteNodeByHandleUseCase,
            moveNodeToRubbishBinUseCase,
            getImageByAlbumImportNodeUseCase,
        )
        stubCommon()
    }

    private fun stubCommon() {
        runBlocking {
            whenever(isUserLoggedIn.invoke()).thenReturn(true)
            whenever(getGlobalChangesUseCase.get()).thenAnswer {
                Flowable.just(
                    GetGlobalChangesUseCase.Result.OnNodesUpdate(
                        emptyList()
                    )
                )
            }
            whenever(
                getImageHandlesUseCase.get(
                    nodeHandles = longArrayOf(123456),
                    isOffline = false
                )
            ).thenReturn(Single.just(listOf(imageItem)))
        }
    }

    private fun initViewModel() {
        underTest = ImageViewerViewModel(
            getImageByNodeHandleUseCase = getImageByNodeHandleUseCase,
            getImageByNodePublicLinkUseCase = getImageByNodePublicLinkUseCase,
            getImageForChatMessageUseCase = getImageForChatMessageUseCase,
            getImageByOfflineNodeHandleUseCase = getImageByOfflineNodeHandleUseCase,
            getImageFromFileUseCase = getImageFromFileUseCase,
            getNumPendingDownloadsNonBackgroundUseCase = getNumPendingDownloadsNonBackgroundUseCase,
            resetTotalDownloadsUseCase = resetTotalDownloadsUseCase,
            getImageHandlesUseCase = getImageHandlesUseCase,
            getGlobalChangesUseCase = getGlobalChangesUseCase,
            getNodeUseCase = getNodeUseCase,
            exportNodeUseCase = exportNodeUseCase,
            disableExportUseCase = disableExportUseCase,
            cancelTransferByTagUseCase = cancelTransferByTagUseCase,
            isUserLoggedInUseCase = isUserLoggedIn,
            deleteChatMessageUseCase = deleteChatMessageUseCase,
            areTransfersPausedUseCase = areTransfersPausedUseCase,
            copyNodeUseCase = copyNodeUseCase,
            moveNodeUseCase = moveNodeUseCase,
            deleteNodeByHandleUseCase = deleteNodeByHandleUseCase,
            checkNameCollision = checkNameCollision,
            getNodeByHandle = getNodeByHandle,
            legacyCopyNodeUseCase = legacyCopyNodeUseCase,
            checkNameCollisionUseCase = checkNameCollisionUseCase,
            moveNodeToRubbishBinUseCase = moveNodeToRubbishBinUseCase,
            getImageByAlbumImportNodeUseCase = getImageByAlbumImportNodeUseCase,
            context = context,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
        RxAndroidPlugins.reset()
    }

    @Test
    internal fun `test that copy complete snack bar is shown when file is imported to different directory`() =
        runTest {
            runBlocking {
                whenever(getNodeByHandle(654321)).thenReturn(targetNode)
                whenever(
                    legacyCopyNodeUseCase.copy(
                        node = selectedNode,
                        parentHandle = 654321
                    )
                ).thenReturn(Completable.complete())
            }
            whenever(
                checkNameCollisionUseCase.check(
                    node = selectedNode,
                    parentNode = targetNode,
                    type = NameCollisionType.COPY,
                )
            ).thenReturn(Single.error(MegaNodeException.ChildDoesNotExistsException()))
            whenever(context.getString(R.string.context_correctly_copied)).thenReturn("Copied")
            initViewModel()
            underTest.retrieveImages(longArrayOf(123456L), 123456L, false)
            scheduler.scheduleDirect({
                underTest.importNode(
                    newParentHandle = targetNode.handle,
                )
            }, 100, TimeUnit.MILLISECONDS)
            underTest.onSnackBarMessage().observeOnce {
                Truth.assertThat(it).isEqualTo("Copied")
            }

        }

    @Test
    internal fun `test that onExceptionThrown is triggered when import failed`() =
        runTest {
            whenever(getNodeByHandle(targetNode.handle)).thenReturn(targetNode)
            whenever(
                checkNameCollisionUseCase.check(
                    node = selectedNode,
                    parentNode = targetNode,
                    type = NameCollisionType.COPY,
                )
            ).thenReturn(Single.error(MegaNodeException.ChildDoesNotExistsException()))
            val runtimeException = RuntimeException("Import node failed")
            whenever(
                legacyCopyNodeUseCase.copy(
                    node = selectedNode,
                    parentHandle = targetNode.handle
                )
            ).thenReturn(Completable.error(runtimeException))
            initViewModel()
            underTest.retrieveImages(longArrayOf(selectedNode.handle), selectedNode.handle, false)
            scheduler.scheduleDirect({
                underTest.importNode(
                    newParentHandle = targetNode.handle,
                )
            }, 100, TimeUnit.MILLISECONDS)
            underTest.onCopyMoveException().observeOnce {
                Truth.assertThat(it).isEqualTo(runtimeException)
            }
        }

    @Test
    internal fun `test copy complete snack bar is shown when file is copied to different directory`() =
        runTest {
            copyNodeUseCase.stub {
                onBlocking {
                    invoke(
                        nodeToCopy = selectedNodeId,
                        newNodeParent = targetNodeId,
                        newNodeName = null
                    )
                } doReturn selectedNodeId
            }
            checkNameCollision.stub {
                onBlocking {
                    invoke(
                        nodeHandle = selectedNodeId,
                        parentHandle = targetNodeId,
                        type = NameCollisionType.COPY,
                    )
                } doThrow MegaNodeException.ChildDoesNotExistsException()
            }

            whenever(context.getString(R.string.context_correctly_copied)).thenReturn("Copied")
            initViewModel()
            underTest.retrieveImages(longArrayOf(selectedNode.handle), selectedNode.handle, false)
            scheduler.scheduleDirect({
                underTest.copyNode(
                    nodeHandle = selectedNode.handle,
                    newParentHandle = targetNode.handle,
                )
            }, 100, TimeUnit.MILLISECONDS)
            underTest.onSnackBarMessage().observeOnce {
                Truth.assertThat(it).isEqualTo("Copied")
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when copy failed`() =
        runTest {
            val runtimeException = RuntimeException("Copy node failed")
            copyNodeUseCase.stub {
                onBlocking {
                    invoke(
                        nodeToCopy = selectedNodeId,
                        newNodeParent = targetNodeId,
                        newNodeName = null
                    )
                } doThrow runtimeException
            }
            checkNameCollision.stub {
                onBlocking {
                    invoke(
                        nodeHandle = selectedNodeId,
                        parentHandle = targetNodeId,
                        type = NameCollisionType.COPY,
                    )
                } doThrow MegaNodeException.ChildDoesNotExistsException()
            }
            initViewModel()
            underTest.retrieveImages(longArrayOf(selectedNode.handle), selectedNode.handle, false)
            scheduler.scheduleDirect({
                underTest.copyNode(
                    nodeHandle = selectedNode.handle,
                    newParentHandle = targetNode.handle,
                )
            }, 100, TimeUnit.MILLISECONDS)
            underTest.onCopyMoveException().observeOnce {
                Truth.assertThat(it).isEqualTo(runtimeException)
            }
        }

    @Test
    internal fun `test move complete snack bar is shown when file is moved to different directory`() =
        runTest {
            whenever(
                checkNameCollision(
                    nodeHandle = selectedNodeId,
                    parentHandle = targetNodeId,
                    type = NameCollisionType.MOVE,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            whenever(context.getString(R.string.context_correctly_moved)).thenReturn("Moved")
            whenever(
                moveNodeUseCase(
                    nodeToMove = selectedNodeId,
                    newNodeParent = targetNodeId
                )
            ).thenReturn(selectedNodeId)
            initViewModel()
            underTest.retrieveImages(longArrayOf(selectedNode.handle), selectedNode.handle, false)
            scheduler.scheduleDirect({
                underTest.moveNode(
                    nodeHandle = selectedNode.handle,
                    newParentHandle = targetNode.handle,
                )
            }, 100, TimeUnit.MILLISECONDS)
            underTest.onSnackBarMessage().observeOnce {
                Truth.assertThat(it).isEqualTo("Moved")
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when move failed`() =
        runTest {
            whenever(
                checkNameCollision(
                    nodeHandle = selectedNodeId,
                    parentHandle = targetNodeId,
                    type = NameCollisionType.MOVE,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            val runtimeException = RuntimeException("Move node failed")
            whenever(
                moveNodeUseCase(
                    nodeToMove = selectedNodeId,
                    newNodeParent = targetNodeId
                )
            ).thenThrow(runtimeException)
            initViewModel()
            underTest.retrieveImages(longArrayOf(selectedNode.handle), selectedNode.handle, false)
            scheduler.scheduleDirect({
                underTest.moveNode(
                    nodeHandle = selectedNodeId.longValue,
                    newParentHandle = targetNodeId.longValue,
                )
            }, 100, TimeUnit.MILLISECONDS)
            underTest.onCopyMoveException().observeOnce {
                Truth.assertThat(it).isEqualTo(runtimeException)
            }
        }

    @Test
    internal fun `test that link removed snack bar is shown when removeLink is called and operation is success`() =
        runTest {
            val nodeHandle = 1L
            val expectedMessage = "Link removed"
            whenever(context.resources).thenReturn(mock())
            whenever(
                context.resources.getQuantityString(
                    R.plurals.context_link_removal_success,
                    1
                )
            ).thenReturn(expectedMessage)
            whenever(disableExportUseCase(NodeId(nodeHandle))).thenReturn(Unit)
            initViewModel()
            underTest.removeLink(nodeHandle)
            underTest.onSnackBarMessage().observeOnce {
                Truth.assertThat(it).isEqualTo(expectedMessage)
            }
        }
}