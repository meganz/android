package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.favourite.FavouriteSortOrder
import mega.privacy.android.domain.usecase.favourites.MapFavouriteSortOrderUseCase
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapFavouriteSortOrderUseCaseTest {
    private val underTest: MapFavouriteSortOrderUseCase =
        MapFavouriteSortOrderUseCase()

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
    fun `test that ORDER_CREATION_ASC returns FavouriteSortOrder AddedDate with sortDescending false`() =
        runTest {
            val actual = underTest(SortOrder.ORDER_CREATION_ASC)

            assertThat(actual).isEqualTo(FavouriteSortOrder.AddedDate(false))
        }

    @Test
    fun `test that ORDER_CREATION_DESC returns FavouriteSortOrder AddedDate with sortDescending true`() =
        runTest {
            val actual = underTest(SortOrder.ORDER_CREATION_DESC)

            assertThat(actual).isEqualTo(FavouriteSortOrder.AddedDate(true))
        }

    @Test
    fun `test that ORDER_LABEL_ASC returns FavouriteSortOrder Label with sortDescending false`() =
        runTest {
            val actual = underTest(SortOrder.ORDER_LABEL_ASC)

            assertThat(actual).isEqualTo(FavouriteSortOrder.Label(false))
        }

    @Test
    fun `test that ORDER_LABEL_DESC returns FavouriteSortOrder Label with sortDescending true`() =
        runTest {
            val actual = underTest(SortOrder.ORDER_LABEL_DESC)

            assertThat(actual).isEqualTo(FavouriteSortOrder.Label(true))
        }
}