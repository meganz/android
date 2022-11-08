package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetFavouriteSortOrderTest {
    private lateinit var underTest: GetFavouriteSortOrder

    private val getCloudSortOrder = mock<GetCloudSortOrder> {
        onBlocking {
            invoke()
        }.thenReturn(SortOrder.ORDER_DEFAULT_ASC)
    }

    private val mapFavouriteSortOrder = mock<MapFavouriteSortOrder>()

    @Before
    fun setUp() {
        underTest = DefaultGetFavouriteSortOrder(
            getCloudSortOrder = getCloudSortOrder,
            mapFavouriteSortOrder = mapFavouriteSortOrder,
        )
    }

    @Test
    fun `test that mapper is called`() = runTest {
        underTest()

        verify(mapFavouriteSortOrder).invoke(any())
    }


}