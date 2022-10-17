package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.SortOrderMapper
import mega.privacy.android.domain.entity.SortOrder
import nz.mega.sdk.MegaApiJava
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
    private val sortOrderIntMapper = mock<SortOrderIntMapper>()

    @Before
    fun setUp() {
        underTest = DefaultSortOrderRepository(
            ioDispatcher = UnconfinedTestDispatcher(),
            megaLocalStorageGateway = megaLocalStorageGateway,
            sortOrderMapper = sortOrderMapper,
            sortOrderIntMapper = sortOrderIntMapper,
        )
    }

    @Test
    fun `test that get camera sort order return type is sort order`() = runTest {
        val order = MegaApiJava.ORDER_DEFAULT_ASC
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
        val order = MegaApiJava.ORDER_DEFAULT_ASC
        whenever(megaLocalStorageGateway.getCameraSortOrder()).thenReturn(order)
        underTest.getCameraSortOrder()
        verify(sortOrderMapper).invoke(order)
    }

    @Test
    fun `test that get cloud sort order return type is sort order`() = runTest {
        val order = MegaApiJava.ORDER_DEFAULT_DESC
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
        val order = MegaApiJava.ORDER_DEFAULT_DESC
        whenever(megaLocalStorageGateway.getCloudSortOrder()).thenReturn(order)
        underTest.getCloudSortOrder()
        verify(sortOrderMapper).invoke(order)
    }

    @Test
    fun `test that get links sort order return type is sort order`() = runTest {
        val order = MegaApiJava.ORDER_SIZE_ASC
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
        val order = MegaApiJava.ORDER_SIZE_ASC
        whenever(megaLocalStorageGateway.getLinksSortOrder()).thenReturn(order)
        underTest.getLinksSortOrder()
        verify(sortOrderMapper).invoke(order)
    }

    @Test
    fun `test that get others sort order return type is sort order`() = runTest {
        val order = MegaApiJava.ORDER_SIZE_DESC
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
        val order = MegaApiJava.ORDER_SIZE_DESC
        whenever(megaLocalStorageGateway.getOthersSortOrder()).thenReturn(order)
        underTest.getOthersSortOrder()
        verify(sortOrderMapper).invoke(order)
    }

    @Test
    fun `test that get offline sort order return type is sort order`() = runTest {
        val order = MegaApiJava.ORDER_CREATION_ASC
        whenever(megaLocalStorageGateway.getOfflineSortOrder()).thenReturn(order)
        whenever(sortOrderMapper.invoke(order)).thenReturn(SortOrder.ORDER_CREATION_ASC)
        assertThat(underTest.getOfflineSortOrder()).isInstanceOf(SortOrder::class.java)
    }

    @Test
    fun `test that get offline sort order invokes get offline sort order of gateway`() = runTest {
        underTest.getOfflineSortOrder()
        verify(megaLocalStorageGateway).getOfflineSortOrder()
    }

    @Test
    fun `test that get offline sort order invokes sort order mapper`() = runTest {
        val order = MegaApiJava.ORDER_CREATION_ASC
        whenever(megaLocalStorageGateway.getOfflineSortOrder()).thenReturn(order)
        underTest.getOfflineSortOrder()
        verify(sortOrderMapper).invoke(order)
    }

    @Test
    fun `test that set offline sort order invokes set offline sort order of gateway`() = runTest {
        val order = SortOrder.ORDER_CREATION_DESC
        val expected = MegaApiJava.ORDER_CREATION_DESC
        whenever(sortOrderIntMapper.invoke(order)).thenReturn(expected)
        underTest.setOfflineSortOrder(order)
        verify(megaLocalStorageGateway).setOfflineSortOrder(expected)
    }

    @Test
    fun `test that set offline sort order invokes sort order int mapper`() = runTest {
        val order = SortOrder.ORDER_CREATION_DESC
        val expected = MegaApiJava.ORDER_CREATION_DESC
        whenever(sortOrderIntMapper.invoke(order)).thenReturn(expected)
        underTest.setOfflineSortOrder(order)
        verify(sortOrderIntMapper).invoke(order)
    }

    @Test
    fun `test that set camera sort order invokes set camera sort order of gateway`() = runTest {
        val order = SortOrder.ORDER_MODIFICATION_ASC
        val expected = MegaApiJava.ORDER_MODIFICATION_ASC
        whenever(sortOrderIntMapper.invoke(order)).thenReturn(expected)
        underTest.setCameraSortOrder(order)
        verify(megaLocalStorageGateway).setCameraSortOrder(expected)
    }

    @Test
    fun `test that set camera sort order invokes sort order int mapper`() = runTest {
        val order = SortOrder.ORDER_MODIFICATION_ASC
        val expected = MegaApiJava.ORDER_MODIFICATION_ASC
        whenever(sortOrderIntMapper.invoke(order)).thenReturn(expected)
        underTest.setCameraSortOrder(order)
        verify(sortOrderIntMapper).invoke(order)
    }

    @Test
    fun `test that set cloud sort order invokes set cloud sort order of gateway`() = runTest {
        val order = SortOrder.ORDER_MODIFICATION_DESC
        val expected = MegaApiJava.ORDER_MODIFICATION_DESC
        whenever(sortOrderIntMapper.invoke(order)).thenReturn(expected)
        underTest.setCloudSortOrder(order)
        verify(megaLocalStorageGateway).setCloudSortOrder(expected)
    }

    @Test
    fun `test that set cloud sort order invokes sort order int mapper`() = runTest {
        val order = SortOrder.ORDER_MODIFICATION_DESC
        val expected = MegaApiJava.ORDER_MODIFICATION_DESC
        whenever(sortOrderIntMapper.invoke(order)).thenReturn(expected)
        underTest.setCloudSortOrder(order)
        verify(sortOrderIntMapper).invoke(order)
    }

    @Test
    fun `test that set others sort order invokes set others sort order of gateway`() = runTest {
        val order = SortOrder.ORDER_ALPHABETICAL_ASC
        val expected = MegaApiJava.ORDER_ALPHABETICAL_ASC
        whenever(sortOrderIntMapper.invoke(order)).thenReturn(expected)
        underTest.setOthersSortOrder(order)
        verify(megaLocalStorageGateway).setOthersSortOrder(expected)
    }

    @Test
    fun `test that set others sort order invokes sort order int mapper`() = runTest {
        val order = SortOrder.ORDER_ALPHABETICAL_ASC
        val expected = MegaApiJava.ORDER_ALPHABETICAL_ASC
        whenever(sortOrderIntMapper.invoke(order)).thenReturn(expected)
        underTest.setOthersSortOrder(order)
        verify(sortOrderIntMapper).invoke(order)
    }
}