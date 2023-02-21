package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultGetSearchInSharesNodes
import mega.privacy.android.app.domain.usecase.GetSearchInSharesNodes
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaCancelToken
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetSearchInSharesNodesTest {

    private lateinit var underTest: GetSearchInSharesNodes
    private val megaRepo: MegaNodeRepository = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val megaCancelToken: MegaCancelToken = mock()

    @Before
    fun setUp() {
        underTest = DefaultGetSearchInSharesNodes(
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
                megaCancelToken,
                getCloudSortOrder()
            )
        ).thenReturn(emptyList())
        val list = underTest(query, megaCancelToken)
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
                megaCancelToken,
                getCloudSortOrder()
            )
        ).thenReturn(listOf(mock(), mock()))
        val list = underTest(query, megaCancelToken)
        Truth.assertThat(
            list
        ).hasSize(2)
    }
}