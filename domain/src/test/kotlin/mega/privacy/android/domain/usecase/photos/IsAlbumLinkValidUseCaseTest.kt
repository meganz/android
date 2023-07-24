package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class IsAlbumLinkValidUseCaseTest {
    private lateinit var underTest: IsAlbumLinkValidUseCase

    private val albumRepository: AlbumRepository = mock()

    @Before
    fun setUp() {
        underTest = IsAlbumLinkValidUseCase(
            albumRepository,
        )
    }

    @Test
    fun `test that use case returns correct result`() = runTest {
        // given
        val albumLink = AlbumLink("https://mega.nz/collection")

        whenever(albumRepository.isAlbumLinkValid(albumLink))
            .thenReturn(true)

        // when
        val isValid = underTest(albumLink)

        // then
        assertThat(isValid).isTrue()
    }
}
