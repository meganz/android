package mega.privacy.android.domain.usecase.media

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.photos.AlbumId
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
 * Test class for [ValidateAndUpdateUserAlbumUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ValidateAndUpdateUserAlbumUseCaseTest {
    private lateinit var underTest: ValidateAndUpdateUserAlbumUseCase

    private val albumRepository: AlbumRepository = mock()
    private val validateAlbumNameUseCase: ValidateAlbumNameUseCase = mock()

    @BeforeEach
    fun setUp() {
        reset(albumRepository, validateAlbumNameUseCase)
    }

    @BeforeAll
    fun init() {
        underTest = ValidateAndUpdateUserAlbumUseCase(
            albumRepository = albumRepository,
            validateAlbumNameUseCase = validateAlbumNameUseCase
        )
    }

    @Test
    fun `test that album name is updated successfully when validation passes`() = runTest {
        val albumId = AlbumId(123L)
        val newName = "Updated Album"

        whenever(albumRepository.updateAlbumName(albumId, newName)).thenReturn(newName)

        underTest(albumId, newName)

        verify(validateAlbumNameUseCase).invoke(newName)
        verify(albumRepository).updateAlbumName(albumId, newName)
    }

    @Test
    fun `test that on exception is thrown should not update album name`() = runTest {
        val albumId = AlbumId(123L)

        whenever(validateAlbumNameUseCase(any())).thenThrow(AlbumNameValidationException.Empty)

        assertThrows<AlbumNameValidationException.Empty> {
            underTest(albumId, "User")
        }

        verify(validateAlbumNameUseCase).invoke("User")
        verify(albumRepository, never()).updateAlbumName(any(), any())
    }
}

