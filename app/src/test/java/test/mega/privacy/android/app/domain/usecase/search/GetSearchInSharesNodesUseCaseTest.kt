package test.mega.privacy.android.app.domain.usecase.search

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.search.GetSearchInSharesNodesUseCase
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetSearchInSharesNodesUseCaseTest {

    private lateinit var underTest: GetSearchInSharesNodesUseCase
    private val megaRepo: MegaNodeRepository = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()

    @Before
    fun setUp() {
        underTest = GetSearchInSharesNodesUseCase(
            megaNodeRepository = megaRepo,
            getCloudSortOrder = getCloudSortOrder
        )
    }

    @Test
    fun `test when search in shares returns empty response`() = runTest {
        val query = "SomeQuerry"
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            megaRepo.searchInShares(
                query,
                getCloudSortOrder()
            )
        ).thenReturn(emptyList())
        val list = underTest(query)
        Truth.assertThat(
            list
        ).isEmpty()
    }

    @Test
    fun `test when search in shares returns some items in list`() = runTest {
        val query = "SomeQuerry"
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            megaRepo.searchInShares(
                query,
                getCloudSortOrder()
            )
        ).thenReturn(listOf(mock(), mock()))
        val list = underTest(query)
        Truth.assertThat(
            list
        ).hasSize(2)
    }
}