package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetUserAlbumsTest {
    private lateinit var underTest: GetUserAlbums

    private val albumRepository = mock<AlbumRepository>()
    private val photosRepository = mock<PhotosRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetUserAlbums(
            albumRepository = albumRepository,
            photosRepository = photosRepository,
            defaultDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that user albums collect is working`() = runTest {
        val userSets = (1..3).map { createUserSet(id = it.toLong()) }
        whenever(albumRepository.getAllUserSets()).thenReturn(userSets)
        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf())

        underTest().test {
            val actualUserAlbums = awaitItem()

            assertThat(actualUserAlbums.size).isEqualTo(3)
            assertThat(actualUserAlbums[0].id).isEqualTo(AlbumId(1L))
            assertThat(actualUserAlbums[1].id).isEqualTo(AlbumId(2L))
            assertThat(actualUserAlbums[2].id).isEqualTo(AlbumId(3L))

            awaitComplete()
        }
    }

    private fun createUserSet(
        id: Long = 0L,
        name: String = "",
        cover: Long? = null,
        modificationTime: Long = 0L,
        isExported: Boolean = false,
    ): UserSet = object : UserSet {
        override val id: Long = id

        override val name: String = name

        override val cover: Long? = cover

        override val modificationTime: Long = modificationTime

        override val isExported: Boolean = isExported
    }
}
