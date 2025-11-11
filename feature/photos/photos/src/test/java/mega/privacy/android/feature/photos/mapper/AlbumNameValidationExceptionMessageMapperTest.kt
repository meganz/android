package mega.privacy.android.feature.photos.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AlbumNameValidationExceptionMessageMapperTest {

    private lateinit var underTest: AlbumNameValidationExceptionMessageMapper

    @BeforeAll
    fun setUp() {
        underTest = AlbumNameValidationExceptionMessageMapper()
    }

    @Test
    fun `test that Empty exception maps to general_invalid_string resource`() {
        val exception = AlbumNameValidationException.Empty
        val result = underTest(exception)
        assertThat(result).isEqualTo(sharedR.string.general_invalid_string)
    }

    @Test
    fun `test that Exists exception maps to album_invalid_name_error_message resource`() {
        val exception = AlbumNameValidationException.Exists
        val result = underTest(exception)
        assertThat(result).isEqualTo(sharedR.string.album_invalid_name_error_message)
    }

    @Test
    fun `test that InvalidCharacters exception maps to album_name_exists_error_message resource`() {
        val exception = AlbumNameValidationException.InvalidCharacters
        val result = underTest(exception)
        assertThat(result).isEqualTo(sharedR.string.album_name_exists_error_message)
    }

    @Test
    fun `test that Proscribed exception maps to general_invalid_characters_defined resource`() {
        val exception = AlbumNameValidationException.Proscribed
        val result = underTest(exception)
        assertThat(result).isEqualTo(sharedR.string.general_invalid_characters_defined)
    }
}

