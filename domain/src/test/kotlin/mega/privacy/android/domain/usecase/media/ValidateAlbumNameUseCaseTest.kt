package mega.privacy.android.domain.usecase.media

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNamesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [ValidateAlbumNameUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ValidateAlbumNameUseCaseTest {
    private lateinit var underTest: ValidateAlbumNameUseCase

    private val getProscribedAlbumNamesUseCase: GetProscribedAlbumNamesUseCase = mock()
    private val albumRepository: AlbumRepository = mock()

    @BeforeAll
    fun init() {
        underTest = ValidateAlbumNameUseCase(
            getProscribedAlbumNamesUseCase = getProscribedAlbumNamesUseCase,
            albumsRepository = albumRepository
        )
    }

    @BeforeEach
    fun setUp() {
        reset(getProscribedAlbumNamesUseCase, albumRepository)
    }

    @Test
    fun `test that Empty exception is thrown when album name is blank`() = runTest {
        val blankName = "   "

        assertThrows<AlbumNameValidationException.Empty> {
            underTest(blankName)
        }
    }

    @Test
    fun `test that Empty exception is thrown when album name is empty`() = runTest {
        val emptyName = ""

        assertThrows<AlbumNameValidationException.Empty> {
            underTest(emptyName)
        }
    }

    @Test
    fun `test that Proscribed exception is thrown when album name is a system album name`() =
        runTest {
            val proscribedName = "Camera Uploads"
            val proscribedNames = listOf("Camera Uploads", "My Chat Files", "Favourites")

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedNames)

            assertThrows<AlbumNameValidationException.Proscribed> {
                underTest(proscribedName)
            }
        }

    @Test
    fun `test that Proscribed exception is thrown when album name matches proscribed name case-insensitively`() =
        runTest {
            val proscribedName = "camera uploads"
            val proscribedNames = listOf("Camera Uploads", "My Chat Files", "Favourites")

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedNames)

            assertThrows<AlbumNameValidationException.Proscribed> {
                underTest(proscribedName)
            }
        }

    @Test
    fun `test that Exists exception is thrown when album name already exists`() = runTest {
        val existingName = "My Album"
        val proscribedNames = emptyList<String>()
        val existingAlbums = listOf(
            createMockUserSet(name = "My Album"),
            createMockUserSet(name = "Another Album")
        )

        whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedNames)
        whenever(albumRepository.getAllUserSets()).thenReturn(existingAlbums)

        assertThrows<AlbumNameValidationException.Exists> {
            underTest(existingName)
        }
    }

    @Test
    fun `test that InvalidCharacters exception is thrown when album name contains backslash`() =
        runTest {
            val invalidName = "My\\Album"
            val proscribedNames = emptyList<String>()
            val existingAlbums = emptyList<UserSet>()

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedNames)
            whenever(albumRepository.getAllUserSets()).thenReturn(existingAlbums)

            assertThrows<AlbumNameValidationException.InvalidCharacters> {
                underTest(invalidName)
            }
        }

    @Test
    fun `test that InvalidCharacters exception is thrown when album name contains asterisk`() =
        runTest {
            val invalidName = "My*Album"
            val proscribedNames = emptyList<String>()
            val existingAlbums = emptyList<UserSet>()

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedNames)
            whenever(albumRepository.getAllUserSets()).thenReturn(existingAlbums)

            assertThrows<AlbumNameValidationException.InvalidCharacters> {
                underTest(invalidName)
            }
        }

    @Test
    fun `test that InvalidCharacters exception is thrown when album name contains forward slash`() =
        runTest {
            val invalidName = "My/Album"
            val proscribedNames = emptyList<String>()
            val existingAlbums = emptyList<UserSet>()

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedNames)
            whenever(albumRepository.getAllUserSets()).thenReturn(existingAlbums)

            assertThrows<AlbumNameValidationException.InvalidCharacters> {
                underTest(invalidName)
            }
        }

    @Test
    fun `test that InvalidCharacters exception is thrown when album name contains colon`() =
        runTest {
            val invalidName = "My:Album"
            val proscribedNames = emptyList<String>()
            val existingAlbums = emptyList<UserSet>()

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedNames)
            whenever(albumRepository.getAllUserSets()).thenReturn(existingAlbums)

            assertThrows<AlbumNameValidationException.InvalidCharacters> {
                underTest(invalidName)
            }
        }

    @Test
    fun `test that InvalidCharacters exception is thrown when album name contains less than`() =
        runTest {
            val invalidName = "My<Album"
            val proscribedNames = emptyList<String>()
            val existingAlbums = emptyList<UserSet>()

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedNames)
            whenever(albumRepository.getAllUserSets()).thenReturn(existingAlbums)

            assertThrows<AlbumNameValidationException.InvalidCharacters> {
                underTest(invalidName)
            }
        }

    @Test
    fun `test that InvalidCharacters exception is thrown when album name contains greater than`() =
        runTest {
            val invalidName = "My>Album"
            val proscribedNames = emptyList<String>()
            val existingAlbums = emptyList<UserSet>()

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedNames)
            whenever(albumRepository.getAllUserSets()).thenReturn(existingAlbums)

            assertThrows<AlbumNameValidationException.InvalidCharacters> {
                underTest(invalidName)
            }
        }

    @Test
    fun `test that InvalidCharacters exception is thrown when album name contains question mark`() =
        runTest {
            val invalidName = "My?Album"
            val proscribedNames = emptyList<String>()
            val existingAlbums = emptyList<UserSet>()

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedNames)
            whenever(albumRepository.getAllUserSets()).thenReturn(existingAlbums)

            assertThrows<AlbumNameValidationException.InvalidCharacters> {
                underTest(invalidName)
            }
        }

    @Test
    fun `test that InvalidCharacters exception is thrown when album name contains double quote`() =
        runTest {
            val invalidName = "My\"Album"
            val proscribedNames = emptyList<String>()
            val existingAlbums = emptyList<UserSet>()

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedNames)
            whenever(albumRepository.getAllUserSets()).thenReturn(existingAlbums)

            assertThrows<AlbumNameValidationException.InvalidCharacters> {
                underTest(invalidName)
            }
        }

    @Test
    fun `test that InvalidCharacters exception is thrown when album name contains pipe`() =
        runTest {
            val invalidName = "My|Album"
            val proscribedNames = emptyList<String>()
            val existingAlbums = emptyList<UserSet>()

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedNames)
            whenever(albumRepository.getAllUserSets()).thenReturn(existingAlbums)

            assertThrows<AlbumNameValidationException.InvalidCharacters> {
                underTest(invalidName)
            }
        }

    @Test
    fun `test that validation passes for valid album name`() = runTest {
        val validName = "My Valid Album"
        val proscribedNames = listOf("Camera Uploads", "My Chat Files")
        val existingAlbums = listOf(
            createMockUserSet(name = "Other Album"),
            createMockUserSet(name = "Another Album")
        )

        whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedNames)
        whenever(albumRepository.getAllUserSets()).thenReturn(existingAlbums)

        // Should not throw any exception
        underTest(validName)
    }

    @Test
    fun `test that validation passes for album name with numbers and special characters that are allowed`() =
        runTest {
            val validName = "My Album 2024 - (Photos)!"
            val proscribedNames = listOf("Camera Uploads")
            val existingAlbums = emptyList<UserSet>()

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedNames)
            whenever(albumRepository.getAllUserSets()).thenReturn(existingAlbums)

            // Should not throw any exception
            underTest(validName)
        }

    private fun createMockUserSet(
        id: Long = 1L,
        name: String = "Test Album",
    ): UserSet {
        return mock {
            on { this.id }.thenReturn(id)
            on { this.name }.thenReturn(name)
        }
    }
}

