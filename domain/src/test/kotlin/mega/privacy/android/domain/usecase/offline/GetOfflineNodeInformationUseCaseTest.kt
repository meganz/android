package mega.privacy.android.domain.usecase.offline

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.BackupsOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.usecase.node.GetNestedParentFoldersUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInCloudDriveUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetOfflineNodeInformationUseCaseTest {

    private val isNodeInCloudDriveUseCase: IsNodeInCloudDriveUseCase = mock()
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase = mock()
    private val getNestedParentFoldersUseCase: GetNestedParentFoldersUseCase = mock()
    private val node = mock<FileNode>()
    private val parent = mock<FolderNode>()
    private val grandParent = mock<FolderNode>()

    private lateinit var underTest: GetOfflineNodeInformationUseCase

    @BeforeAll
    fun setup() {
        underTest = GetOfflineNodeInformationUseCase(
            getNestedParentFoldersUseCase, isNodeInCloudDriveUseCase, isNodeInBackupsUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getNestedParentFoldersUseCase, isNodeInCloudDriveUseCase, isNodeInBackupsUseCase,
            node, parent, grandParent
        )
    }


    @Test
    fun `test that invoke returns the correct name`() = runTest {
        stubNodes()
        stubFolderTree()
        val actual = underTest.invoke(node)
        assertThat(actual.name).isEqualTo(NODE_NAME)
    }

    @Test
    fun `test that invoke returns the correct path`() = runTest {
        stubNodes()
        stubFolderTree()
        val actual = underTest.invoke(node)
        assertThat(actual.path).isEqualTo(
            File.separator + GRAND_PARENT_NAME + File.separator + PARENT_NAME + File.separator
        )
    }

    @Test
    fun `test that invoke returns the correct path when node is in backup`() = runTest {
        stubNodes()
        stubFolderTree()
        stubInBackup()
        val actual = underTest.invoke(node)
        assertThat(actual.path).isEqualTo(
            File.separator + PARENT_NAME + File.separator
        )
    }

    @Test
    fun `test that invoke returns the correct path when node is in cloud drive`() = runTest {
        stubNodes()
        stubFolderTree()
        stubInCloudDrive()
        val actual = underTest.invoke(node)
        assertThat(actual.path).isEqualTo(
            File.separator + PARENT_NAME + File.separator
        )
    }

    @Test
    fun `test that invoke returns a BackupsOfflineNodeInformation when node is a backup file`() =
        runTest {
            stubNodes()
            stubFolderTree()
            stubInBackup()
            val actual = underTest.invoke(node)
            assertThat(actual).isInstanceOf(BackupsOfflineNodeInformation::class.java)
        }

    @Test
    fun `test that invoke returns an IncomingShareOfflineNodeInformation with correct incomingHandle when node is a shared node`() =
        runTest {
            stubNodes()
            stubFolderTree()
            whenever(grandParent.isIncomingShare).thenReturn(true)
            val actual = underTest.invoke(node)
            assertThat(actual).isInstanceOf(IncomingShareOfflineNodeInformation::class.java)
            assertThat((actual as? IncomingShareOfflineNodeInformation)?.incomingHandle)
                .isEqualTo(grandParent.id.longValue.toString())
        }

    @Test
    fun `test that invoke returns an OtherOfflineNodeInformation when node is not a shared node`() =
        runTest {
            stubNodes()
            stubFolderTree()
            whenever(node.isIncomingShare).thenReturn(true)
            val actual = underTest.invoke(node)
            assertThat(actual).isInstanceOf(IncomingShareOfflineNodeInformation::class.java)
        }

    private fun stubNodes() {
        whenever(node.id).thenReturn(nodeId)
        whenever(node.name).thenReturn(NODE_NAME)
        whenever(node.parentId).thenReturn(parentId)
        whenever(parent.id).thenReturn(parentId)
        whenever(parent.name).thenReturn(PARENT_NAME)
        whenever(parent.parentId).thenReturn(grandParentId)
        whenever(grandParent.id).thenReturn(grandParentId)
        whenever(grandParent.name).thenReturn(GRAND_PARENT_NAME)
    }

    private suspend fun stubFolderTree() {
        whenever(getNestedParentFoldersUseCase(node)).thenReturn(listOf(grandParent, parent))
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(isNodeInCloudDriveUseCase(any())).thenReturn(false)
    }

    private suspend fun stubInBackup() {
        whenever(isNodeInBackupsUseCase(any())).thenReturn(true)
    }

    private suspend fun stubInCloudDrive() {
        whenever(isNodeInCloudDriveUseCase(any())).thenReturn(true)
    }

    companion object {
        private val nodeId = NodeId(1L)
        private val parentId = NodeId(2L)
        private val grandParentId = NodeId(3L)
        private const val NODE_NAME = "node.txt"
        private const val PARENT_NAME = "parent"
        private const val GRAND_PARENT_NAME = "grand parent"
    }

}