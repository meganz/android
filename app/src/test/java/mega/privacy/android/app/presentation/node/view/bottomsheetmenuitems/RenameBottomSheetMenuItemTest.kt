package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

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
    ) {
        val result = renameBottomSheetMenuItem.shouldDisplay(
            isNodeInRubbish,
            accessPermission,
            isInBackups,
            node,
            true
        )
        assertEquals(expected, result)
    }

    private fun provideTestParameters() = Stream.of(
        Arguments.of(
            true,
            AccessPermission.OWNER,
            false,
            mock<TypedFolderNode> { on { isTakenDown } doReturn true },
            false
        ),
        Arguments.of(
            false,
            AccessPermission.READWRITE,
            false,
            mock<TypedFolderNode> { on { isTakenDown } doReturn true },
            false
        ),
        Arguments.of(
            false,
            AccessPermission.FULL,
            false,
            mock<TypedFolderNode> { on { isTakenDown } doReturn true },
            true
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFolderNode> { on { isTakenDown } doReturn true },
            true
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            true,
            mock<TypedFolderNode> { on { isTakenDown } doReturn true },
            false
        ),
    )
}