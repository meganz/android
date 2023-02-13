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
class DefaultGetUserAlbumTest {
    private lateinit var underTest: GetUserAlbum

    private val albumRepository = mock<AlbumRepository>()
    private val photosRepository = mock<PhotosRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetUserAlbum(
            albumRepository = albumRepository,
            photosRepository = photosRepository,
            isNodeInRubbish = { false },
            defaultDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that get album collect behaves properly`() = runTest {
        val albumId = AlbumId(1L)
        val userSet = createUserSet(id = 1L)

        whenever(albumRepository.getUserSet(albumId)).thenReturn(userSet)
        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf())

        underTest(albumId).test {
            val actualUserAlbum = awaitItem()
            assertThat(actualUserAlbum?.id).isEqualTo(albumId)
            awaitComplete()
        }
    }

    private fun createUserSet(
        id: Long = 0L,
        name: String = "",
        cover: Long? = null,
        modificationTime: Long = 0L,
    ): UserSet = object : UserSet {
        override val id: Long = id

        override val name: String = name

        override val cover: Long? = cover

        override val modificationTime: Long = modificationTime
    }
}
