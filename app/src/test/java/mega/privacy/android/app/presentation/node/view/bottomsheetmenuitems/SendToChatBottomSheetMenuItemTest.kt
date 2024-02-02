package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.SendToChatMenuAction
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.chat.GetNodeToAttachUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendToChatBottomSheetMenuItemTest {

    private val getNodeToAttachUseCase: GetNodeToAttachUseCase = mock()

    private val scope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())
    private val menuAction = SendToChatMenuAction()

    private val sendToChatBottomSheetMenuItem =
        SendToChatBottomSheetMenuItem(
            scope = scope,
            menuAction = menuAction,
            getNodeToAttachUseCase = getNodeToAttachUseCase
        )

    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - isConnected {4} - expected {5}")
    @MethodSource("provideTestParameters")
    fun `test that send to chat bottom sheet menu item visibility is correct`(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
        expected: Boolean,
    ) = runTest {
        val result = sendToChatBottomSheetMenuItem.shouldDisplay(
            isNodeInRubbish,
            accessPermission,
            isInBackups,
            node,
            isConnected,
        )
        assertEquals(expected, result)
    }

    @Test
    fun `test that on click on send to chat calls GetNodeToAttach returns some node`() = runTest {
        val node: TypedFileNode = mock {
            whenever(it.id).thenReturn(NodeId(1234L))
            whenever(it.name).thenReturn("Sample Name.txt")
        }
        val onDismiss = mock<() -> Unit>()
        val actionHandler = mock<(menuAction: MenuAction, node: TypedNode) -> Unit>()
        val navController = mock<NavHostController>()
        whenever(getNodeToAttachUseCase(node)).thenReturn(node)
        val onClick = sendToChatBottomSheetMenuItem.getOnClickFunction(
            node = node,
            onDismiss = onDismiss,
            actionHandler = actionHandler,
            navController = navController
        )
        onClick()
        verify(getNodeToAttachUseCase).invoke(node)
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getNodeToAttachUseCase
        )
    }

    private fun provideTestParameters() = Stream.of(
        Arguments.of(
            true,
            AccessPermission.OWNER,
            false,
            mock<TypedFolderNode> { on { isTakenDown } doReturn true },
            false,
            false,
        ),
        Arguments.of(
            false,
            AccessPermission.READWRITE,
            false,
            mock<TypedFolderNode> { on { isTakenDown } doReturn false },
            false,
            false,
        ),
        Arguments.of(
            false,
            AccessPermission.FULL,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            true,
            true
        ),
    )
}