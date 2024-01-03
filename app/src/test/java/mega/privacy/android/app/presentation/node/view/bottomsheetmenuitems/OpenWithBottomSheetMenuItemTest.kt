package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.OpenWithMenuAction
import mega.privacy.android.domain.entity.node.TypedFileNode
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
class OpenWithBottomSheetMenuItemTest {
    private val underTest = OpenWithBottomSheetMenuItem(OpenWithMenuAction())

    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - expected {4}")
    @MethodSource("provideTestParameters")
    fun `test that open with bottom sheet menu item visibility is correct`(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        expected: Boolean,
    ) = runTest {
        val result = underTest.shouldDisplay(
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
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn true },
            false
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            true
        ),
        Arguments.of(
            false,
            AccessPermission.READWRITE,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            true
        ),
        Arguments.of(
            true,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            false
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            true,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            true
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            true,
            mock<TypedFolderNode> { on { isTakenDown } doReturn false },
            false
        ),
    )
}