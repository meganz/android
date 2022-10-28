package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.FavouritesRepository
import mega.privacy.android.domain.repository.FileRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class DefaultGetAllFavoritesTest {
    lateinit var underTest: GetAllFavorites
    private val favouritesRepository = mock<FavouritesRepository>()

    private val addNodeType = mock<AddNodeType> {
        val typedNode = mock<TypedNode>()
        onBlocking { invoke(any()) }.thenReturn(typedNode)
    }

    private val fileRepository = mock<FileRepository> {
        on { monitorNodeUpdates() }.thenReturn(flowOf(
            emptyList()))
    }

    @Before
    fun setUp() {
        underTest = DefaultGetAllFavorites(
            favouritesRepository = favouritesRepository,
            fileRepository = fileRepository,
            addNodeType = addNodeType,
        )
    }

    @Test
    fun `test that favourites is not empty`() {
        runTest {
            val list = listOf(mock<UnTypedNode>())
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

    @Test
    fun `test that favourites returns result of getAllFavorites when a node update occur`() =
        runTest {
            whenever(favouritesRepository.getAllFavorites()).thenReturn(emptyList())
            whenever(fileRepository.monitorNodeUpdates()).thenReturn(flowOf(mock()))
            Truth.assertThat(underTest().count()).isEqualTo(2)
        }
}
