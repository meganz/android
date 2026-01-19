package mega.privacy.android.domain.usecase.videosection

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.account.PlaylistNameValidationException
import mega.privacy.android.domain.repository.VideoSectionRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ValidatePlaylistNameUseCaseTest {
    private lateinit var underTest: ValidatePlaylistNameUseCase

    private val videoSectionRepository: VideoSectionRepository = mock()

    @BeforeAll
    fun init() {
        underTest = ValidatePlaylistNameUseCase(
            videoSectionRepository = videoSectionRepository
        )
    }

    @BeforeEach
    fun setUp() {
        reset(videoSectionRepository)
    }

    @ParameterizedTest(name = "Empty exception for: {0}")
    @ValueSource(strings = ["", "   ", "\t", "\n"])
    fun `test that Empty exception is thrown when playlist name is blank or empty`(
        name: String
    ) = runTest {
        assertThrows<PlaylistNameValidationException.Empty> {
            underTest(name)
        }
    }

    @Test
    fun `test that Exists exception is thrown when playlist name already exists`() = runTest {
        val existingName = "My Playlist"
        val existingTitles = listOf("My Playlist", "Another Playlist")

        whenever(videoSectionRepository.getVideoPlaylistTitles()).thenReturn(existingTitles)

        assertThrows<PlaylistNameValidationException.Exists> {
            underTest(existingName)
        }
    }

    @ParameterizedTest(name = "InvalidCharacters exception for: {0}")
    @MethodSource("invalidCharacterNames")
    fun `test that InvalidCharacters exception is thrown when playlist name contains invalid characters`(
        invalidName: String
    ) = runTest {
        whenever(videoSectionRepository.getVideoPlaylistTitles()).thenReturn(emptyList())

        assertThrows<PlaylistNameValidationException.InvalidCharacters> {
            underTest(invalidName)
        }
    }

    @ParameterizedTest(name = "Valid name: {0}")
    @MethodSource("validNames")
    fun `test that validation passes for valid playlist names`(
        validName: String,
        existingTitles: List<String>
    ) = runTest {
        whenever(videoSectionRepository.getVideoPlaylistTitles()).thenReturn(existingTitles)

        // Should not throw any exception
        underTest(validName)
    }

    companion object {
        @JvmStatic
        fun invalidCharacterNames(): Stream<Arguments> = Stream.of(
            Arguments.of("My\\Playlist"), // backslash
            Arguments.of("My*Playlist"), // asterisk
            Arguments.of("My/Playlist"), // forward slash
            Arguments.of("My:Playlist"), // colon
            Arguments.of("My<Playlist"), // less than
            Arguments.of("My>Playlist"), // greater than
            Arguments.of("My?Playlist"), // question mark
            Arguments.of("My\"Playlist"), // double quote
            Arguments.of("My|Playlist"), // pipe
            Arguments.of("My<Playlist>"), // multiple invalid characters
            Arguments.of("\\"), // only invalid character
            Arguments.of("Playlist*"), // invalid character at end
            Arguments.of("*Playlist"), // invalid character at start
        )

        @JvmStatic
        fun validNames(): Stream<Arguments> = Stream.of(
            Arguments.of("My Valid Playlist", listOf("Other Playlist", "Another Playlist")),
            Arguments.of("My Playlist 2024 - (Videos)!", emptyList<String>()),
            Arguments.of("New Playlist", emptyList<String>()),
            Arguments.of("My Playlist Name", emptyList<String>()),
            Arguments.of("My_Playlist-Name", emptyList<String>()),
            Arguments.of("Playlist123", emptyList<String>()),
            Arguments.of("My Playlist!", emptyList<String>()),
            Arguments.of("Playlist (2024)", emptyList<String>()),
            Arguments.of("Playlist-Name_2024", emptyList<String>()),
        )
    }
}