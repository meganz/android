package mega.privacy.android.domain.usecase.offline

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.InboxOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.IsNodeInInbox
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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

    private val nodeRepository: NodeRepository = mock()
    private val getParentNodeUseCase: GetParentNodeUseCase = mock()
    private val isNodeInInbox: IsNodeInInbox = mock()
    private val node = mock<FileNode>()
    private val parent = mock<FolderNode>()
    private val grandParent = mock<FolderNode>()

    private lateinit var underTest: GetOfflineNodeInformationUseCase

    @BeforeAll
    fun setup() {
        underTest = GetOfflineNodeInformationUseCase(
            nodeRepository, getParentNodeUseCase, isNodeInInbox
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            nodeRepository, getParentNodeUseCase, isNodeInInbox,
            node, parent, grandParent
        )
    }

    @Nested
    @DisplayName("Nodes  in backup")
    inner class InBackup {
        @BeforeEach
        fun setup() = runTest {
            stubInBackup()
        }

        @Test
        fun `test that invoke returns the correct path and name when node is a backup file`() =
            runTest {
                stubNodes()
                stubFolderTree()
                val actual = underTest.invoke(node)
                assertThat(actual.path).isEqualTo(
                    File.separator + PARENT_NAME + File.separator
                )
                assertThat(actual.name).isEqualTo(NODE_NAME)
            }

        @Test
        fun `test that invoke returns the correct path and name when node is a backup folder`() =
            runTest {
                stubNodes()
                stubFolderTree()
                val actual = underTest.invoke(parent)
                assertThat(actual.path).isEqualTo(
                    File.separator
                )
                assertThat(actual.name).isEqualTo(PARENT_NAME)
            }

        @Test
        fun `test that invoke does not include root backup node when node is a backup file`() =
            runTest {
                stubNodes()
                stubFolderTree()
                whenever(nodeRepository.getBackupFolderId()).thenReturn(parentId)
                assertThat(underTest.invoke(node).path).doesNotContain(GRAND_PARENT_NAME)
            }

        @Test
        fun `test that invoke returns an InboxOfflineNodeInformation when node is a backup file`() =
            runTest {
                stubNodes()
                stubFolderTree()
                val actual = underTest.invoke(node)
                assertThat(actual).isInstanceOf(InboxOfflineNodeInformation::class.java)
            }
    }

    @Nested
    @DisplayName("Nodes not in backup")
    inner class NotInBackup {

        @BeforeEach
        fun setup() = runTest {
            stubNotInBackup()
        }

        @Test
        fun `test that invoke returns correct path and name when node is a drive file`() =
            runTest {
                stubNodes()
                stubFolderTree()
                val actual = underTest.invoke(node)
                assertThat(actual.path).isEqualTo(
                    File.separator +
                            GRAND_PARENT_NAME + File.separator +
                            PARENT_NAME + File.separator
                )
                assertThat(actual.name).isEqualTo(NODE_NAME)
            }

        @Test
        fun `test that invoke returns the correct path and name when node is a drive folder`() =
            runTest {
                stubNodes()
                stubFolderTree()
                val actual = underTest.invoke(parent)
                assertThat(actual.path).isEqualTo(
                    File.separator + GRAND_PARENT_NAME + File.separator
                )
                assertThat(actual.name).isEqualTo(PARENT_NAME)
            }

        @Test
        fun `test that invoke does not include root drive node when node is a backup file`() =
            runTest {
                stubNodes()
                stubFolderTree()
                whenever(nodeRepository.getRootNode()).thenReturn(grandParent)
                assertThat(underTest.invoke(node).path).doesNotContain(GRAND_PARENT_NAME)
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
        whenever(getParentNodeUseCase(nodeId)).thenReturn(parent)
        whenever(getParentNodeUseCase(parentId)).thenReturn(grandParent)
    }

    private suspend fun stubNotInBackup() {
        whenever(isNodeInInbox(any())).thenReturn(false)
        whenever(nodeRepository.getBackupFolderId()).thenReturn(invalidId)
    }

    private suspend fun stubInBackup() {
        whenever(isNodeInInbox(any())).thenReturn(true)
        whenever(nodeRepository.getBackupFolderId()).thenReturn(grandParentId)
    }

    companion object {
        private val nodeId = NodeId(1L)
        private val parentId = NodeId(2L)
        private val grandParentId = NodeId(3L)
        private val invalidId = NodeId(-1L)
        private const val NODE_NAME = "node.txt"
        private const val PARENT_NAME = "parent"
        private const val GRAND_PARENT_NAME = "grand parent"
    }

}