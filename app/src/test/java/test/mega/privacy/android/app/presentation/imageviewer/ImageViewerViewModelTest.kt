package test.mega.privacy.android.app.presentation.imageviewer

import android.content.Context
import com.jraska.livedata.test
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.imageviewer.ImageViewerViewModel
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.usecase.GetGlobalChangesUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.IsUserLoggedIn
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ImageViewerViewModelTest {
    private lateinit var underTest: ImageViewerViewModel
    private lateinit var checkNameCollision: CheckNameCollision
    private lateinit var copyNodeUseCase: CopyNodeUseCase
    private lateinit var moveNodeUseCase: MoveNodeUseCase
    private lateinit var isUserLoggedIn: IsUserLoggedIn
    private lateinit var getGlobalChangesUseCase: GetGlobalChangesUseCase
    private lateinit var context: Context

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(StandardTestDispatcher())
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @BeforeEach
    fun setUp() {
        checkNameCollision = mock()
        copyNodeUseCase = mock()
        moveNodeUseCase = mock()
        isUserLoggedIn = mock()
        getGlobalChangesUseCase = mock()

        whenever(getGlobalChangesUseCase.get()).thenAnswer {
            Flowable.just(
                GetGlobalChangesUseCase.Result.OnNodesUpdate(
                    emptyList()
                )
            )
        }
        context = mock()
        underTest = ImageViewerViewModel(
            getImageUseCase = mock(),
            getImageHandlesUseCase = mock(),
            getGlobalChangesUseCase = getGlobalChangesUseCase,
            getNodeUseCase = mock(),
            exportNodeUseCase = mock(),
            cancelTransferUseCase = mock(),
            isUserLoggedInUseCase = isUserLoggedIn,
            deleteChatMessageUseCase = mock(),
            areTransfersPausedUseCase = mock(),
            copyNodeUseCase = copyNodeUseCase,
            moveNodeUseCase = moveNodeUseCase,
            removeNodeUseCase = mock(),
            checkNameCollision = checkNameCollision,
            moveNodeToRubbishByHandle = mock(),
            context = context,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test copy complete snack bar is shown when file is copied to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(isUserLoggedIn.invoke()).thenReturn(true)
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.COPY,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            whenever(context.getString(R.string.context_correctly_copied)).thenReturn("Copied")
            whenever(
                copyNodeUseCase(
                    nodeToCopy = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode), newNodeName = null
                )
            ).thenReturn(NodeId(selectedNode))
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            testScheduler.advanceUntilIdle()
            underTest.onSnackBarMessage().test().assertValue("Copied")
        }

    @Test
    fun `test that onExceptionThrown is triggered when copy failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(isUserLoggedIn.invoke()).thenReturn(true)
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.COPY,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            val runtimeException = RuntimeException("Copy node failed")
            whenever(
                copyNodeUseCase(
                    nodeToCopy = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode), newNodeName = null
                )
            ).thenAnswer { throw runtimeException }
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onCopyMoveException().test().assertValue(runtimeException)
        }

    @Test
    fun `test move complete snack bar is shown when file is moved to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(isUserLoggedIn.invoke()).thenReturn(true)
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.MOVE,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            whenever(context.getString(R.string.context_correctly_moved)).thenReturn("Moved")
            whenever(
                moveNodeUseCase(
                    nodeToMove = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode)
                )
            ).thenReturn(NodeId(selectedNode))
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackBarMessage().test().assertValue("Moved")
        }

    @Test
    fun `test that onExceptionThrown is triggered when move failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(isUserLoggedIn.invoke()).thenReturn(true)
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.MOVE,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            val runtimeException = RuntimeException("Move node failed")
            whenever(
                moveNodeUseCase(
                    nodeToMove = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode)
                )
            ).thenThrow(runtimeException)
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onCopyMoveException().test().assertValue(runtimeException)
        }
}