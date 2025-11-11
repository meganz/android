package mega.privacy.android.domain.usecase.media

import kotlinx.coroutines.test.runTest
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
 * Test class for [CreateUserAlbumUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CreateUserAlbumUseCaseTest {
    private lateinit var underTest: CreateUserAlbumUseCase

    private val albumRepository: AlbumRepository = mock()
    private val validateAlbumNameUseCase: ValidateAlbumNameUseCase = mock()

    @BeforeEach
    fun setUp() {
        reset(albumRepository, validateAlbumNameUseCase)
    }

    @BeforeAll
    fun init() {
        underTest = CreateUserAlbumUseCase(
            albumRepository = albumRepository,
            validateAlbumNameUseCase = validateAlbumNameUseCase
        )
    }

    @Test
    fun `test that album is created successfully when validation passes`() = runTest {
        val albumName = "My Album"
        val mockUserSet = mock<mega.privacy.android.domain.entity.set.UserSet>()

        whenever(albumRepository.createAlbum(albumName)).thenReturn(mockUserSet)

        underTest(albumName)

        verify(validateAlbumNameUseCase).invoke(albumName)
        verify(albumRepository).createAlbum(albumName)
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

