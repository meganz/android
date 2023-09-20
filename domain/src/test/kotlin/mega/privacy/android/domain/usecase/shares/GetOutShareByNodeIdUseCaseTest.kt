package mega.privacy.android.domain.usecase.shares

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetOutShareByNodeIdUseCaseTest {
    private lateinit var underTest: GetOutShareByNodeIdUseCase
    private val nodeRepository: NodeRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = GetOutShareByNodeIdUseCase(nodeRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that when nodeRepository's getNodeOutgoingShares returns an empty list then an empty list is returned`() =
        runTest {
            whenever(nodeRepository.getNodeOutgoingShares(NodeId(any()))).thenReturn(emptyList())
            val actual = underTest(NodeId(1L))
            Truth.assertThat(actual).isEmpty()
        }

    @Test
    fun `test that when nodeRepository's getNodeOutgoingShares returns a list then a list return correctly`() =
        runTest {
            val shares = listOf(mock<ShareData> {
                on { it.nodeHandle }.thenReturn(1L)
                on { it.user }.thenReturn("email1")
            })
            whenever(nodeRepository.getNodeOutgoingShares(NodeId(any()))).thenReturn(shares)
            val actual = underTest(NodeId(1L))
            Truth.assertThat(actual).isEqualTo(shares)
        }
}