package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.repository.FavouritesRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class DefaultGetAllFavoritesTest {
    lateinit var underTest: GetAllFavorites
    private val favouritesRepository = mock<FavouritesRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetAllFavorites(favouritesRepository)
        whenever(favouritesRepository.monitorNodeChange()).thenReturn(flowOf(false))
    }

    @Test
    fun `test that favourites is not empty`() {
        runTest {
            val list = mock<List<Node>>()
            whenever(favouritesRepository.getAllFavorites()).thenReturn(
                list
            )
            underTest().collect {
                assertTrue(it.isNotEmpty())
            }
        }
    }

    @Test
    fun `test that favourites is empty`() {
        runTest {
            whenever(favouritesRepository.getAllFavorites()).thenReturn(emptyList())
            underTest().collect {
                assertTrue(it.isEmpty())
            }
        }
    }
}