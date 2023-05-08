package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.repository.FavouritesRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.favourites.GetAllFavoritesUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class GetAllFavoritesUseCaseTest {
    lateinit var underTest: GetAllFavoritesUseCase
    private val favouritesRepository = mock<FavouritesRepository>()

    private val addNodeType = mock<AddNodeType> {
        val typedNode = mock<TypedFolderNode>()
        onBlocking { invoke(any()) }.thenReturn(typedNode)
    }

    private val nodeRepository = mock<NodeRepository> {
        on { monitorNodeUpdates() }.thenReturn(
            flowOf(
                NodeUpdate(emptyMap())
            )
        )
    }

    @Before
    fun setUp() {
        underTest = GetAllFavoritesUseCase(
            favouritesRepository = favouritesRepository,
            nodeRepository = nodeRepository,
            addNodeType = addNodeType,
        )
    }

    @Test
    fun `test that favourites is not empty`() {
        runTest {
            val list = listOf(mock<FolderNode>())
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
            whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf(NodeUpdate(emptyMap())))
            Truth.assertThat(underTest().count()).isEqualTo(2)
        }
}
