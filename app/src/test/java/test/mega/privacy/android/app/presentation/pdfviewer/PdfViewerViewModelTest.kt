package test.mega.privacy.android.app.presentation.pdfviewer

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerViewModel
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PdfViewerViewModelTest {

    private lateinit var underTest: PdfViewerViewModel
    private val checkNameCollision = mock<CheckNameCollision>()
    private val copyNodeUseCase = mock<CopyNodeUseCase>()
    private val moveNodeUseCase = mock<MoveNodeUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = PdfViewerViewModel(
            moveNodeUseCase = moveNodeUseCase,
            checkNameCollision = checkNameCollision,
            copyNodeUseCase = copyNodeUseCase,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test move complete snack bar is shown when file is moved to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.MOVE,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
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
            underTest.uiState.test {
                val actual = awaitItem()
                Truth.assertThat(actual.snackBarMessage).isEqualTo(R.string.context_correctly_moved)
            }
        }

    @Test
    fun `test that onExceptionThrown is triggered when move failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.MOVE,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            whenever(
                moveNodeUseCase(
                    nodeToMove = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode)
                )
            ).thenThrow(IllegalStateException())
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            underTest.uiState.test {
                val actual = awaitItem()
                Truth.assertThat(actual.nodeMoveError)
                    .isInstanceOf(IllegalStateException::class.java)
            }
        }

    @Test
    fun `test copy complete snack bar is shown when file is copied to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.COPY,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
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
            underTest.uiState.test {
                val actual = awaitItem()
                Truth.assertThat(actual.snackBarMessage)
                    .isEqualTo(R.string.context_correctly_copied)
            }
        }

    @Test
    fun `test that onExceptionThrown is triggered when copy failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.COPY,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            whenever(
                copyNodeUseCase(
                    nodeToCopy = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode), newNodeName = null
                )
            ).thenThrow(IllegalStateException())
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            underTest.uiState.test {
                val actual = awaitItem()
                Truth.assertThat(actual.nodeCopyError)
                    .isInstanceOf(IllegalStateException::class.java)
            }
        }
}