package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateNodeFavoriteUseCaseTest {
    private val nodeRepository: NodeRepository = mock()
    private val underTest = UpdateNodeFavoriteUseCase(nodeRepository)

    @ParameterizedTest(name = "isFavorite set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that updateFavoriteNode is called when invoked`(
        isFavorite: Boolean,
    ) = runTest {
        underTest(nodeId = nodeId, isFavorite = isFavorite)
        verify(nodeRepository).updateFavoriteNode(nodeId = nodeId, isFavorite = isFavorite)
    }

    companion object {
        val nodeId = NodeId(1L)
    }
}