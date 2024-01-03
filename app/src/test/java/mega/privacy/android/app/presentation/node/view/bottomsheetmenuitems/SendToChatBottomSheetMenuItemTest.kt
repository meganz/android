package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.SendToChatMenuAction
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
class SendToChatBottomSheetMenuItemTest {

    private val sendToChatBottomSheetMenuItem =
        SendToChatBottomSheetMenuItem(SendToChatMenuAction())

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