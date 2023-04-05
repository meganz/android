package mega.privacy.android.domain.usecase.shares

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetInShareUseCaseTest {
    private val nodeRepository = mock<NodeRepository>()
    private val underTest = GetInSharesUseCase(nodeRepository)
    private val testEmail = "test@mega.nz"

    @Test
    fun `test that get in shares list of nodes when success`() = runTest {
        val nodeList = mock<List<UnTypedNode>> {
            on { size }.thenReturn(8)
        }
        whenever(nodeRepository.getInShares(testEmail)).thenReturn(nodeList)
        val actual = underTest(testEmail)
        val expected = 8
        Truth.assertThat(actual.size).isEqualTo(expected)
    }
}