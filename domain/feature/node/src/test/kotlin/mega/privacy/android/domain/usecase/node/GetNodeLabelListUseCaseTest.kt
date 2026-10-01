package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class GetNodeLabelListUseCaseTest {
    private val nodeRepository: NodeRepository = mock()

    private val underTest = GetNodeLabelListUseCase(nodeRepository)

    @Test
    fun `test that while getting node list it executes getNodeLabelList at least once`() = runTest {
        underTest()
        verify(nodeRepository).getNodeLabelList()
    }
}