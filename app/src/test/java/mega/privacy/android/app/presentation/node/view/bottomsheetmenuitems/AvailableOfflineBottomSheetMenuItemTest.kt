package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.foldernode.IsFolderEmptyUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AvailableOfflineBottomSheetMenuItemTest {

    private val removeOfflineNodeUseCase = mock<RemoveOfflineNodeUseCase>()
    private val isFolderEmptyUseCase = mock<IsFolderEmptyUseCase>()
    private val scope = mock<CoroutineScope>()
    private val underTest = AvailableOfflineBottomSheetMenuItem(
        menuAction = AvailableOfflineMenuAction(),
        removeOfflineNodeUseCase = removeOfflineNodeUseCase,
        isFolderEmptyUseCase = isFolderEmptyUseCase,
        scope = scope
    )

    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - isConnected {4} - expected {5}")
    @MethodSource("provideTestParametersWithEmptyFolder")
    fun `test that available offline bottom sheet menu item visibility is correct when folder is not empty`(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
        expected: Boolean,
    ) = runTest {
        whenever(isFolderEmptyUseCase(node)).thenReturn(false)
        val result = underTest.shouldDisplay(
            isNodeInRubbish,
            accessPermission,
            isInBackups,
            node,
            isConnected,
        )
        assertEquals(expected, result)
    }

    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - isConnected {4} - expected {5}")
    @MethodSource("provideTestParametersWithNonEmptyFolder")
    fun `test that available offline bottom sheet menu item visibility is correct when folder is empty`(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
        expected: Boolean,
    ) = runTest {
        whenever(isFolderEmptyUseCase(node)).thenReturn(true)
        val result = underTest.shouldDisplay(
            isNodeInRubbish,
            accessPermission,
            isInBackups,
            node,
            isConnected,
        )
        assertEquals(expected, result)
    }

    private fun provideTestParametersWithEmptyFolder() = Stream.of(
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn true },
            false,
            false,
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            false,
            true,
        ),
    )

    private fun provideTestParametersWithNonEmptyFolder() = Stream.of(
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn true },
            false,
            false,
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            false,
            false,
        ),
    )

}