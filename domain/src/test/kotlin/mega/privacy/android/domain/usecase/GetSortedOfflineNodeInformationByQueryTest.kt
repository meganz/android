package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetSortedOfflineNodeInformationByQueryTest {
    private val nodeRepository: NodeRepository = mock()
    private val underTest = GetSortedOfflineNodeInformationByQuery(nodeRepository = nodeRepository)

    @Test
    fun `test that the list of offline node information is returned`() = runTest {
        val searchQuery = "Some data"
        val offlineInfo1 = mock<OtherOfflineNodeInformation> {
            whenever(it.isFolder).thenReturn(true)
        }
        val offlineInfo2 = mock<OtherOfflineNodeInformation> {
            whenever(it.isFolder).thenReturn(false)
        }
        whenever(nodeRepository.getOfflineByQuery(searchQuery)).thenReturn(
            listOf(offlineInfo1, offlineInfo2)
        )
        assertThat(underTest.invoke(searchQuery)).hasSize(2)
    }
}