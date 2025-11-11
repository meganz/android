package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.RenameMenuAction
import mega.privacy.android.app.presentation.search.navigation.searchRenameDialog
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RenameBottomSheetMenuItemTest {

    private val renameBottomSheetMenuItem = RenameBottomSheetMenuItem()

    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - expected {4}")
    @MethodSource("provideTestParameters")
    fun `test that rename bottom sheet menu item visibility is correct`(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        expected: Boolean,
    ) = runTest {
        val result = renameBottomSheetMenuItem.shouldDisplay(
            isNodeInRubbish,
            accessPermission,
            isInBackups,
            node,
            true
        )
        Truth.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that rename bottom sheet menu item has the correct menu action`() {
        Truth.assertThat(renameBottomSheetMenuItem.menuAction).isInstanceOf(RenameMenuAction::class.java)
    }

    @Test
    fun `test that rename bottom sheet menu item onClick function opens dialog`() = runTest {
        val node: TypedFileNode = mock {
            whenever(it.id).thenReturn(NodeId(1234L))
        }
        val onDismiss = mock<() -> Unit>()
        val actionHandler = mock<(menuAction: MenuAction, node: TypedNode) -> Unit>()
        val navController = mock<NavHostController>()
        val parentScope = mock<CoroutineScope>()
        val onClickFunction = renameBottomSheetMenuItem.getOnClickFunction(
            node,
            onDismiss,
            actionHandler,
            navController,
            parentScope
        )
        onClickFunction()
        verify(onDismiss).invoke()
        verify(navController).navigate(searchRenameDialog.plus("/${node.id.longValue}"))
    }

    private fun provideTestParameters() = Stream.of(
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> {
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            },
            true
        ),
        Arguments.of(
            false,
            AccessPermission.FULL,
            false,
            mock<TypedFileNode> {
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            },
            true
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFolderNode> {
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            },
            true
        ),
        Arguments.of(
            true,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> {
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            },
            false
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            true,
            mock<TypedFileNode> {
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            },
            false
        ),
        Arguments.of(
            false,
            AccessPermission.READWRITE,
            false,
            mock<TypedFileNode> {
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            },
            false
        ),
        Arguments.of(
            false,
            AccessPermission.READ,
            false,
            mock<TypedFileNode> {
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            },
            false
        ),
        Arguments.of(
            false,
            null,
            false,
            mock<TypedFileNode> {
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            },
            false
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFolderNode> {
                on { isTakenDown } doReturn false
                on { isS4Container } doReturn true
                on { isNodeKeyDecrypted } doReturn true
            },
            false
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> {
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn false
            },
            false
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> {
                on { isTakenDown } doReturn true
                on { isNodeKeyDecrypted } doReturn true
            },
            true
        ),
    )
}

