package mega.privacy.android.core.nodecomponents.dialog.sharefolder

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeUseCase
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
    private val checkBackupNodeTypeUseCase: CheckBackupNodeTypeUseCase = mock()

    private val underTest = ShareFolderDialogViewModel(
        getNodeByIdUseCase = getNodeByIdUseCase,
        checkBackupNodeTypeUseCase = checkBackupNodeTypeUseCase
    )

    @Test
    fun `test that the share folder dialog returns the correct information when the BackupNodeType is a RootNode`() =
        runTest {
            val handle = 1234L
            val node: TypedFolderNode = mock()
            whenever(getNodeByIdUseCase(NodeId(handle))).thenReturn(node)
            whenever(checkBackupNodeTypeUseCase(node)).thenReturn(BackupNodeType.FolderNode)
            underTest.getDialogContents(listOf(NodeId(handle)))
            val state = underTest.state.value

            assertThat(state.dialogType).isInstanceOf(ShareFolderDialogType.Single::class.java)
        }

    @Test
    fun `test that the share folder dialog returns the correct information when the BackupNodeType is not a RootNode`() =
        runTest {
            val handle = 1234L
            val node: TypedFolderNode = mock()
            whenever(getNodeByIdUseCase(NodeId(handle))).thenReturn(node)
            whenever(checkBackupNodeTypeUseCase(node)).thenReturn(BackupNodeType.RootNode)
            underTest.getDialogContents(listOf(NodeId(handle), NodeId(2345L)))
            val state = underTest.state.value

            assertThat(state.dialogType).isInstanceOf(ShareFolderDialogType.Multiple::class.java)
        }

    @AfterEach
    fun resetMocks() {
        reset(
            getNodeByIdUseCase,
            checkBackupNodeTypeUseCase,
        )
    }
}
