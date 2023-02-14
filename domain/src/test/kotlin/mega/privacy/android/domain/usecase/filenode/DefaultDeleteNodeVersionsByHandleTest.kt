package mega.privacy.android.domain.usecase.filenode

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.exception.VersionsNotDeletedException
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
internal class DefaultDeleteNodeVersionsByHandleTest {
    private lateinit var underTest: DeleteNodeVersionsByHandle

    private lateinit var nodeRepository: NodeRepository

    @Before
    fun setUp() {
        nodeRepository = mock()
        underTest = DefaultDeleteNodeVersionsByHandle(nodeRepository)
    }

    @Test
    fun `test that nodeRepository_deleteVersion is called for each version except last one`() =
        runTest {
            whenever(nodeRepository.getNodeHistoryVersions(nodeId)).thenReturn(versions)
            underTest.invoke(nodeId)
            val versionIdsToDelete = versionIds.drop(1)
            versionIdsToDelete.forEach {
                verify(nodeRepository).deleteNodeVersionByHandle(it)
            }
        }

    @Test
    fun `test that the number of not deleted versions is returned as an exception with correct amount`() =
        runTest {
            whenever(nodeRepository.getNodeHistoryVersions(nodeId)).thenReturn(versions)
            val successDelete = versionIds.filter { it.longValue.mod(2) == 0 }
            val failDelete = versionIds - successDelete.toSet()
            successDelete.forEach {
                whenever(nodeRepository.deleteNodeVersionByHandle(it)).thenReturn(Unit)
            }
            failDelete.forEach {
                whenever(nodeRepository.deleteNodeVersionByHandle(it)).thenThrow(RuntimeException::class.java)
            }
            val exception = runCatching { underTest.invoke(nodeId) }.exceptionOrNull()
            Truth.assertThat(exception).isNotNull()
            Truth.assertThat(exception).isInstanceOf(VersionsNotDeletedException::class.java)
            (exception as? VersionsNotDeletedException)?.let {
                Truth.assertThat(it.totalRequestedToDelete).isEqualTo(versionIds.size - 1)
                Truth.assertThat(it.totalNotDeleted).isEqualTo(failDelete.size)
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