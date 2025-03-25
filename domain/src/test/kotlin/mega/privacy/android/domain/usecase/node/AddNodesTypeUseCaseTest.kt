package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetFolderType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddNodesTypeUseCaseTest {

    private val getFolderType: GetFolderType = mock()
    private lateinit var addNodesTypeUseCase: AddNodesTypeUseCase

    @BeforeAll
    fun setUp() {
        addNodesTypeUseCase =
            AddNodesTypeUseCase(getFolderType = getFolderType)
    }

    @BeforeEach
    fun resetMocks() {
        reset(getFolderType)
    }

    @Test
    fun `test when node is already a TypedNode`() = runTest {
        val typedNode = mock<DefaultTypedFolderNode> {
            on { id }.thenReturn(NodeId(1))
        }
        val nodes = listOf(typedNode)

        val result = addNodesTypeUseCase(nodes)

        assertThat(result).isEqualTo(nodes)
    }

    @Test
    fun `test when node is a FolderNode and device is not null`() = runTest {
        val folderNode = mock<FolderNode> {
            on { id }.thenReturn(NodeId(1))
            on { parentId }.thenReturn(NodeId(2))
            on { device }.thenReturn("device")
        }
        val nodes = listOf(folderNode)

        whenever(getFolderType(folderNode)).thenReturn(FolderType.DeviceBackup(DeviceType.Unknown))

        val result = addNodesTypeUseCase(nodes)

        assertThat(result.first()).isInstanceOf(DefaultTypedFolderNode::class.java)
        assertThat((result.first() as DefaultTypedFolderNode).type).isInstanceOf(FolderType.DeviceBackup::class.java)
    }

    @Test
    fun `test when node is a FolderNode and parent is in backup`() = runTest {
        val folderNode = mock<FolderNode> {
            on { id }.thenReturn(NodeId(1))
            on { parentId }.thenReturn(NodeId(2))
        }
        val nodes = listOf(folderNode)

        whenever(getFolderType(folderNode)).thenReturn(FolderType.ChildBackup)

        val result = addNodesTypeUseCase(nodes)

        assertThat(result.first()).isInstanceOf(DefaultTypedFolderNode::class.java)
        assertThat((result.first() as DefaultTypedFolderNode).type).isEqualTo(FolderType.ChildBackup)
    }

    @Test
    fun `test when node is a FolderNode and type is MediaSyncFolder`() = runTest {
        val folderNode = mock<FolderNode> {
            on { id }.thenReturn(NodeId(1))
            on { parentId }.thenReturn(NodeId(2))
        }
        val nodes = listOf(folderNode)

        whenever(getFolderType(folderNode)).thenReturn(FolderType.MediaSyncFolder)

        val result = addNodesTypeUseCase(nodes)

        assertThat(result.first()).isInstanceOf(DefaultTypedFolderNode::class.java)
        assertThat((result.first() as DefaultTypedFolderNode).type).isEqualTo(FolderType.MediaSyncFolder)
    }

    @Test
    fun `test when node is a FolderNode and type is ChatFilesFolder`() = runTest {
        val folderNode = mock<FolderNode> {
            on { id }.thenReturn(NodeId(1))
            on { parentId }.thenReturn(NodeId(2))
        }
        val nodes = listOf(folderNode)

        whenever(getFolderType(folderNode)).thenReturn(FolderType.ChatFilesFolder)

        val result = addNodesTypeUseCase(nodes)

        assertThat(result.first()).isInstanceOf(DefaultTypedFolderNode::class.java)
        assertThat((result.first() as DefaultTypedFolderNode).type).isEqualTo(FolderType.ChatFilesFolder)
    }

    @Test
    fun `test when node is a FolderNode and type is RootBackup`() = runTest {
        val folderNode = mock<FolderNode> {
            on { id }.thenReturn(NodeId(1))
            on { parentId }.thenReturn(NodeId(2))
        }
        val nodes = listOf(folderNode)

        whenever(getFolderType(folderNode)).thenReturn(FolderType.RootBackup)

        val result = addNodesTypeUseCase(nodes)

        assertThat(result.first()).isInstanceOf(DefaultTypedFolderNode::class.java)
        assertThat((result.first() as DefaultTypedFolderNode).type).isEqualTo(FolderType.RootBackup)
    }

    @Test
    fun `test when node is a FolderNode and type is Default`() = runTest {
        val folderNode = mock<FolderNode> {
            on { id }.thenReturn(NodeId(1))
            on { parentId }.thenReturn(NodeId(2))
        }
        val nodes = listOf(folderNode)

        whenever(getFolderType(folderNode)).thenReturn(FolderType.Default)

        val result = addNodesTypeUseCase(nodes)

        assertThat(result.first()).isInstanceOf(DefaultTypedFolderNode::class.java)
        assertThat((result.first() as DefaultTypedFolderNode).type).isEqualTo(FolderType.Default)
    }
}