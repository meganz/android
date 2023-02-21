package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultGetSearchLinkSharesNodes
import mega.privacy.android.app.domain.usecase.GetSearchLinkSharesNodes
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaCancelToken
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetSearchLinkSharesNodesTest {
    private lateinit var underTest: GetSearchLinkSharesNodes
    private val megaNodeRepository: MegaNodeRepository = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val megaCancelToken: MegaCancelToken = mock()

    @Before
    fun setUp() {
        underTest = DefaultGetSearchLinkSharesNodes(
            megaNodeRepository = megaNodeRepository,
            getCloudSortOrder = getCloudSortOrder
        )
    }

    @Test
    fun `test when search in link returns empty response`() = runTest {
        val query = "SomeQuerry"
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            megaNodeRepository.searchLinkShares(
                query = query,
                megaCancelToken = megaCancelToken,
                order = getCloudSortOrder(),
                isFirstLevelNavigation = false
            )
        ).thenReturn(emptyList())
        val list = underTest(query, megaCancelToken, false)
        Truth.assertThat(list).isEmpty()
    }

    @Test
    fun `test when search in link returns some items in list`() = runTest {
        val query = "SomeQuerry"
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            megaNodeRepository.searchLinkShares(
                query = query,
                megaCancelToken = megaCancelToken,
                order = getCloudSortOrder(),
                isFirstLevelNavigation = false
            )
        ).thenReturn(listOf(mock(), mock()))
        val list = underTest(query, megaCancelToken, false)
        Truth.assertThat(list).hasSize(2)
    }
}