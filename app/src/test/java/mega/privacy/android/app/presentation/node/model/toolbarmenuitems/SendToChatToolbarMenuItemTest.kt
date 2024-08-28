package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.navigation.NavHostController
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.SendToChatMenuAction
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.SendToChatToolbarMenuItem
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendToChatToolbarMenuItemTest {

    private val underTest = SendToChatToolbarMenuItem(
        SendToChatMenuAction(), mock()
    )

    private val oneFileNodeSelected = mock<TypedFolderNode> {
        on { isTakenDown }.thenReturn(false)
    }
    private val oneFolderNodeSelected = mock<TypedFolderNode>()
    private val multipleNodes = listOf(oneFileNodeSelected, oneFolderNodeSelected)

    @ParameterizedTest(name = "when noNodeTakenDown: {0} and allFileNodes: {1} then visibility is {2}")
    @MethodSource("provideArguments")
    fun `test that send to chat item visibility is updated`(
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        expected: Boolean,
    ) = runTest {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = multipleNodes,
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = noNodeTakenDown,
            allFileNodes = allFileNodes,
            resultCount = 10
        )
        Truth.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that send to chat item click function is called`() {
        val actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit =
            mock()
        val onDismiss: () -> Unit = mock()
        val navController: NavHostController = mock()
        val onClick = underTest.getOnClick(
            selectedNodes = multipleNodes,
            onDismiss = onDismiss,
            actionHandler = actionHandler,
            navController = navController,
            parentScope = CoroutineScope(UnconfinedTestDispatcher())
        )
        onClick.invoke()
    }

    private fun provideArguments() = Stream.of(
        Arguments.of(true, true, true),
        Arguments.of(true, false, false),
        Arguments.of(false, true, false)
    )
}