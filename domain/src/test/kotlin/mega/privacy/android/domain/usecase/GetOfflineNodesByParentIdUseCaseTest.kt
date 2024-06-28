package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.offline.GetOfflineFileInformationUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GetOfflineNodesByParentIdUseCaseTest {
    private val nodeRepository: NodeRepository = mock()
    private val sortOfflineInfoUseCase: SortOfflineInfoUseCase = mock()
    private val getOfflineFileInformationUseCase: GetOfflineFileInformationUseCase = mock()

    private val underTest = GetOfflineNodesByParentIdUseCase(
        nodeRepository = nodeRepository,
        sortOfflineInfoUseCase = sortOfflineInfoUseCase,
        getOfflineFileInformationUseCase = getOfflineFileInformationUseCase
    )

    @Test
    fun `test that invoke returns empty list when getOfflineNodeByParentId returns empty list`() =
        runTest {
            val parentId = 1
            whenever(nodeRepository.getOfflineNodesByParentId(parentId)).thenReturn(emptyList())
            val result = underTest.invoke(parentId)
            assertThat(result).isEmpty()
            verify(nodeRepository).getOfflineNodesByParentId(parentId)
        }

    @Test
    fun `test that getOfflineNodesByQuery is called when searchQuery is given`() = runTest {
        val parentId = 1
        val searchQuery = "searchQuery"
        whenever(
            nodeRepository.getOfflineNodesByQuery(
                searchQuery,
                parentId
            )
        ).thenReturn(emptyList())
        underTest.invoke(parentId, searchQuery)
        verify(nodeRepository).getOfflineNodesByQuery(searchQuery, parentId)
    }

    @Test
    fun `test that invoke returns list of offline file information when getOfflineNodeByParentId returns list of offline nodes`() =
        runTest {
            val parentId = 1
            val list = listOf(mock<OtherOfflineNodeInformation>())
            val expected = listOf(mock<OfflineFileInformation>())
            whenever(nodeRepository.getOfflineNodesByParentId(parentId)).thenReturn(list)
            whenever(sortOfflineInfoUseCase.invoke(any())).thenReturn(list)
            whenever(getOfflineFileInformationUseCase.invoke(any(), any())).thenReturn(expected[0])
            val result = underTest.invoke(parentId)
            assertThat(expected).isEqualTo(result)
        }

    @AfterEach
    fun resetMocks() {
        reset(nodeRepository, sortOfflineInfoUseCase, getOfflineFileInformationUseCase)
    }
}