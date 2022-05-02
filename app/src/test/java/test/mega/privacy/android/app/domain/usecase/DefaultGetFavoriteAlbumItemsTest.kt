package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.entity.AlbumItemInfo
import mega.privacy.android.app.domain.repository.AlbumsRepository
import mega.privacy.android.app.domain.usecase.DefaultGetFavoriteAlbumItems
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class DefaultGetFavoriteAlbumItemsTest {
    lateinit var underTest: DefaultGetFavoriteAlbumItems
    private val albumsRepository = mock<AlbumsRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetFavoriteAlbumItems(albumsRepository)
        whenever(albumsRepository.monitorNodeChange()).thenReturn(flowOf(false))
    }

    @Test
    fun `test that favourite album is not empty`() {
        runTest {
            val list = mock<List<AlbumItemInfo>>()
            whenever(albumsRepository.getFavouriteAlbumItems()).thenReturn(
                list
            )
            underTest().collect {
                assertTrue(it.isNotEmpty())
            }
        }
    }

    @Test
    fun `test that favourite album is empty`() {
        runTest {
            whenever(albumsRepository.getFavouriteAlbumItems()).thenReturn(emptyList())
            underTest().collect {
                assertTrue(it.isNullOrEmpty())
            }
        }
    }
}