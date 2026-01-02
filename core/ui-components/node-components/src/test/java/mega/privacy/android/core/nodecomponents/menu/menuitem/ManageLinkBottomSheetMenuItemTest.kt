package mega.privacy.android.core.nodecomponents.menu.menuitem

import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManageLinkBottomSheetMenuItemTest {
    private val underTest = ManageLinkBottomSheetMenuItem(menuAction = mock<ManageLinkMenuAction>())
    private val exportedData = mock<ExportedData> {
        on { publicLink } doReturn "publicLink"
    }

    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - expected {4}")
    @MethodSource("provideTestParameters")
    fun `test that manage link bottom sheet menu item visibility is correct`(
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
            mock<TypedFileNode> {
                on { isTakenDown } doReturn true
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
                on { exportedData } doReturn null
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
                on { exportedData } doReturn exportedData
                on { isNodeKeyDecrypted } doReturn true
            },
            true
        ),
        Arguments.of(
            false,
            AccessPermission.READWRITE,
            false,
            mock<TypedFileNode> {
                on { isTakenDown } doReturn false
                on { exportedData } doReturn exportedData
                on { isNodeKeyDecrypted } doReturn true
            },
            false
        ),
        Arguments.of(
            true,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> {
                on { isTakenDown } doReturn false
                on { exportedData } doReturn exportedData
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
                on { exportedData } doReturn exportedData
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
                on { exportedData } doReturn exportedData
                on { isNodeKeyDecrypted } doReturn false
            },
            false
        ),
    )
}

