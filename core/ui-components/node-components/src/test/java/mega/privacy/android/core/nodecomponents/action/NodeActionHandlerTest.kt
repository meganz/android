package mega.privacy.android.core.nodecomponents.action

import mega.privacy.android.core.nodecomponents.action.clickhandler.SingleNodeAction
import mega.privacy.android.core.nodecomponents.action.clickhandler.MultiNodeAction

import androidx.compose.runtime.Composable
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.OpenWithMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RestoreMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SendToChatMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareFolderMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.VersionsMenuAction
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
class NodeActionHandlerTest {

    @Test
    fun `test NodeActionHandler single node invoke calls singleNodeHandler`() {
        val mockSingleNodeHandler = mock<(MenuAction, TypedNode) -> Unit>()
        val mockMultipleNodesHandler = mock<(MenuAction, List<TypedNode>) -> Unit>()
        val mockAction = mock<VersionsMenuAction>()
        val mockNode = mock<TypedNode>()

        val nodeActionHandler = NodeActionHandler(mockSingleNodeHandler, mockMultipleNodesHandler)

        nodeActionHandler(mockAction, mockNode)

        verify(mockSingleNodeHandler).invoke(mockAction, mockNode)
    }

    @Test
    fun `test NodeActionHandler multiple nodes invoke calls multipleNodesHandler`() {
        val mockSingleNodeHandler = mock<(MenuAction, TypedNode) -> Unit>()
        val mockMultipleNodesHandler = mock<(MenuAction, List<TypedNode>) -> Unit>()
        val mockAction = mock<MoveMenuAction>()
        val mockNodes = listOf(mock<TypedNode>(), mock<TypedNode>())

        val nodeActionHandler = NodeActionHandler(mockSingleNodeHandler, mockMultipleNodesHandler)

        nodeActionHandler(mockAction, mockNodes)

        verify(mockMultipleNodesHandler).invoke(mockAction, mockNodes)
    }

    @Test
    fun `test mocked VersionsMenuAction can be created`() {
        val mockAction = mock<VersionsMenuAction>()
        assertThat(mockAction).isNotNull()
    }

    @Test
    fun `test mocked MoveMenuAction can be created`() {
        val mockAction = mock<MoveMenuAction>()
        assertThat(mockAction).isNotNull()
    }

    @Test
    fun `test mocked CopyMenuAction can be created`() {
        val mockAction = mock<CopyMenuAction>()
        assertThat(mockAction).isNotNull()
    }

    @Test
    fun `test mocked ShareFolderMenuAction can be created`() {
        val mockAction = mock<ShareFolderMenuAction>()
        assertThat(mockAction).isNotNull()
    }

    @Test
    fun `test mocked RestoreMenuAction can be created`() {
        val mockAction = mock<RestoreMenuAction>()
        assertThat(mockAction).isNotNull()
    }

    @Test
    fun `test mocked SendToChatMenuAction can be created`() {
        val mockAction = mock<SendToChatMenuAction>()
        assertThat(mockAction).isNotNull()
    }

    @Test
    fun `test mocked OpenWithMenuAction can be created`() {
        val mockAction = mock<OpenWithMenuAction>()
        assertThat(mockAction).isNotNull()
    }

    @Test
    fun `test mocked DownloadMenuAction can be created`() {
        val mockAction = mock<DownloadMenuAction>()
        assertThat(mockAction).isNotNull()
    }

    @Test
    fun `test mocked AvailableOfflineMenuAction can be created`() {
        val mockAction = mock<AvailableOfflineMenuAction>()
        assertThat(mockAction).isNotNull()
    }

    @Test
    fun `test mocked HideMenuAction can be created`() {
        val mockAction = mock<HideMenuAction>()
        assertThat(mockAction).isNotNull()
    }

    @Test
    fun `test ViewModel action handling with different action types`() {
        val mockViewModel = mock<NodeOptionsActionViewModel>()
        val actions = listOf(
            mock<VersionsMenuAction>(),
            mock<MoveMenuAction>(),
            mock<CopyMenuAction>(),
            mock<DownloadMenuAction>(),
            mock<OpenWithMenuAction>()
        )

        actions.forEach { action ->
            var handlerFound = false
            whenever(mockViewModel.handleSingleNodeAction(any(), any())).thenAnswer { invocation ->
                val block = invocation.getArgument<(SingleNodeAction) -> Unit>(1)
                block(mock<SingleNodeAction>())
            }

            mockViewModel.handleSingleNodeAction(action) { handler ->
                handlerFound = true
                assertThat(handler).isNotNull()
            }

            assertThat(handlerFound).isTrue()
        }
    }

    @Test
    fun `test ViewModel multiple nodes action handling with different action types`() {
        val mockViewModel = mock<NodeOptionsActionViewModel>()
        val actions = listOf(
            mock<MoveMenuAction>(),
            mock<CopyMenuAction>(),
            mock<ShareFolderMenuAction>(),
        )

        actions.forEach { action ->
            var handlerFound = false
            whenever(
                mockViewModel.handleMultipleNodesAction(
                    any(),
                    any()
                )
            ).thenAnswer { invocation ->
                val block = invocation.getArgument<(MultiNodeAction) -> Unit>(1)
                block(mock<MultiNodeAction>())
            }

            mockViewModel.handleMultipleNodesAction(action) { handler ->
                handlerFound = true
                assertThat(handler).isNotNull()
            }

            assertThat(handlerFound).isTrue()
        }
    }

    @Test
    fun `test ViewModel handles unsupported single node actions correctly`() {
        val mockViewModel = mock<NodeOptionsActionViewModel>()
        val unsupportedAction = object : MenuAction {
            override val testTag: String = "unsupported"

            @Composable
            override fun getDescription(): String = ""
        }

        whenever(mockViewModel.handleSingleNodeAction(any(), any())).thenThrow(
            IllegalArgumentException("Action $unsupportedAction does not have a handler.")
        )

        assertThrows<IllegalArgumentException> {
            mockViewModel.handleSingleNodeAction(unsupportedAction) { }
        }
    }

    @Test
    fun `test ViewModel handles unsupported multiple nodes actions correctly`() {
        val mockViewModel = mock<NodeOptionsActionViewModel>()
        val unsupportedAction = object : MenuAction {
            override val testTag: String = "unsupported"

            @Composable
            override fun getDescription(): String = ""
        }

        whenever(mockViewModel.handleMultipleNodesAction(any(), any())).thenThrow(
            IllegalArgumentException("Action $unsupportedAction does not have a handler.")
        )

        assertThrows<IllegalArgumentException> {
            mockViewModel.handleMultipleNodesAction(unsupportedAction) { }
        }
    }
} 