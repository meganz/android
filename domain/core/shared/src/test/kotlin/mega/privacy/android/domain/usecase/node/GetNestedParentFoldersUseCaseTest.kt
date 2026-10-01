package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNestedParentFoldersUseCaseTest {

    private val getParentNodeUseCase: GetParentNodeUseCase = mock()

    private val node = mock<FileNode>()
    private val parent = mock<FolderNode>()
    private val grandParent = mock<FolderNode>()


    private lateinit var underTest: GetNestedParentFoldersUseCase

    @BeforeAll
    fun setup() {
        underTest = GetNestedParentFoldersUseCase(getParentNodeUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getParentNodeUseCase,
            node, parent, grandParent,
        )
    }

    @Test
    fun `test that invoke returns nested parents until parent is null`() = runTest {
        stubNodes()
        stubFolderTree()
        Truth.assertThat(underTest.invoke(node)).containsExactly(grandParent, parent)
    }

    @Test
    fun `test that invoke returns nested parents in the correct order`() = runTest {
        stubNodes()
        stubFolderTree()
        Truth.assertThat(underTest.invoke(node).first()).isEqualTo(grandParent)
        Truth.assertThat(underTest.invoke(node)[1]).isEqualTo(parent)
    }

    @Test
    fun `test that join as path return the correct path`() = runTest {
        stubNodes()
        stubFolderTree()
        Truth.assertThat(underTest.invoke(node).joinAsPath())
            .isEqualTo(File.separator + GRAND_PARENT_NAME + File.separator + PARENT_NAME + File.separator)
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

    companion object {
        private val nodeId = NodeId(1L)
        private val parentId = NodeId(2L)
        private val grandParentId = NodeId(3L)
        private const val NODE_NAME = "node.txt"
        private const val PARENT_NAME = "parent"
        private const val GRAND_PARENT_NAME = "grand parent"
    }
}