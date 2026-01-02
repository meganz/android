package mega.privacy.android.core.nodecomponents.menu.menuitem

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.RenameMenuAction
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
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RenameBottomSheetMenuItemTest {

    private val underTest = RenameBottomSheetMenuItem(menuAction = mock<RenameMenuAction>())

    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - expected {4}")
    @MethodSource("provideTestParameters")
    fun `test that rename bottom sheet menu item visibility is correct`(
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
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that rename bottom sheet menu item has the correct menu action`() {
        assertThat(underTest.menuAction).isInstanceOf(RenameMenuAction::class.java)
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
    )
}

