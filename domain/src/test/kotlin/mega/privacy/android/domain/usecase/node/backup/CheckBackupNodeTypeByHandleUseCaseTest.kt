package mega.privacy.android.domain.usecase.node.backup

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.repository.BackupRepository
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckBackupNodeTypeByHandleUseCaseTest {

    private val nodeRepository: NodeRepository = mock()
    private val backupRepository: BackupRepository = mock()
    private val underTest = CheckBackupNodeTypeByHandleUseCase(nodeRepository, backupRepository)

    @ParameterizedTest(name = "isNodeInBackups : {0}, isNodeInRubbishBin: {1}")
    @MethodSource("provideNotInBackupsParameters")
    fun `test that the node is a non backup node`(
        isNodeInBackups: Boolean,
        isNodeInRubbishBin: Boolean,
        node: Node,
    ) = runTest {
        whenever(nodeRepository.isNodeInBackups(node.id.longValue)).thenReturn(isNodeInBackups)
        whenever(nodeRepository.isNodeInRubbish(node.id.longValue)).thenReturn(isNodeInRubbishBin)

        val value = underTest(node)
        Truth.assertThat(value).isEqualTo(BackupNodeType.NonBackupNode)
    }

    private fun provideNotInBackupsParameters() = Stream.of(
        Arguments.of(false, false, mock<FolderNode>()),
        Arguments.of(false, true, mock<FolderNode>())
    )

    @Test
    fun `test that the node is a backups root node`() = runTest {
        val node = mock<FolderNode> {
            whenever(it.id).thenReturn(NodeId(1234L))
        }
        whenever(nodeRepository.isNodeInBackups(node.id.longValue)).thenReturn(true)
        whenever(nodeRepository.isNodeInRubbish(node.id.longValue)).thenReturn(false)

        whenever(nodeRepository.getBackupsNode()).thenReturn(node)
        val value = underTest(node)
        Truth.assertThat(value).isEqualTo(BackupNodeType.RootNode)
    }

    @Test
    fun `test that the node is a backups device node`() = runTest {
        val node = mock<FolderNode> {
            whenever(it.parentId).thenReturn(NodeId(1234L))
        }
        val passedNode = mock<FolderNode> {
            whenever(it.id).thenReturn(NodeId(1234L))
        }
        whenever(nodeRepository.isNodeInBackups(node.id.longValue)).thenReturn(true)
        whenever(nodeRepository.isNodeInRubbish(node.id.longValue)).thenReturn(false)

        whenever(nodeRepository.getBackupsNode()).thenReturn(passedNode)
        whenever(backupRepository.getDeviceId()).thenReturn("1234")

        val value = underTest(node)
        Truth.assertThat(value).isEqualTo(BackupNodeType.DeviceNode)
    }

    @Test
    fun `test that the node is a backups folder node`() = runTest {
        val node = mock<FolderNode> {
            whenever(it.id).thenReturn(NodeId(1234L))
            whenever(it.parentId).thenReturn(NodeId((5678L)))
        }

        val parentNode = mock<FolderNode> {
            whenever(it.id).thenReturn(NodeId(5678L))
        }
        whenever(nodeRepository.isNodeInBackups(node.id.longValue)).thenReturn(true)
        whenever(nodeRepository.isNodeInRubbish(node.id.longValue)).thenReturn(false)

        whenever(nodeRepository.getNodeById(node.parentId)).thenReturn(parentNode)
        whenever(backupRepository.getDeviceId()).thenReturn("1234")

        val value = underTest(node)
        Truth.assertThat(value).isEqualTo(BackupNodeType.FolderNode)
    }

    @Test
    fun `test that the node is a backups child folder node`() = runTest {
        val node = mock<FolderNode>()

        whenever(nodeRepository.isNodeInBackups(node.id.longValue)).thenReturn(true)
        whenever(nodeRepository.isNodeInRubbish(node.id.longValue)).thenReturn(false)

        val value = underTest(node)
        Truth.assertThat(value).isEqualTo(BackupNodeType.ChildFolderNode)
    }

    @AfterEach
    fun clearMocks() {
        reset(
            nodeRepository,
            backupRepository
        )
    }
}