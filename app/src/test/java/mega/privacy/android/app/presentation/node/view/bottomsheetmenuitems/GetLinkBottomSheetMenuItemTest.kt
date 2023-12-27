package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.GetLinkMenuAction
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
class GetLinkBottomSheetMenuItemTest {
    private val underTest = GetLinkBottomSheetMenuItem(GetLinkMenuAction())

    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - expected {4}")
    @MethodSource("provideTestParameters")
    fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        expected: Boolean,
    ) {
        val result = underTest.shouldDisplay(
            isNodeInRubbish,
            accessPermission,
            isInBackups,
            node
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
            false
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
            true
        ),
    )
}