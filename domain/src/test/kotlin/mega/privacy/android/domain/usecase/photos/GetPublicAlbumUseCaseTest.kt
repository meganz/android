package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPublicAlbumUseCaseTest {
    private lateinit var underTest: GetPublicAlbumUseCase

    private val albumRepository = mock<AlbumRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetPublicAlbumUseCase(
            albumRepository = albumRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(albumRepository)
    }

    @ParameterizedTest
    @MethodSource("domainProvider")
    fun `test that use case returns correct result`(
        domain: String,
    ) = runTest {
        // given
        val albumLink = AlbumLink("https://$domain/collection/example")

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
        assertThat(albumPhotos.second).isEqualTo(albumPhotoIds)
    }

    private fun domainProvider() = listOf("mega.nz", "mega.app")
}
