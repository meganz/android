package test.mega.privacy.android.app.presentation.contact

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.contact.ContactFileListViewModel
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ContactFileListViewModelTest {
    private lateinit var underTest: ContactFileListViewModel
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase = mock()
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase = mock()
    private val moveNodesUseCase: MoveNodesUseCase = mock()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase,
            moveNodesToRubbishUseCase,
            checkNodesNameCollisionUseCase,
            moveNodesUseCase
        )
    }

    private fun initTestClass() {
        underTest = ContactFileListViewModel(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            moveNodesToRubbishUseCase = moveNodesToRubbishUseCase,
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            moveNodesUseCase = moveNodesUseCase
        )
    }

    @Test
    fun `test that moveRequestResult updated correctly when calling moveNodes failed`() =
        runTest {
            initTestClass()
            val nodes = mapOf(1L to 100L, 2L to 100L)
            whenever(moveNodesUseCase(nodes)).thenThrow(RuntimeException::class.java)

            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.moveRequestResult).isNull()
                underTest.moveNodes(nodes)
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.moveRequestResult).isNotNull()
                Truth.assertThat(updatedState.moveRequestResult?.isFailure).isTrue()
            }
        }


    @Test
    fun `test that moveRequestResult updated correctly when calling moveNodes successfully`() =
        runTest {
            initTestClass()
            val result = mock<MoveRequestResult.GeneralMovement>()
            val nodes = mapOf(1L to 100L, 2L to 100L)
            whenever(moveNodesUseCase(nodes)).thenReturn(result)

            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.moveRequestResult).isNull()
                underTest.moveNodes(nodes)
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.moveRequestResult).isNotNull()
                Truth.assertThat(updatedState.moveRequestResult?.isSuccess).isTrue()
            }
        }

    @Test
    fun `test that nodeNameCollisionResult updated when calling checkNodesNameCollisionUseCase successfully`() =
        runTest {
            initTestClass()
            val nodes = listOf(1L, 2L)
            val targetNode = 100L
            val result = mock<NodeNameCollisionResult>()
            whenever(
                checkNodesNameCollisionUseCase(
                    mapOf(1L to 100L, 2L to 100L),
                    NodeNameCollisionType.MOVE
                )
            ).thenReturn(result)

            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.nodeNameCollisionResult).isNull()
                underTest.checkMoveNodesNameCollision(nodes, targetNode)
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.nodeNameCollisionResult).isNotNull()
            }
        }

    @Test
    fun `test that moveRequestResult updated when calling markHandleMoveRequestResult`() =
        runTest {
            initTestClass()
            val result = mock<MoveRequestResult.GeneralMovement>()
            val nodes = mapOf(1L to 100L, 2L to 100L)
            whenever(moveNodesUseCase(nodes)).thenReturn(result)

            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.moveRequestResult).isNull()
                underTest.moveNodes(nodes)
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.moveRequestResult).isNotNull()
                Truth.assertThat(updatedState.moveRequestResult?.isSuccess).isTrue()
                underTest.markHandleMoveRequestResult()
                Truth.assertThat(awaitItem().moveRequestResult).isNull()
            }
        }
}
