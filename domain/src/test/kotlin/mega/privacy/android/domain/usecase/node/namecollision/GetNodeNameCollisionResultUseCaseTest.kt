package mega.privacy.android.domain.usecase.node.namecollision

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodeNameCollisionResultUseCaseTest {

    private lateinit var underTest: GetNodeNameCollisionResultUseCase

    private val getThumbnailUseCase = mock<GetThumbnailUseCase>()
    private val getNodeByHandleUseCase = mock<GetNodeByHandleUseCase>()
    private val getNodeNameCollisionRenameNameUseCase =
        mock<GetNodeNameCollisionRenameNameUseCase>()
    private val getChildNodeUseCase = mock<GetChildNodeUseCase>()
    private val nodeRepository = mock<NodeRepository>()

    private val defaultNodeNameCollision = NodeNameCollision.Default(
        collisionHandle = 123L,
        nodeHandle = 456L,
        name = "name",
        size = 789L,
        childFolderCount = 0,
        childFileCount = 0,
        lastModified = 123456L,
        parentHandle = 789L,
        isFile = true,
    )

    private val thumbFile = mock<File> {
        on { absolutePath } doReturn "path1"
    }
    private val thumbFile2 = mock<File> {
        on { absolutePath } doReturn "path2"
    }

    @BeforeAll
    fun setUp() {
        underTest = GetNodeNameCollisionResultUseCase(
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            getThumbnailUseCase = getThumbnailUseCase,
            getNodeNameCollisionRenameNameUseCase = getNodeNameCollisionRenameNameUseCase,
            nodeRepository = nodeRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getThumbnailUseCase,
            getChildNodeUseCase,
            getNodeByHandleUseCase,
            getThumbnailUseCase
        )
    }

    @Test
    fun `test that exception is thrown when collided node does not exists`() = runTest {
        whenever(getNodeByHandleUseCase(123L)).thenThrow(NodeDoesNotExistsException())
        assertThrows<NodeDoesNotExistsException> {
            underTest(nameCollision = defaultNodeNameCollision)
        }
    }

    @Test
    fun `test that collision result is returned correctly when collided node is a file`() =
        runTest {
            val collidedNode = mock<FileNode> {
                on { name } doReturn defaultNodeNameCollision.name
                on { id.longValue } doReturn defaultNodeNameCollision.collisionHandle
                on { modificationTime } doReturn 123456L
                on { size } doReturn 100L
            }
            val folderNode = mock<FolderNode> {
                on { id } doReturn NodeId(defaultNodeNameCollision.parentHandle)
            }
            whenever(
                getNodeByHandleUseCase(
                    defaultNodeNameCollision.collisionHandle,
                    true
                )
            ) doReturn collidedNode
            whenever(
                getNodeByHandleUseCase(
                    defaultNodeNameCollision.parentHandle,
                    false
                )
            ) doReturn folderNode

            whenever(getThumbnailUseCase(defaultNodeNameCollision.nodeHandle)) doReturn thumbFile
            whenever(getThumbnailUseCase(defaultNodeNameCollision.collisionHandle)) doReturn thumbFile2
            whenever(
                getChildNodeUseCase(
                    folderNode.id,
                    "${defaultNodeNameCollision.name} (1)"
                )
            ) doReturn null
            whenever(getNodeNameCollisionRenameNameUseCase(defaultNodeNameCollision)) doReturn
                    "${defaultNodeNameCollision.name} (1)"

            val result = underTest(nameCollision = defaultNodeNameCollision)

            assertThat(result.renameName).isEqualTo("${defaultNodeNameCollision.name} (1)")
            assertThat(result.thumbnail?.value).isEqualTo("path1")
            assertThat(result.collisionThumbnail?.value).isEqualTo("path2")
            assertThat(result.collisionSize).isEqualTo(100L)
            assertThat(result.nameCollision).isEqualTo(defaultNodeNameCollision)
            assertThat(result.collisionLastModified).isEqualTo(123456L)
            assertThat(result.collisionName).isEqualTo(defaultNodeNameCollision.name)
        }

    @Test
    fun `test that collision result is returned correctly when collided node is a folder`() =
        runTest {
            val collidedNode = mock<FolderNode> {
                on { name } doReturn defaultNodeNameCollision.name
                on { id.longValue } doReturn defaultNodeNameCollision.collisionHandle
                on { creationTime } doReturn 123456L
            }
            val folderNode = mock<FolderNode> {
                on { id } doReturn NodeId(defaultNodeNameCollision.parentHandle)
            }
            whenever(getNodeByHandleUseCase(123L, true)).thenReturn(collidedNode)
            whenever(
                getNodeByHandleUseCase(
                    defaultNodeNameCollision.parentHandle,
                    false
                )
            ).thenReturn(
                folderNode
            )
            whenever(
                getChildNodeUseCase(
                    folderNode.id,
                    "${defaultNodeNameCollision.name} (1)"
                )
            ) doReturn null
            whenever(nodeRepository.getFolderTreeInfo(folderNode)) doReturn mock<FolderTreeInfo>()
            whenever(getNodeNameCollisionRenameNameUseCase(defaultNodeNameCollision)) doReturn
                    "${defaultNodeNameCollision.name} (1)"

            val result = underTest(
                nameCollision = defaultNodeNameCollision
            )

            assertThat(result.renameName).isEqualTo("${defaultNodeNameCollision.name} (1)")
            assertThat(result.thumbnail).isNull()
            assertThat(result.collisionThumbnail).isNull()
            assertThat(result.collisionSize).isNull()
            assertThat(result.nameCollision).isEqualTo(defaultNodeNameCollision)
            assertThat(result.collisionLastModified).isEqualTo(123456L)
            assertThat(result.collisionName).isEqualTo(defaultNodeNameCollision.name)
        }

    @Test
    fun `test that current node thumbnail is not fetched when collision type is not NodeNameCollision`() =
        runTest {
            val uploadCollision = mock<FileNameCollision> {
                on { name } doReturn "file"
                on { collisionHandle } doReturn 123L
                on { parentHandle } doReturn 456L
            }
            val nodeNameCollision = defaultNodeNameCollision.copy(
                parentHandle = -1L
            )
            val folderNode = mock<FolderNode> {
                on { id } doReturn NodeId(nodeNameCollision.parentHandle)
            }
            val collidedNode = mock<FileNode> {
                on { name } doReturn "file"
                on { id.longValue } doReturn 355L
                on { modificationTime } doReturn 123456L
                on { size } doReturn 100L
            }
            whenever(getNodeByHandleUseCase(123L, true)).thenReturn(collidedNode)
            whenever(getNodeByHandleUseCase(456L, false)).thenReturn(
                folderNode
            )

            underTest(uploadCollision)

            verify(getThumbnailUseCase, never()).invoke(123L)
            verify(getThumbnailUseCase).invoke(355L)
        }
}