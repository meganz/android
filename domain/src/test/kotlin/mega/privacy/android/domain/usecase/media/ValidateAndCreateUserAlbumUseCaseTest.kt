package mega.privacy.android.domain.usecase.media

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [ValidateAndCreateUserAlbumUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ValidateAndCreateUserAlbumUseCaseTest {
    private lateinit var underTest: ValidateAndCreateUserAlbumUseCase

    private val albumRepository: AlbumRepository = mock()
    private val validateAlbumNameUseCase: ValidateAlbumNameUseCase = mock()

    @BeforeEach
    fun setUp() {
        reset(albumRepository, validateAlbumNameUseCase)
    }

    @BeforeAll
    fun init() {
        underTest = ValidateAndCreateUserAlbumUseCase(
            albumRepository = albumRepository,
            validateAlbumNameUseCase = validateAlbumNameUseCase
        )
    }

    @Test
    fun `test that album is created successfully when validation passes`() = runTest {
        val albumName = "My Album"
        val mockUserSet = mock<UserSet> {
            on { id }.thenReturn(1)
        }

        whenever(albumRepository.createAlbum(albumName)).thenReturn(mockUserSet)

        val result = underTest(albumName)

        verify(validateAlbumNameUseCase).invoke(albumName)
        verify(albumRepository).createAlbum(albumName)
        assertThat(result).isEqualTo(AlbumId(1))
    }

    @Test
    fun `test that on exception is thrown should not create album`() = runTest {
        whenever(validateAlbumNameUseCase(any())).thenThrow(AlbumNameValidationException.Empty)

        assertThrows<AlbumNameValidationException.Empty> {
            underTest("User")
        }

        verify(validateAlbumNameUseCase).invoke("User")
        verify(albumRepository, never()).createAlbum(any())
    }
}

