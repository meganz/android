package test.mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.navigation.NavHostController
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.mapper.RestoreNodeResultMapper
import mega.privacy.android.app.presentation.node.model.menuaction.RestoreMenuAction
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RestoreToolbarMenuItem
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.SingleNodeRestoreResult
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.RestoreNodesUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RestoreToolbarMenuItemTest {

    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase = mock()
    private val restoreNodesUseCase: RestoreNodesUseCase = mock()
    private val restoreNodeResultMapper: RestoreNodeResultMapper = mock()
    private val snackBarHandler: SnackBarHandler = mock()

    private val underTest = RestoreToolbarMenuItem(
        menuAction = RestoreMenuAction(),
        checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
        restoreNodesUseCase = restoreNodesUseCase,
        restoreNodeResultMapper = restoreNodeResultMapper,
        snackBarHandler = snackBarHandler,
    )

    private val oneFileNodeSelected = mock<TypedFolderNode> {
        on { isTakenDown }.thenReturn(false)
    }
    private val oneFolderNodeSelected = mock<TypedFolderNode>()
    private val multipleNodes = listOf(oneFileNodeSelected, oneFolderNodeSelected)

    @ParameterizedTest(name = "when noNodeIsTakenDown: {0} and selected nodes are {1} then visibility is {2}")
    @MethodSource("provideArguments")
    fun `test that restore item visibility is updated`(
        noNodeIsTakenDown: Boolean,
        selectedNodes: List<TypedNode>,
        expected: Boolean,
    ) = runTest {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = selectedNodes,
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = noNodeIsTakenDown,
            allFileNodes = false,
            resultCount = 10
        )
        Truth.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that restore click without conflicts prints appropriate message`() = runTest {
        val navController = mock<NavHostController>()
        val node1 = mock<TypedFolderNode> {
            on(it.id).thenReturn(NodeId(1234L))
            on(it.restoreId).thenReturn(NodeId(2345L))
        }
        val node2 = mock<TypedFolderNode> {
            on(it.id).thenReturn(NodeId(234L))
            on(it.restoreId).thenReturn(NodeId(2345L))
        }
        val singleRestoreResult = SingleNodeRestoreResult(
            successCount = 1,
            destinationFolderName = null
        )
        val nodeMap = mapOf(Pair(1234L, 2345L), Pair(234L, 2345L))
        whenever(checkNodesNameCollisionUseCase(nodeMap, NodeNameCollisionType.RESTORE)).thenReturn(
            NodeNameCollisionsResult(
                conflictNodes = emptyMap(),
                noConflictNodes = nodeMap,
                type = NodeNameCollisionType.RESTORE
            )
        )
        whenever(restoreNodesUseCase(nodeMap)).thenReturn(singleRestoreResult)
        whenever(restoreNodeResultMapper(singleRestoreResult)).thenReturn("Success")

        val onClickFunction = underTest.getOnClick(
            selectedNodes = listOf(node1, node2),
            onDismiss = {},
            actionHandler = { _, _ -> },
            navController = navController,
            parentScope = CoroutineScope(UnconfinedTestDispatcher())
        )
        onClickFunction()
        verify(checkNodesNameCollisionUseCase).invoke(nodeMap, NodeNameCollisionType.RESTORE)
        verify(restoreNodesUseCase).invoke(nodeMap)
        verify(restoreNodeResultMapper).invoke(singleRestoreResult)
        verify(snackBarHandler).postSnackbarMessage("Success")
    }

    private fun provideArguments() = Stream.of(
        Arguments.of(false, emptyList<TypedFolderNode>(), false),
        Arguments.of(false, multipleNodes, false),
        Arguments.of(true, emptyList<TypedFolderNode>(), false),
        Arguments.of(true, multipleNodes, true),
    )

    @AfterEach
    fun resetMocks() {
        reset(
            checkNodesNameCollisionUseCase,
            restoreNodesUseCase,
            restoreNodeResultMapper,
            snackBarHandler
        )
    }
}