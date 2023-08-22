package test.mega.privacy.android.app.domain.usecase.search

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.search.GetSearchOutSharesNodesUseCase
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetSearchOutSharesNodesUseCaseTest {
    private lateinit var underTest: GetSearchOutSharesNodesUseCase
    private val megaNodeRepository: MegaNodeRepository = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()

    @Before
    fun setUp() {
        underTest = GetSearchOutSharesNodesUseCase(
            megaNodeRepository = megaNodeRepository,
            getCloudSortOrder = getCloudSortOrder
        )
    }

    @Test
    fun `test when search in out shares returns empty response`() = runTest {
        val query = "SomeQuerry"
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            megaNodeRepository.searchOutShares(
                query = query,
                order = getCloudSortOrder()
            )
        ).thenReturn(emptyList())
        val list = underTest(query)
        Truth.assertThat(list).isEmpty()
    }

    @Test
    fun `test when search in out shares returns some items in list`() = runTest {
        val query = "SomeQuerry"
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            megaNodeRepository.searchOutShares(
                query = query,
                order = getCloudSortOrder()
            )
        ).thenReturn(listOf(mock(), mock()))
        val list = underTest(query)
        Truth.assertThat(list).hasSize(2)
    }
}