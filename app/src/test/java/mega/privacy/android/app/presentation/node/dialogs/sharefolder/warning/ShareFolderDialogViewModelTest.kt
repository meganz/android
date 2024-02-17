package mega.privacy.android.app.presentation.node.dialogs.sharefolder.warning

import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeByHandleUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShareFolderDialogViewModelTest {
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val checkBackupNodeTypeByHandleUseCase: CheckBackupNodeTypeByHandleUseCase = mock()
    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())

    private val underTest = ShareFolderDialogViewModel(
        applicationScope = applicationScope,
        getNodeByIdUseCase = getNodeByIdUseCase,
        checkBackupNodeTypeByHandleUseCase = checkBackupNodeTypeByHandleUseCase
    )

    @Test
    fun `test that the share folder dialog returns the correct information when the BackupNodeType is a RootNode`() =
        runTest {
            val handle = 1234L
            val node: TypedFolderNode = mock()
            whenever(getNodeByIdUseCase(NodeId(handle))).thenReturn(node)
            whenever(checkBackupNodeTypeByHandleUseCase(node)).thenReturn(BackupNodeType.FolderNode)
            underTest.getDialogContents(listOf(NodeId(handle)))
            val state = underTest.state.value
            Truth.assertThat(state.info).isEqualTo(R.string.backup_share_permission_text)
            Truth.assertThat(state.positiveButton).isEqualTo(R.string.button_permission_info)
            Truth.assertThat(state.negativeButton).isEqualTo(null)
        }

    @Test
    fun `test that the share folder dialog returns the correct information when the BackupNodeType is not a RootNode`() =
        runTest {
            val handle = 1234L
            val node: TypedFolderNode = mock()
            whenever(getNodeByIdUseCase(NodeId(handle))).thenReturn(node)
            whenever(checkBackupNodeTypeByHandleUseCase(node)).thenReturn(BackupNodeType.RootNode)
            underTest.getDialogContents(listOf(NodeId(handle), NodeId(2345L)))
            val state = underTest.state.value
            Truth.assertThat(state.info).isEqualTo(R.string.backup_multi_share_permission_text)
            Truth.assertThat(state.positiveButton).isEqualTo(R.string.general_positive_button)
            Truth.assertThat(state.negativeButton).isEqualTo(R.string.general_cancel)
        }

    @AfterEach
    fun resetMocks() {
        reset(
            getNodeByIdUseCase,
            checkBackupNodeTypeByHandleUseCase,
        )
    }
}