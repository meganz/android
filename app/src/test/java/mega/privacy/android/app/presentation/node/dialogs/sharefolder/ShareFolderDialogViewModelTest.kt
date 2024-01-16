package mega.privacy.android.app.presentation.node.dialogs.sharefolder

import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeByHandleUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShareFolderDialogViewModelTest {
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val checkBackupNodeTypeByHandleUseCase: CheckBackupNodeTypeByHandleUseCase = mock()

    private val underTest = ShareFolderDialogViewModel(
        getNodeByHandleUseCase = getNodeByHandleUseCase,
        checkBackupNodeTypeByHandleUseCase = checkBackupNodeTypeByHandleUseCase
    )

    @BeforeAll
    fun setDispatchers() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `test that backup type is not root node then state has shouldHandlePositiveClick value to truetest that the share folder dialog returns the correct information when the BackupNodeType is a RootNode`() =
        runTest {
            val handle = 1234L
            val node: TypedFolderNode = mock()
            whenever(getNodeByHandleUseCase(handle)).thenReturn(node)
            whenever(checkBackupNodeTypeByHandleUseCase(node)).thenReturn(BackupNodeType.RootNode)
            underTest.getDialogContents(listOf(NodeId(handle), NodeId(2345L)))
            val state = underTest.state.value
            Truth.assertThat(state.title).isEqualTo(R.string.backup_share_permission_title)
            Truth.assertThat(state.info).isEqualTo(R.string.backup_multi_share_permission_text)
            Truth.assertThat(state.positiveButton).isEqualTo(R.string.general_positive_button)
            Truth.assertThat(state.negativeButton).isEqualTo(R.string.general_cancel)
            Truth.assertThat(state.shouldHandlePositiveClick).isEqualTo(true)
        }

    @Test
    fun `test that the share folder dialog returns the correct information when the BackupNodeType is not a RootNode`() =
        runTest {
            val handle = 1234L
            val node: TypedFolderNode = mock()
            whenever(getNodeByHandleUseCase(handle)).thenReturn(node)
            whenever(checkBackupNodeTypeByHandleUseCase(node)).thenReturn(BackupNodeType.NonBackupNode)
            underTest.getDialogContents(listOf(NodeId(handle), NodeId(2345L)))
            val state = underTest.state.value
            Truth.assertThat(state.title).isEqualTo(R.string.backup_share_permission_title)
            Truth.assertThat(state.info).isEqualTo(R.string.backup_share_with_root_permission_text)
            Truth.assertThat(state.positiveButton).isEqualTo(R.string.button_permission_info)
            Truth.assertThat(state.negativeButton).isEqualTo(null)
            Truth.assertThat(state.shouldHandlePositiveClick).isEqualTo(false)
        }

    @AfterEach
    fun resetMocks() {
        reset(
            getNodeByHandleUseCase,
            checkBackupNodeTypeByHandleUseCase
        )
    }

    @AfterAll
    fun resetDispatchers() {
        Dispatchers.resetMain()
    }
}