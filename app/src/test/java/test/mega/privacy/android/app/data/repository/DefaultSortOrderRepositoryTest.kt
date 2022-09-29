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
        val order = 1
        whenever(megaLocalStorageGateway.getCameraSortOrder()).thenReturn(order)
        whenever(sortOrderMapper.invoke(order)).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        assertThat(underTest.getCameraSortOrder()).isInstanceOf(SortOrder::class.java)
    }

    @Test
    fun `test that get camera sort order invokes get camera sort order of gateway`() = runTest {
        underTest.getCameraSortOrder()
        verify(megaLocalStorageGateway).getCameraSortOrder()
    }

    @Test
    fun `test that get camera sort order invokes sort order mapper`() = runTest {
        val order = 1
        whenever(megaLocalStorageGateway.getCameraSortOrder()).thenReturn(order)
        underTest.getCameraSortOrder()
        verify(sortOrderMapper).invoke(order)
    }

    @Test
    fun `test that get cloud sort order return type is sort order`() = runTest {
        val order = 2
        whenever(megaLocalStorageGateway.getCloudSortOrder()).thenReturn(order)
        whenever(sortOrderMapper.invoke(order)).thenReturn(SortOrder.ORDER_DEFAULT_DESC)
        assertThat(underTest.getCloudSortOrder()).isInstanceOf(SortOrder::class.java)
    }

    @Test
    fun `test that get cloud sort order invokes get cloud sort order of gateway`() = runTest {
        underTest.getCloudSortOrder()
        verify(megaLocalStorageGateway).getCloudSortOrder()
    }

    @Test
    fun `test that get cloud sort order invokes sort order mapper`() = runTest {
        val order = 2
        whenever(megaLocalStorageGateway.getCloudSortOrder()).thenReturn(order)
        underTest.getCloudSortOrder()
        verify(sortOrderMapper).invoke(order)
    }

    @Test
    fun `test that get links sort order return type is sort order`() = runTest {
        val order = 3
        whenever(megaLocalStorageGateway.getLinksSortOrder()).thenReturn(order)
        whenever(sortOrderMapper.invoke(order)).thenReturn(SortOrder.ORDER_SIZE_ASC)
        assertThat(underTest.getLinksSortOrder()).isInstanceOf(SortOrder::class.java)
    }

    @Test
    fun `test that get links sort order invokes get links sort order of gateway`() = runTest {
        underTest.getLinksSortOrder()
        verify(megaLocalStorageGateway).getLinksSortOrder()
    }

    @Test
    fun `test that get links sort order invokes sort order mapper`() = runTest {
        val order = 3
        whenever(megaLocalStorageGateway.getLinksSortOrder()).thenReturn(order)
        underTest.getLinksSortOrder()
        verify(sortOrderMapper).invoke(order)
    }

    @Test
    fun `test that get others sort order return type is sort order`() = runTest {
        val order = 4
        whenever(megaLocalStorageGateway.getOthersSortOrder()).thenReturn(order)
        whenever(sortOrderMapper.invoke(order)).thenReturn(SortOrder.ORDER_SIZE_DESC)
        assertThat(underTest.getOthersSortOrder()).isInstanceOf(SortOrder::class.java)
    }

    @Test
    fun `test that get others sort order invokes get links sort order of gateway`() = runTest {
        underTest.getOthersSortOrder()
        verify(megaLocalStorageGateway).getOthersSortOrder()
    }

    @Test
    fun `test that get others sort order invokes sort order mapper`() = runTest {
        val order = 4
        whenever(megaLocalStorageGateway.getOthersSortOrder()).thenReturn(order)
        underTest.getOthersSortOrder()
        verify(sortOrderMapper).invoke(order)
    }
}