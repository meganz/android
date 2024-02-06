package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.ShareFolderMenuAction
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeByHandleUseCase
import mega.privacy.android.domain.usecase.shares.CreateShareKeyUseCase
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShareFolderBottomSheetMenuItemTest {

    private val applicationScope: CoroutineScope = CoroutineScope(
        UnconfinedTestDispatcher()
    )
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

    private val createShareKeyUseCase: CreateShareKeyUseCase = mock()
    private val checkBackupNodeTypeByHandleUseCase: CheckBackupNodeTypeByHandleUseCase = mock()

    private val shareFolderBottomSheetMenuItem = ShareFolderBottomSheetMenuItem(
        menuAction = ShareFolderMenuAction(),
        scope = applicationScope,
        mainDispatcher = mainDispatcher,
        createShareKeyUseCase = createShareKeyUseCase,
        checkBackupNodeTypeByHandleUseCase = checkBackupNodeTypeByHandleUseCase
    )

    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - isConnected {4} - expected {5}")
    @MethodSource("provideTestParameters")
    fun `test that share folder bottom sheet menu item visibility is correct`(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
        expected: Boolean,
    ) = runTest {
        val result = shareFolderBottomSheetMenuItem.shouldDisplay(
            isNodeInRubbish,
            accessPermission,
            isInBackups,
            node,
            isConnected,
        )
        Truth.assertThat(result).isEqualTo(expected)
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
            AccessPermission.OWNER,
            false,
            mock<TypedFolderNode> { on { isTakenDown } doReturn false },
            false,
            false,
        ),
        Arguments.of(
            true,
            AccessPermission.OWNER,
            false,
            mock<TypedFolderNode> {
                on { isTakenDown } doReturn false
                on { isPendingShare } doReturn true
            },
            false,
            false,
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFolderNode> {
                on { isTakenDown } doReturn false
                on { isPendingShare } doReturn true
            },
            false,
            false,
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFolderNode> {
                on { isTakenDown } doReturn false
                on { isPendingShare } doReturn false
            },
            false,
            true,
        ),
        Arguments.of(
            false,
            AccessPermission.READWRITE,
            false,
            mock<TypedFolderNode> {
                on { isTakenDown } doReturn false
                on { isPendingShare } doReturn false
            },
            false,
            false,
        ),
    )
}