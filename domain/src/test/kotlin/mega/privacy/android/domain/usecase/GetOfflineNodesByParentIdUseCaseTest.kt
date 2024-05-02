package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetOfflineNodesByParentIdUseCaseTest {
    private val nodeRepository: NodeRepository = mock()
    private val sortOfflineInfoUseCase: SortOfflineInfoUseCase = mock()
    private val underTest = GetOfflineNodesByParentIdUseCase(
        nodeRepository = nodeRepository,
        sortOfflineInfoUseCase = sortOfflineInfoUseCase
    )

    @Test
    fun `test that the list of offline nodes are returned`() = runTest {
        val parentId = 1
        whenever(nodeRepository.getOfflineNodeByParentId(parentId)).thenReturn(emptyList())
        assertThat(underTest.invoke(parentId)).isEmpty()
    }
}