package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.favourite.FavouriteSortOrder
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMapFavouriteSortOrderTest {
    private val underTest: MapFavouriteSortOrder =
        DefaultMapFavouriteSortOrder()

    @Test
    fun `test that ORDER_DEFAULT_ASC returns FavouriteSortOrder Name with sortDescending false`() =
        runTest {
            val actual = underTest(SortOrder.ORDER_DEFAULT_ASC)

            assertThat(actual).isEqualTo(FavouriteSortOrder.Name(false))
        }

    @Test
    fun `test that ORDER_DEFAULT_DESC returns FavouriteSortOrder Name with sortDescending true`() =
        runTest {
            val actual = underTest(SortOrder.ORDER_DEFAULT_DESC)

            assertThat(actual).isEqualTo(FavouriteSortOrder.Name(true))
        }

    @Test
    fun `test that ORDER_SIZE_ASC returns FavouriteSortOrder Size with sortDescending false`() =
        runTest {
            val actual = underTest(SortOrder.ORDER_SIZE_ASC)

            assertThat(actual).isEqualTo(FavouriteSortOrder.Size(false))
        }

    @Test
    fun `test that ORDER_SIZE_DESC returns FavouriteSortOrder Size with sortDescending true`() =
        runTest {
            val actual = underTest(SortOrder.ORDER_SIZE_DESC)

            assertThat(actual).isEqualTo(FavouriteSortOrder.Size(true))
        }

    @Test
    fun `test that ORDER_MODIFICATION_ASC returns FavouriteSortOrder ModifiedDate with sortDescending false`() =
        runTest {
            val actual = underTest(SortOrder.ORDER_MODIFICATION_ASC)

            assertThat(actual).isEqualTo(FavouriteSortOrder.ModifiedDate(false))
        }

    @Test
    fun `test that ORDER_MODIFICATION_DESC returns FavouriteSortOrder ModifiedDate with sortDescending true`() =
        runTest {
            val actual = underTest(SortOrder.ORDER_MODIFICATION_DESC)

            assertThat(actual).isEqualTo(FavouriteSortOrder.ModifiedDate(true))
        }

    @Test
    fun `test that remaining values return FavouriteSortOrder Label`() = runTest {
        val remainingSortOrders = SortOrder.values().toList() - listOf(
            SortOrder.ORDER_DEFAULT_ASC,
            SortOrder.ORDER_DEFAULT_DESC,
            SortOrder.ORDER_SIZE_ASC,
            SortOrder.ORDER_SIZE_DESC,
            SortOrder.ORDER_MODIFICATION_ASC,
            SortOrder.ORDER_MODIFICATION_DESC,
        )

        remainingSortOrders.forEach { order ->
            val actual = underTest(order)
            assertThat(actual).isEqualTo(FavouriteSortOrder.Label)
        }
    }
}