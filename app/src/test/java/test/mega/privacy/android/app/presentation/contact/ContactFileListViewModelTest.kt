package test.mega.privacy.android.app.presentation.contact

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.contact.ContactFileListViewModel
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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
    private val copyNodesUseCase: CopyNodesUseCase = mock()
    private val nodeNameCollision = mock<NodeNameCollision> {
        on { nodeHandle }.thenReturn(2L)
        on { collisionHandle }.thenReturn(123L)
        on { name }.thenReturn("node")
        on { size }.thenReturn(199L)
        on { childFileCount }.thenReturn(5)
        on { childFolderCount }.thenReturn(10)
        on { lastModified }.thenReturn(234L)
        on { parentHandle }.thenReturn(345L)
        on { isFile }.thenReturn(true)
    }
    private val nodes = listOf(1L, 2L)
    private val targetNode = 100L

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
            moveNodesUseCase,
            copyNodesUseCase,
        )
    }

    private fun initTestClass() {
        underTest = ContactFileListViewModel(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            moveNodesToRubbishUseCase = moveNodesToRubbishUseCase,
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            moveNodesUseCase = moveNodesUseCase,
            copyNodesUseCase = copyNodesUseCase,
        )
    }

    @ParameterizedTest(name = "test that snack bar message is updated when internet connectivity is not available and transfer type is {0}")
    @EnumSource(NodeNameCollisionType::class, names = ["RESTORE"], mode = EnumSource.Mode.EXCLUDE)
    fun `test that snack bar message is updated when internet connectivity is not available`(type: NodeNameCollisionType) =
        runTest {
            initTestClass()
            whenever(isConnectedToInternetUseCase()).thenReturn(false)
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.moveRequestResult).isNull()
                underTest.copyOrMoveNodes(nodes = nodes, targetNode = targetNode, type = type)
                val state1 = awaitItem()
                Truth.assertThat(state1.snackBarMessage).isNotNull()
                underTest.onConsumeSnackBarMessageEvent()
                Truth.assertThat(awaitItem().snackBarMessage).isNull()
            }
        }

    @ParameterizedTest(name = "test that copyMoveAlertTextId is updated when checkNameCollision failed and transfer type is {0}")
    @EnumSource(NodeNameCollisionType::class, names = ["RESTORE"], mode = EnumSource.Mode.EXCLUDE)
    fun `test that copyMoveAlertTextId is updated when checkNameCollision failed`(type: NodeNameCollisionType) =
        runTest {
            initTestClass()
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            whenever(
                checkNodesNameCollisionUseCase(nodes = mapOf(1L to 100L, 2L to 100L), type = type)
            ).thenThrow(RuntimeException::class.java)
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.moveRequestResult).isNull()
                underTest.copyOrMoveNodes(nodes = nodes, targetNode = targetNode, type = type)
                val copyStartedState = awaitItem()
                Truth.assertThat(copyStartedState.copyMoveAlertTextId).isNotNull()
                Truth.assertThat(awaitItem().copyMoveAlertTextId).isNull()
            }
        }

    @ParameterizedTest(name = "test that moveRequestResult updated correctly when calling copyOrMoveNodes failed and transfer type is {0}")
    @EnumSource(NodeNameCollisionType::class, names = ["RESTORE"], mode = EnumSource.Mode.EXCLUDE)
    fun `test that moveRequestResult updated correctly when calling copyOrMoveNodes failed`(type: NodeNameCollisionType) =
        runTest {
            initTestClass()
            val nameCollisionResult = NodeNameCollisionResult(
                noConflictNodes = mapOf(pair = Pair(1L, 100L)),
                conflictNodes = mapOf(Pair(2L, nodeNameCollision)),
                type = type
            )
            whenever(
                moveNodesUseCase(mapOf(Pair(1L, 100L)))
            ).thenThrow(RuntimeException::class.java)
            whenever(
                copyNodesUseCase(mapOf(Pair(1L, 100L)))
            ).thenThrow(RuntimeException::class.java)
            whenever(
                checkNodesNameCollisionUseCase(
                    nodes = mapOf(1L to 100L, 2L to 100L),
                    type = type
                )
            ).thenReturn(nameCollisionResult)
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.moveRequestResult).isNull()
                underTest.copyOrMoveNodes(nodes, targetNode, type)
                Truth.assertThat(awaitItem().copyMoveAlertTextId).isNotNull()
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.moveRequestResult).isNotNull()
                Truth.assertThat(updatedState.moveRequestResult?.isFailure).isTrue()
            }
        }


    @ParameterizedTest(name = "test that moveRequestResult updated correctly when calling copyOrMoveNodes successfully and transfer type is {0}")
    @EnumSource(NodeNameCollisionType::class, names = ["RESTORE"], mode = EnumSource.Mode.EXCLUDE)
    fun `test that moveRequestResult updated correctly when calling copyOrMoveNodes successfully`(
        type: NodeNameCollisionType,
    ) =
        runTest {
            initTestClass()
            val result = mock<MoveRequestResult.GeneralMovement>()
            whenever(
                moveNodesUseCase(mapOf(Pair(1L, 100L)))
            ).thenReturn(result)
            whenever(
                copyNodesUseCase(mapOf(Pair(1L, 100L)))
            ).thenReturn(result)
            val nameCollisionResult = NodeNameCollisionResult(
                noConflictNodes = mapOf(pair = Pair(1L, 100L)),
                conflictNodes = mapOf(Pair(2L, nodeNameCollision)),
                type = type
            )
            whenever(
                checkNodesNameCollisionUseCase(
                    nodes = mapOf(1L to 100L, 2L to 100L),
                    type = type
                )
            ).thenReturn(nameCollisionResult)
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.moveRequestResult).isNull()
                underTest.copyOrMoveNodes(nodes, targetNode, type)
                Truth.assertThat(awaitItem().copyMoveAlertTextId).isNotNull()
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.moveRequestResult).isNotNull()
                Truth.assertThat(updatedState.moveRequestResult?.isSuccess).isTrue()
            }
        }

    @ParameterizedTest(name = "test that nodeNameCollisionResult updated when calling checkNodesNameCollisionUseCase successfully and transfer type is {0}")
    @EnumSource(NodeNameCollisionType::class, names = ["RESTORE"], mode = EnumSource.Mode.EXCLUDE)
    fun `test that nodeNameCollisionResult updated when calling checkNodesNameCollisionUseCase successfully`(
        type: NodeNameCollisionType,
    ) =
        runTest {
            initTestClass()
            val nameCollisionResult = NodeNameCollisionResult(
                noConflictNodes = mapOf(pair = Pair(1L, 100L)),
                conflictNodes = mapOf(Pair(2L, nodeNameCollision)),
                type = type
            )
            whenever(
                checkNodesNameCollisionUseCase(
                    nodes = mapOf(1L to 100L, 2L to 100L),
                    type = type
                )
            ).thenReturn(nameCollisionResult)
            val result = mock<MoveRequestResult.GeneralMovement>()
            whenever(
                moveNodesUseCase(mapOf(Pair(1L, 100L)))
            ).thenReturn(result)
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.nodeNameCollisionResult).isEmpty()
                underTest.copyOrMoveNodes(nodes, targetNode, type)
                Truth.assertThat(awaitItem().copyMoveAlertTextId).isNotNull()
                Truth.assertThat(awaitItem().nodeNameCollisionResult).isNotEmpty()
                underTest.markHandleNodeNameCollisionResult()
                Truth.assertThat(awaitItem().nodeNameCollisionResult).isEmpty()
            }
        }

    @ParameterizedTest(name = "test that moveRequestResult updated when calling markHandleMoveRequestResult and transfer type is {0}")
    @EnumSource(NodeNameCollisionType::class, names = ["RESTORE"], mode = EnumSource.Mode.EXCLUDE)
    fun `test that moveRequestResult updated when calling markHandleMoveRequestResult`(type: NodeNameCollisionType) =
        runTest {
            initTestClass()
            val result = mock<MoveRequestResult.GeneralMovement>()
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            val nameCollisionResult = NodeNameCollisionResult(
                noConflictNodes = mapOf(pair = Pair(1L, 100L)),
                conflictNodes = mapOf(Pair(2L, nodeNameCollision)),
                type = type
            )
            whenever(
                checkNodesNameCollisionUseCase(
                    nodes = mapOf(1L to 100L, 2L to 100L),
                    type = type
                )
            ).thenReturn(nameCollisionResult)
            whenever(
                moveNodesUseCase(mapOf(Pair(1L, 100L)))
            ).thenReturn(result)
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.moveRequestResult).isNull()
                underTest.copyOrMoveNodes(nodes, targetNode, type)
                Truth.assertThat(awaitItem().copyMoveAlertTextId).isNotNull()
                val movementComplete = awaitItem()
                Truth.assertThat(movementComplete.nodeNameCollisionResult).isNotEmpty()
                Truth.assertThat(movementComplete.moveRequestResult).isNotNull()
                Truth.assertThat(movementComplete.moveRequestResult?.isSuccess).isTrue()
                underTest.markHandleMoveRequestResult()
                Truth.assertThat(awaitItem().moveRequestResult).isNull()
            }
        }

    @Test
    fun `test that StorageState is returned when getStorageState is called`() = runTest {
        initTestClass()
        whenever(monitorStorageStateEventUseCase()).thenReturn(
            MutableStateFlow(
                StorageStateEvent(
                    handle = 1L,
                    eventString = "eventString",
                    number = 0L,
                    text = "text",
                    type = EventType.Storage,
                    storageState = StorageState.Red
                )
            )
        )
        val state = underTest.getStorageState()
        Truth.assertThat(state).isEqualTo(StorageState.Red)
    }

    @Test
    fun `test that test that moveRequestResult updated correctly when calling moveNodesToRubbish failed`() =
        runTest {
            initTestClass()
            whenever(moveNodesToRubbishUseCase(nodes)).thenThrow(RuntimeException::class.java)
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.moveRequestResult).isNull()
                underTest.moveNodesToRubbish(nodes)
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.moveRequestResult).isNotNull()
                Truth.assertThat(updatedState.moveRequestResult?.isFailure).isTrue()
            }
        }

    @Test
    fun `test that test that moveRequestResult updated correctly when calling moveNodesToRubbish completed successfully`() =
        runTest {
            initTestClass()
            val result = mock<MoveRequestResult.RubbishMovement>()
            whenever(moveNodesToRubbishUseCase(nodes)).thenReturn(result)
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.moveRequestResult).isNull()
                underTest.moveNodesToRubbish(nodes)
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.moveRequestResult).isNotNull()
                Truth.assertThat(updatedState.moveRequestResult?.isSuccess).isTrue()
            }
        }
}
