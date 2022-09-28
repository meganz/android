package test.mega.privacy.android.app.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.data.mapper.SortOrderMapper
import mega.privacy.android.app.data.repository.DefaultSortOrderRepository
import mega.privacy.android.domain.entity.SortOrder
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSortOrderRepositoryTest {
    private lateinit var underTest: DefaultSortOrderRepository

    private val megaLocalStorageGateway = mock<MegaLocalStorageGateway>()
    private val sortOrderMapper = mock<SortOrderMapper>()

    @Before
    fun setUp() {
        underTest = DefaultSortOrderRepository(
            ioDispatcher = UnconfinedTestDispatcher(),
            megaLocalStorageGateway = megaLocalStorageGateway,
            sortOrderMapper = sortOrderMapper,
        )
    }

    @Test
    fun `test that get camera sort order return type is sort order`() = runTest {
        whenever(megaLocalStorageGateway.getCameraSortOrder()).thenReturn(1)
        whenever(sortOrderMapper.invoke(1)).thenReturn(SortOrder.ORDER_NONE)
        assertThat(underTest.getCameraSortOrder()).isInstanceOf(SortOrder::class.java)
    }

    @Test
    fun `test that get camera sort order calls get camera sort order of mega local storage gateway`() =
        runTest {
            underTest.getCameraSortOrder()
            verify(megaLocalStorageGateway).getCameraSortOrder()
        }

    @Test
    fun `test that get camera sort order invokes sort order mapper`() = runTest {
        whenever(megaLocalStorageGateway.getCameraSortOrder()).thenReturn(1)
        underTest.getCameraSortOrder()
        verify(sortOrderMapper).invoke(1)
    }
}