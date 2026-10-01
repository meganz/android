package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class GetAllNodeTagsUseCaseTest {
    private val nodeRepository = mock<NodeRepository>()
    private val underTest = GetAllNodeTagsUseCase(
        nodeRepository = nodeRepository
    )

    @Test
    fun `test get all node tags use case triggers getAllNodeTags method from repository`() =
        runTest {
            val searchString = "searchString"

            underTest(searchString)

            verify(nodeRepository).getAllNodeTags(searchString)
        }
}