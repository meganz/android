package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.mapper.RestoreNodeResultMapper
import mega.privacy.android.app.presentation.node.model.menuaction.RestoreMenuAction
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.SingleNodeRestoreResult
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.RestoreNodesUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
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
class RestoreBottomSheetMenuItemTest {

    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase = mock()
    private val restoreNodesUseCase: RestoreNodesUseCase = mock()
    private val restoreNodeResultMapper: RestoreNodeResultMapper = mock()
    private val snackBarHandler: SnackBarHandler = mock()

    private val restoreBottomSheetMenuItem =
        RestoreBottomSheetMenuItem(
            menuAction = RestoreMenuAction(),
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            restoreNodesUseCase = restoreNodesUseCase,
            restoreNodeResultMapper = restoreNodeResultMapper,
            snackBarHandler = snackBarHandler,
            scope = CoroutineScope(UnconfinedTestDispatcher())
        )

    @ParameterizedTest(name = "isNodeInRubbish {0} - expected {1}")
    @MethodSource("provideTestParameters")
    fun `test that restore bottom sheet menu item visibility is correct`(
        isNodeInRubbish: Boolean,
        expected: Boolean,
    ) = runTest {
        val result = restoreBottomSheetMenuItem.shouldDisplay(
            isNodeInRubbish,
            null,
            false,
            mock<TypedFolderNode>(),
            true
        )
        assertEquals(expected, result)
    }

    private fun provideTestParameters() = Stream.of(
        Arguments.of(true, true),
        Arguments.of(false, false),
    )

    @Test
    fun `test that restore click without conflicts prints appropriate message`() = runTest {
        val navController = mock<NavHostController>()
        val node = mock<TypedFolderNode> {
            on(it.id).thenReturn(NodeId(1234L))
            on(it.restoreId).thenReturn(NodeId(2345L))
        }
        val singleRestoreResult = SingleNodeRestoreResult(
            successCount = 1,
            destinationFolderName = null
        )
        val nodeMap = mapOf(Pair(1234L, 2345L))
        whenever(checkNodesNameCollisionUseCase(nodeMap, NodeNameCollisionType.RESTORE)).thenReturn(
            NodeNameCollisionResult(
                conflictNodes = emptyMap(),
                noConflictNodes = nodeMap,
                type = NodeNameCollisionType.RESTORE
            )
        )
        whenever(restoreNodesUseCase(nodeMap)).thenReturn(singleRestoreResult)
        whenever(restoreNodeResultMapper(singleRestoreResult)).thenReturn("Success")

        val onClickFunction = restoreBottomSheetMenuItem.getOnClickFunction(
            node = node,
            onDismiss = {},
            actionHandler = { _, _ -> },
            navController = navController
        )
        onClickFunction()
        verify(checkNodesNameCollisionUseCase).invoke(nodeMap, NodeNameCollisionType.RESTORE)
        verify(restoreNodesUseCase).invoke(nodeMap)
        verify(restoreNodeResultMapper).invoke(singleRestoreResult)
        verify(snackBarHandler).postSnackbarMessage("Success")
    }

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