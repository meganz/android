package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.ShareMenuAction
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShareBottomSheetMenuItemTest {

    private val shareBottomSheetMenuItem = ShareBottomSheetMenuItem(ShareMenuAction())

    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - isConnected {4} - expected {5}")
    @MethodSource("provideTestParameters")
    fun `test that share bottom sheet menu item visibility is correct`(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
        expected: Boolean,
    ) {
        val result = shareBottomSheetMenuItem.shouldDisplay(
            isNodeInRubbish,
            accessPermission,
            isInBackups,
            node,
            isConnected,
        )
        assertEquals(expected, result)
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
            true,
            AccessPermission.FULL,
            false,
            mock<TypedFolderNode> { on { isTakenDown } doReturn false },
            false,
            false,
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFolderNode> { on { isTakenDown } doReturn false },
            false,
            true,
        )
    )
}