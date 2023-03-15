package mega.privacy.android.domain.usecase.filenode

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.exception.VersionsNotDeletedException
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
internal class DefaultDeleteNodeVersionsByHandleJunit5ExampleTest {
    private lateinit var underTest: DeleteNodeVersionsByHandle

    private lateinit var nodeRepository: NodeRepository

    @BeforeEach
    fun setUp() {
        nodeRepository = mock()
        underTest = DefaultDeleteNodeVersionsByHandle(nodeRepository)
    }

    @Nested
    @DisplayName("Test that when DeleteNodeVersionsByHandle works well")
    inner class DeleteNodeSuccessTests {
        @Test
        fun `then nodeRepository_deleteVersion is called for each version except last one`() =
            runTest {
                whenever(nodeRepository.getNodeHistoryVersions(nodeId)).thenReturn(versions)
                underTest.invoke(nodeId)
                val versionIdsToDelete = versionIds.drop(1)
                versionIdsToDelete.forEach {
                    verify(nodeRepository).deleteNodeVersionByHandle(it)
                }
            }
    }

    @Nested
    @DisplayName("Test that when some versions are not deleted ")
    inner class DeleteNodeFailTests {

        private lateinit var failDelete: List<NodeId>

        @BeforeEach
        fun setup() = runBlocking {
            whenever(nodeRepository.getNodeHistoryVersions(nodeId)).thenReturn(versions)
            val successDelete = versionIds.filter { it.longValue.mod(2) == 0 }
            failDelete = versionIds - successDelete.toSet()
            successDelete.forEach {
                whenever(nodeRepository.deleteNodeVersionByHandle(it)).thenReturn(Unit)
            }
            failDelete.forEach {
                whenever(nodeRepository.deleteNodeVersionByHandle(it)).thenThrow(
                    RuntimeException::class.java
                )
            }
        }

        @Test
        fun `then an exception is returned`() =
            runTest {
                val exception = runCatching { underTest.invoke(nodeId) }.exceptionOrNull()
                Truth.assertThat(exception).isNotNull()
            }

        @Test
        fun `then the exception is of type VersionsNotDeletedException`() =
            runTest {
                val exception = runCatching { underTest.invoke(nodeId) }.exceptionOrNull()
                Truth.assertThat(exception).isInstanceOf(VersionsNotDeletedException::class.java)
            }

        @Test
        fun `then the totalRequestedToDelete amount is returned trough the exception`() =
            runTest {
                val exception = runCatching { underTest.invoke(nodeId) }.exceptionOrNull()
                (exception as? VersionsNotDeletedException)?.let {
                    Truth.assertThat(it.totalRequestedToDelete).isEqualTo(versionIds.size - 1)
                }
            }

        @Test
        fun `then the totalNotDeleted amount is returned trough the exception`() =
            runTest {
                val exception = runCatching { underTest.invoke(nodeId) }.exceptionOrNull()
                (exception as? VersionsNotDeletedException)?.let {
                    Truth.assertThat(it.totalNotDeleted).isEqualTo(failDelete.size)
                }
            }
    }

    companion object {
        private val nodeId = NodeId(1L)
        private val versionIds = (10L..15L).map { NodeId(it) }
        private val versions = versionIds.map { createVersionNodeMock(it) }

        private fun createVersionNodeMock(id: NodeId): UnTypedNode = mock<UnTypedNode>().also {
            whenever(it.id).thenReturn(id)
        }
    }
}