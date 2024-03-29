package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetDeviceType
import mega.privacy.android.domain.usecase.HasAncestor
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddNodesTypeUseCaseTest {

    private val getGroupFolderTypeUseCase: GetGroupFolderTypeUseCase = mock()
    private val getDeviceType: GetDeviceType = mock()
    private val hasAncestor: HasAncestor = mock()
    private lateinit var addNodesTypeUseCase: AddNodesTypeUseCase

    @BeforeAll
    fun setUp() {
        addNodesTypeUseCase =
            AddNodesTypeUseCase(getGroupFolderTypeUseCase, getDeviceType, hasAncestor)
    }

    @BeforeEach
    fun resetMocks() {
        reset(getGroupFolderTypeUseCase, getDeviceType, hasAncestor)
    }

    @Test
    fun `test when node is already a TypedNode`() = runTest {
        val typedNode = mock<DefaultTypedFolderNode> {
            on { id }.thenReturn(NodeId(1))
        }
        val nodes = listOf(typedNode)

        whenever(getGroupFolderTypeUseCase()).thenReturn(emptyMap())
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
        val groupFolderTypes = mapOf(NodeId(3) to FolderType.MediaSyncFolder)

        whenever(getGroupFolderTypeUseCase()).thenReturn(groupFolderTypes)
        whenever(getDeviceType(folderNode)).thenReturn(DeviceType.Mac)

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
        val groupFolderTypes = mapOf(
            NodeId(3) to FolderType.MediaSyncFolder,
            NodeId(4) to FolderType.RootBackup
        )

        whenever(getGroupFolderTypeUseCase()).thenReturn(groupFolderTypes)
        whenever(hasAncestor(NodeId(2), NodeId(4))).thenReturn(true)

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
        val groupFolderTypes = mapOf(NodeId(1) to FolderType.MediaSyncFolder)

        whenever(getGroupFolderTypeUseCase()).thenReturn(groupFolderTypes)
        whenever(hasAncestor(NodeId(1), NodeId(2))).thenReturn(false)

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
        val groupFolderTypes = mapOf(NodeId(1) to FolderType.ChatFilesFolder)

        whenever(getGroupFolderTypeUseCase()).thenReturn(groupFolderTypes)
        whenever(hasAncestor(NodeId(1), NodeId(2))).thenReturn(false)

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
        val groupFolderTypes = mapOf(NodeId(1) to FolderType.RootBackup)

        whenever(getGroupFolderTypeUseCase()).thenReturn(groupFolderTypes)
        whenever(hasAncestor(NodeId(2), NodeId(1))).thenReturn(false)

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
        val groupFolderTypes = emptyMap<NodeId, FolderType>()

        whenever(getGroupFolderTypeUseCase()).thenReturn(groupFolderTypes)
        whenever(hasAncestor(NodeId(1), NodeId(2))).thenReturn(false)

        val result = addNodesTypeUseCase(nodes)

        assertThat(result.first()).isInstanceOf(DefaultTypedFolderNode::class.java)
        assertThat((result.first() as DefaultTypedFolderNode).type).isEqualTo(FolderType.Default)
    }
}