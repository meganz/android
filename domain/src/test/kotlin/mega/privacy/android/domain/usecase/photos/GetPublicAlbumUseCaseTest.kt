package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class GetPublicAlbumUseCaseTest {
    private lateinit var underTest: GetPublicAlbumUseCase

    private val albumRepository = mock<AlbumRepository>()

    @Before
    fun setUp() {
        underTest = GetPublicAlbumUseCase(
            albumRepository = albumRepository,
        )
    }

    @Test
    fun `test that use case returns correct result`() = runTest {
        // given
        val albumLink = AlbumLink("https://mega.nz/collection/example")

        val userSet = mock<UserSet> {
            on { id }.thenReturn(1L)
            on { name }.thenReturn("Album 1")
        }
        val albumPhotoIds = listOf(AlbumPhotoId.default)
        val photos = listOf<Photo>(mock<Photo.Image>())

        whenever(albumRepository.fetchPublicAlbum(albumLink))
            .thenReturn(userSet to albumPhotoIds)

        whenever(albumRepository.getPublicPhotos(albumPhotoIds))
            .thenReturn(photos)

        // when
        val albumPhotos = underTest(albumLink)

        // then
        assertThat(albumPhotos.first).isNotNull()
        assertThat(albumPhotos.second).isEqualTo(photos)
    }
}
