package mega.privacy.android.domain.usecase.node.namecollision

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodeNameCollisionRenameNameUseCaseTest {

    private lateinit var underTest: GetNodeNameCollisionRenameNameUseCase

    private val getNodeByHandleUseCase = mock<GetNodeByHandleUseCase>()
    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()
    private val getChildNodeUseCase = mock<GetChildNodeUseCase>()
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
        type = NodeNameCollisionType.COPY
    )

    @BeforeAll
    fun setUp() {
        underTest = GetNodeNameCollisionRenameNameUseCase(
            getNodeByHandleUseCase,
            getRootNodeUseCase,
            getChildNodeUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getNodeByHandleUseCase,
            getRootNodeUseCase,
            getChildNodeUseCase
        )
    }

    @Test
    fun `test root node is fetched when parent handle is -1L`() = runTest {
        val nodeNameCollision = defaultNodeNameCollision.copy(
            parentHandle = -1L
        )
        val folderNode = mock<FolderNode> {
            on { id } doReturn NodeId(nodeNameCollision.parentHandle)
        }
        whenever(getRootNodeUseCase()).thenReturn(folderNode)
        whenever(
            getChildNodeUseCase(
                folderNode.id,
                "${nodeNameCollision.name} (1)"
            )
        ) doReturn null

        underTest(nameCollision = nodeNameCollision)

        verify(getRootNodeUseCase).invoke()
    }


    @Test
    fun `test exception is thrown when parent doesn't found`() = runTest {
        val nodeNameCollision = defaultNodeNameCollision.copy(
            parentHandle = -1L
        )
        val folderNode = mock<FolderNode> {
            on { id } doReturn NodeId(nodeNameCollision.parentHandle)
        }
        whenever(getRootNodeUseCase()).thenThrow(NodeDoesNotExistsException())
        whenever(getNodeByHandleUseCase(nodeNameCollision.parentHandle)).thenReturn(
            folderNode
        )
        whenever(
            getChildNodeUseCase(
                folderNode.id,
                "${nodeNameCollision.name}(1)"
            )
        ) doReturn null

        assertThrows<NodeDoesNotExistsException> {
            underTest(nameCollision = nodeNameCollision)
        }
    }

    @ParameterizedTest
    @CsvSource(
        "file.txt, file (1).txt",
        "image.jpg, image (1).jpg",
        "document (5).pdf, document (6).pdf",
        "noextension, noextension (1)",
        "folder, folder (1)",
    )
    fun `test possible rename name is generated correctly`(fileName: String, newName: String) =
        runTest {
            val parentNodeId = NodeId(789L)
            val nodeNameCollision = NodeNameCollision.Default(
                collisionHandle = 123L,
                nodeHandle = 456L,
                name = fileName,
                size = 789L,
                childFolderCount = 0,
                childFileCount = 0,
                lastModified = 123456L,
                parentHandle = parentNodeId.longValue,
                isFile = true,
                type = NodeNameCollisionType.COPY
            )
            val folderNode = mock<FolderNode> {
                on { id } doReturn parentNodeId
            }
            whenever(getNodeByHandleUseCase(parentNodeId.longValue)).thenReturn(
                folderNode
            )
            whenever(
                getChildNodeUseCase(
                    parentNodeId,
                    fileName
                )
            ) doReturn mock<FileNode>()
            whenever(
                getChildNodeUseCase(
                    parentNodeId,
                    newName
                )
            ) doReturn null

            val result = underTest(nameCollision = nodeNameCollision)

            assertThat(result).isEqualTo(newName)
        }

}