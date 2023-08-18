package mega.privacy.android.domain.usecase.shares

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetIncomingShareParentUserEmailUseCaseTest {

    private val nodeRepository = mock<NodeRepository>()

    private val underTest = GetIncomingShareParentUserEmailUseCase(nodeRepository)

    private val nodeHandle = -1L
    private val nodeId = NodeId(nodeHandle)
    private val userEmail = "user@mega.com"

    @Test
    fun `test that getIncomingShareParentUserEmail returns valid email if node repository returns valid response`() =
        runTest {
            whenever(nodeRepository.getIncomingShareParentUserEmail(nodeId)).thenReturn(userEmail)
            val actual = underTest(nodeId)
            Truth.assertThat(actual).isEqualTo(userEmail)
        }

    @Test
    fun `test that getIncomingShareParentUserEmail returns null if node repository returns invalid response`() =
        runTest {
            whenever(nodeRepository.getIncomingShareParentUserEmail(nodeId)).thenReturn(null)
            val actual = underTest(nodeId)
            Truth.assertThat(actual).isNull()
        }
}