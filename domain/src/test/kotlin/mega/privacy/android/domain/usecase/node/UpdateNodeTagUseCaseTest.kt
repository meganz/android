package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class UpdateNodeTagUseCaseTest {

    private val nodeRepository = mock<NodeRepository>()
    private val updateNodeTagUseCase = UpdateNodeTagUseCase(nodeRepository)

    @Test
    fun `test that when newTag is null and oldTag is not null, removeNodeTag is called`() =
        runTest {
            val nodeHandle = NodeId(1)
            val oldTag = "oldTag"
            val newTag = null

            updateNodeTagUseCase(nodeHandle, oldTag, newTag)

            verify(nodeRepository).removeNodeTag(nodeHandle, oldTag)
        }

    @Test
    fun `test that when newTag is not null and oldTag is null, addNodeTag is called`() =
        runTest {
            val nodeHandle = NodeId(1)
            val oldTag = null
            val newTag = "newTag"

            updateNodeTagUseCase(nodeHandle, oldTag, newTag)

            verify(nodeRepository).addNodeTag(nodeHandle, newTag)
        }

    @Test
    fun `test that when newTag is not null and oldTag is not null, updateNodeTag is called`() =
        runTest {
            val nodeHandle = NodeId(1)
            val oldTag = "oldTag"
            val newTag = "newTag"

            updateNodeTagUseCase(nodeHandle, oldTag, newTag)

            verify(nodeRepository).updateNodeTag(nodeHandle, newTag, oldTag)
        }

    @Test
    fun `test that when both newTag and oldTag are null, IllegalArgumentException is thrown`() =
        runTest {
            val nodeHandle = NodeId(1)
            val oldTag = null
            val newTag = null

            assertThrows<IllegalArgumentException> {
                updateNodeTagUseCase(nodeHandle, oldTag, newTag)
            }
        }
}