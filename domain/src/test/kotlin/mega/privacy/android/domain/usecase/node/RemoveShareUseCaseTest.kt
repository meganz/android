package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveShareUseCaseTest {
    private lateinit var underTest: RemoveShareUseCase
    private val nodeRepository: NodeRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = RemoveShareUseCase(nodeRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that when nodeRepository's getNodeOutgoingShares returns an empty list then result returns correctly`() =
        runTest {
            whenever(nodeRepository.getNodeOutgoingShares(NodeId(any()))).thenReturn(emptyList())
            val actual = underTest(listOf(NodeId(1L)))
            Truth.assertThat(actual.errorCount).isEqualTo(0)
            Truth.assertThat(actual.successCount).isEqualTo(0)
        }

    @Test
    fun `test that when nodeRepository's getNodeOutgoingShares returns a list and call setShareAccess success then result returns correctly`() =
        runTest {
            val ids = listOf(NodeId(1L), NodeId(2L))
            val shares1 = listOf(mock<ShareData> {
                on { it.nodeHandle }.thenReturn(ids.first().longValue)
                on { it.user }.thenReturn("email1")
            })
            val shares2 = listOf(mock<ShareData> {
                on { it.nodeHandle }.thenReturn(ids.last().longValue)
                on { it.user }.thenReturn("email2")
            })
            whenever(nodeRepository.getNodeOutgoingShares(ids.first())).thenReturn(shares1)
            whenever(nodeRepository.getNodeOutgoingShares(ids.last())).thenReturn(shares2)
            whenever(nodeRepository.setShareAccess(NodeId(any()), any(), any())).thenReturn(Unit)
            val actual = underTest(ids)
            Truth.assertThat(actual.errorCount).isEqualTo(0)
            Truth.assertThat(actual.successCount).isEqualTo(2)
        }

    @Test
    fun `test that when nodeRepository's getNodeOutgoingShares returns a list and call setShareAccess with one failed then result returns correctly`() =
        runTest {
            val ids = listOf(NodeId(1L), NodeId(2L))
            val shares1 = listOf(mock<ShareData> {
                on { it.nodeHandle }.thenReturn(ids.first().longValue)
                on { it.user }.thenReturn("email1")
            })
            val shares2 = listOf(mock<ShareData> {
                on { it.nodeHandle }.thenReturn(ids.last().longValue)
                on { it.user }.thenReturn("email2")
            })
            whenever(nodeRepository.getNodeOutgoingShares(ids.first())).thenReturn(shares1)
            whenever(nodeRepository.getNodeOutgoingShares(ids.last())).thenReturn(shares2)
            whenever(
                nodeRepository.setShareAccess(
                    nodeId = ids.first(),
                    accessPermission = AccessPermission.UNKNOWN,
                    email = "email1"
                )
            ).thenReturn(Unit)
            whenever(
                nodeRepository.setShareAccess(
                    nodeId = ids.last(),
                    accessPermission = AccessPermission.UNKNOWN,
                    email = "email2"
                )
            ).thenThrow(RuntimeException::class.java)
            val actual = underTest(ids)
            Truth.assertThat(actual.errorCount).isEqualTo(1)
            Truth.assertThat(actual.successCount).isEqualTo(1)
        }
}