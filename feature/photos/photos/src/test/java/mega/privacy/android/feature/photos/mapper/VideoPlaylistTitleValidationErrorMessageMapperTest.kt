package mega.privacy.android.feature.photos.mapper

import android.content.Context
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.exception.account.PlaylistNameValidationException
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoPlaylistTitleValidationErrorMessageMapperTest {

    private lateinit var underTest: VideoPlaylistTitleValidationErrorMessageMapper
    private val context: Context = mock()

    @BeforeAll
    fun setUp() {
        underTest = VideoPlaylistTitleValidationErrorMessageMapper(context)
    }

    @BeforeEach
    fun init() {
        reset(context)
    }

    @Test
    fun `test that Empty exception maps to general_invalid_string resource`() {
        val expected = "expected"
        val exception = PlaylistNameValidationException.Empty
        whenever(context.getString(sharedR.string.general_invalid_string)).thenReturn(expected)
        assertThat(underTest(exception)).isEqualTo(expected)
    }

    @Test
    fun `test that Exists exception maps to video_section_playlists_error_message_playlist_name_exists resource`() {
        val expected = "expected"
        val exception = PlaylistNameValidationException.Exists
        whenever(
            context.getString(sharedR.string.video_section_playlists_error_message_playlist_name_exists)
        ).thenReturn(expected)
        assertThat(underTest(exception)).isEqualTo(expected)
    }

    @Test
    fun `test that InvalidCharacters exception maps to general_invalid_characters_defined resource`() {
        val expected = "expected"
        val invalidChars = """\ / * : < > ? " |"""
        val exception = PlaylistNameValidationException.InvalidCharacters(invalidChars)
        whenever(
            context.getString(
                sharedR.string.general_invalid_characters_defined,
                invalidChars
            )
        ).thenReturn(expected)
        assertThat(underTest(exception)).isEqualTo(expected)
    }

    @Test
    fun `test that Proscribed exception returns null`() {
        val exception = PlaylistNameValidationException.Proscribed
        assertThat(underTest(exception)).isNull()
    }
}